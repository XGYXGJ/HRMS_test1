package com.example.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.SalaryStandardDTO;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalaryService {

    @Autowired private SalaryStandardMasterMapper standardMasterMapper;
    @Autowired private SalaryStandardDetailMapper standardDetailMapper;
    @Autowired private SalaryRegisterMasterMapper registerMasterMapper;
    @Autowired private SalaryRegisterDetailMapper registerDetailMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private SalaryItemMapper itemMapper;

    // ✅ 新增：注入 PositionMapper 用于查职位
    @Autowired private PositionMapper positionMapper;

    // -------------------------
    // 1) 提交薪酬标准
    // -------------------------
    @Transactional
    public void submitStandard(SalaryStandardDTO dto, Integer submitterId, Integer l3OrgId) {

        SalaryStandardMaster master = new SalaryStandardMaster();

        master.setStandardCode(dto.getStandardCode());
        master.setStandardName(dto.getStandardName());
        master.setL3OrgId(l3OrgId);
        master.setPositionId(dto.getPositionId());
        master.setSubmitterId(submitterId);
        master.setAuditStatus("Pending");
        master.setSubmissionTime(LocalDateTime.now());

        standardMasterMapper.insert(master);

        if (dto.getItems() != null) {
            Integer standardId = master.getStandardId();

            for (Map.Entry<Integer, BigDecimal> entry : dto.getItems().entrySet()) {
                BigDecimal value = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;

                SalaryStandardDetail detail = new SalaryStandardDetail();
                detail.setStandardId(standardId);
                detail.setItemId(entry.getKey());
                detail.setValue(value);

                standardDetailMapper.insert(detail);
            }
        }
    }

    // -------------------------
    // ✅ 新增：按机构反推可用职位（无 SQL 改动，仅 MP 查询）
    // -------------------------
    public List<Position> getApplicablePositionsByOrg(Integer l3OrgId) {
        if (l3OrgId == null) {
            // 没 orgId 就直接退化为全量职位，避免页面空
            return positionMapper.selectList(null);
        }

        // 1) 找该机构下有效用户
        List<User> usersInOrg = userMapper.selectList(
                new QueryWrapper<User>()
                        .eq("L3_Org_ID", l3OrgId)
                        .eq("Is_Deleted", 0)
        );

        // 2) 取 distinct positionId
        List<Integer> positionIds = usersInOrg.stream()
                .map(User::getPositionId)
                .filter(pid -> pid != null)
                .distinct()
                .collect(Collectors.toList());

        // 3) 有岗位就批量查岗位
        if (!positionIds.isEmpty()) {
            List<Position> positions = positionMapper.selectBatchIds(positionIds);
            if (positions != null && !positions.isEmpty()) {
                return positions;
            }
        }

        // 4) 兜底：机构下没人/没岗位 ⇒ 返回全量职位
        List<Position> all = positionMapper.selectList(null);
        return all != null ? all : Collections.emptyList();
    }

    // -------------------------
    // 2) 一键登记本月工资
    // -------------------------
    @Transactional
    public void createMonthlyRegister(Integer l3OrgId) {

        // ---------- 0. 参数检查 ----------
        if (l3OrgId == null) {
            throw new RuntimeException("L3机构ID不能为空");
        }

        // ---------- 1. 创建主表（Draft） ----------
        SalaryRegisterMaster master = new SalaryRegisterMaster();
        master.setL3OrgId(l3OrgId);
        master.setRegisterTime(LocalDateTime.now());
        master.setPayDate(LocalDate.now().withDayOfMonth(1));   // 工资月份=当月1号（你原来如果有别的规则可替换）
        master.setAuditStatus("Draft");                        // ✅ 关键修复：刚生成=草稿
        master.setTotalPeople(0);
        master.setTotalAmount(BigDecimal.ZERO);

        registerMasterMapper.insert(master);
        Integer registerId = master.getRegisterId();
        if (registerId == null) {
            throw new RuntimeException("工资主表插入失败，Register_ID为空");
        }

        // ---------- 2. 查询本机构员工 ----------
        List<User> employees = userMapper.selectList(
                new QueryWrapper<User>()
                        .eq("L3_Org_ID", l3OrgId)
                        .eq("Is_Deleted", 0)          // 你 User 表有软删字段:contentReference[oaicite:3]{index=3}
                        .ne("Position_ID", 1)         // 排除管理员/非员工（按你初始化 Position_ID=1 为管理员）
        );

        if (employees == null || employees.isEmpty()) {
            // 没员工也允许生成空草稿
            return;
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // ---------- 3. 为每个员工生成明细 ----------
        for (User emp : employees) {

            // 3.1 找“已审批通过”的薪酬标准（按职位+机构）
            SalaryStandardMaster standard = standardMasterMapper.selectOne(
                    new QueryWrapper<SalaryStandardMaster>()
                            .eq("L3_Org_ID", l3OrgId)
                            .eq("Position_ID", emp.getPositionId())
                            .eq("Audit_Status", "Approved")
                            .orderByDesc("Standard_ID")
                            .last("LIMIT 1")
            );

            if (standard == null) {
                // 没标准就跳过（也可插0薪资明细，看你作业需求）
                continue;
            }

            // 3.2 拉标准详情（薪酬项目）
            List<SalaryStandardDetail> stdDetails =
                    standardDetailMapper.selectList(
                            new QueryWrapper<SalaryStandardDetail>()
                                    .eq("Standard_ID", standard.getStandardId())
                    );

            // 3.3 计算薪资各构成
            BigDecimal baseSalary = BigDecimal.ZERO;
            BigDecimal totalSubsidy = BigDecimal.ZERO;
            BigDecimal kpiUnitPrice = BigDecimal.ZERO;
            BigDecimal attendanceBonus = BigDecimal.ZERO;
            BigDecimal absencePenaltyPerDay = BigDecimal.ZERO;
            BigDecimal overtimePricePerHour = BigDecimal.ZERO;

            for (SalaryStandardDetail d : stdDetails) {
                SalaryItem item = itemMapper.selectById(d.getItemId());
                if (item == null) continue;

                String name = item.getItemName();
                BigDecimal val = d.getValue();

                // 你薪酬项目初始化里就有这些名字:contentReference[oaicite:4]{index=4}
                if ("基本工资".equals(name)) baseSalary = val;
                else if (name.contains("补贴")) totalSubsidy = totalSubsidy.add(val);
                else if ("KPI 单价".equals(name) || name.contains("KPI")) kpiUnitPrice = val;
                else if ("全勤奖".equals(name)) attendanceBonus = val;
                else if ("旷工扣除".equals(name)) absencePenaltyPerDay = val;
                else if (name.contains("加班")) overtimePricePerHour = val;
            }

            // 3.4 这里 KPI/考勤/加班先置 0（草稿里再由你刚实现的弹窗/输入框填写）
            BigDecimal kpiUnits = BigDecimal.ZERO;
            int attendanceCount = 0;
            BigDecimal overtimeHours = BigDecimal.ZERO;

            // 3.5 计算 KPI_Bonus / Attendance_Adjustment / Overtime_Pay
            BigDecimal kpiBonus = kpiUnits.multiply(kpiUnitPrice);
            BigDecimal attendanceAdjustment = attendanceBonus
                    .subtract(absencePenaltyPerDay.multiply(BigDecimal.valueOf(0))); // 旷工天数这里简化成 0
            BigDecimal overtimePay = overtimeHours.multiply(overtimePricePerHour);

            // 3.6 Gross = 基本 + 补贴 + KPI + 考勤调整 + 加班
            BigDecimal grossMoney = baseSalary
                    .add(totalSubsidy)
                    .add(kpiBonus)
                    .add(attendanceAdjustment)
                    .add(overtimePay);

            // ---------- 4. 插入明细 ----------
            SalaryRegisterDetail detail = new SalaryRegisterDetail();
            detail.setRegisterId(registerId);
            detail.setUserId(emp.getUserId());
            detail.setStandardIdUsed(standard.getStandardId());

            detail.setKpiUnits(kpiUnits);                   // 对应表字段 KPI_Units:contentReference[oaicite:5]{index=5}
            detail.setAttendanceCount(attendanceCount);     // Attendance_Count:contentReference[oaicite:6]{index=6}
            detail.setOvertimeHours(overtimeHours);         // Overtime_Hours:contentReference[oaicite:7]{index=7}

            detail.setBaseSalary(baseSalary);
            detail.setTotalSubsidy(totalSubsidy);
            detail.setKpiBonus(kpiBonus);
            detail.setAttendanceAdjustment(attendanceAdjustment);
            detail.setOvertimePay(overtimePay);
            detail.setGrossMoney(grossMoney);

            detail.setPayrollMonth(master.getPayDate());

            registerDetailMapper.insert(detail);

            totalAmount = totalAmount.add(grossMoney);
        }

        // ---------- 5. 回写主表统计 ----------
        int totalPeople = Math.toIntExact(
                registerDetailMapper.selectCount(
                        new QueryWrapper<SalaryRegisterDetail>().eq("Register_ID", registerId)
                )
        );

        SalaryRegisterMaster update = new SalaryRegisterMaster();
        update.setRegisterId(registerId);
        update.setTotalPeople(totalPeople);
        update.setTotalAmount(totalAmount);

        registerMasterMapper.updateById(update);
    }

    // -------------------------
    // 3) HR查询历史标准
    // -------------------------
    public List<SalaryStandardMaster> getStandardsByOrg(Integer orgId) {
        return standardMasterMapper.selectList(
                new QueryWrapper<SalaryStandardMaster>()
                        .eq("L3_Org_ID", orgId)
                        .orderByDesc("Submission_Time")
        );
    }

    // -------------------------
    // 4) 查询标准详情
    // -------------------------
    public List<SalaryStandardDetail> getStandardDetails(Integer standardId) {
        return standardDetailMapper.selectList(
                new QueryWrapper<SalaryStandardDetail>()
                        .eq("Standard_ID", standardId)
        );
    }

    // -------------------------
    // 5) 审核薪酬标准
    // -------------------------
    @Transactional
    public void auditStandard(Integer standardId, Integer auditorId, boolean pass) {
        SalaryStandardMaster master = new SalaryStandardMaster();
        master.setStandardId(standardId);

        master.setAuditStatus(pass ? "Approved" : "Rejected");
        master.setAuditorId(auditorId);
        master.setAuditTime(LocalDateTime.now());

        standardMasterMapper.updateById(master);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }


}
