package com.example.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.SalaryStandardDTO;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
    @Autowired private AttendanceMapper attendanceMapper;
    @Autowired private KPIItemRecordMapper kpiItemMapper;

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

    /**
     * 2. 一键登记本月工资 (修改版)
     * @param l3OrgId 机构ID
     * @param standardWorkDays 标准工作天数 (新参数)
     */
    @Transactional
    public void createMonthlyRegister(Integer l3OrgId, Integer standardWorkDays) {

        if (l3OrgId == null) throw new RuntimeException("机构ID不能为空");

        // 1. 创建主表 (Draft)
        SalaryRegisterMaster master = new SalaryRegisterMaster();
        master.setL3OrgId(l3OrgId);
        master.setRegisterTime(LocalDateTime.now());
        LocalDate now = LocalDate.now();
        // 工资月份统一用当月 1 号
        master.setPayDate(now.withDayOfMonth(1));
        master.setAuditStatus("Draft");
        master.setTotalPeople(0);
        master.setTotalAmount(BigDecimal.ZERO);
        master.setStandardWorkDays(standardWorkDays); // 保存标准天数

        // 生成薪资发放单号：PAY + yyyyMMdd + 两位流水号
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PAY" + dateStr;

        QueryWrapper<SalaryRegisterMaster> codeQuery = new QueryWrapper<>();
        codeQuery.likeRight("Register_Code", prefix);
        codeQuery.orderByDesc("Register_Code");
        codeQuery.last("LIMIT 1");

        SalaryRegisterMaster lastRegister = registerMasterMapper.selectOne(codeQuery);
        String newCode;
        if (lastRegister != null && lastRegister.getRegisterCode() != null) {
            String lastCode = lastRegister.getRegisterCode();
            String seqStr = lastCode.substring(prefix.length());
            int seq = Integer.parseInt(seqStr);
            newCode = prefix + String.format("%02d", seq + 1);
        } else {
            newCode = prefix + "01";
        }
        master.setRegisterCode(newCode);

        // 插入主表，拿到自增的 Register_ID
        registerMasterMapper.insert(master);
        Integer registerId = master.getRegisterId();

        // 2. 查询员工
        List<User> employees = userMapper.selectList(
                new QueryWrapper<User>()
                        .eq("L3_Org_ID", l3OrgId)
                        .eq("Is_Deleted", 0)
                        .ne("Position_ID", 1) // 排除管理员
        );
        if (employees == null || employees.isEmpty()) return;

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. 遍历员工生成明细
        for (User emp : employees) {
            // A. 获取生效薪酬标准
            SalaryStandardMaster standard = standardMasterMapper.selectOne(
                    new QueryWrapper<SalaryStandardMaster>()
                            .eq("L3_Org_ID", l3OrgId)
                            .eq("Position_ID", emp.getPositionId())
                            .eq("Audit_Status", "Approved")
                            .orderByDesc("Standard_ID")
                            .last("LIMIT 1")
            );
            if (standard == null) continue;

            List<SalaryStandardDetail> stdDetails = standardDetailMapper.selectList(
                    new QueryWrapper<SalaryStandardDetail>().eq("Standard_ID", standard.getStandardId())
            );

            // B. 提取标准基数
            BigDecimal baseSalary = BigDecimal.ZERO;
            BigDecimal totalSubsidy = BigDecimal.ZERO;
            BigDecimal kpiUnitPrice = BigDecimal.ZERO;       // KPI 每分多少钱 (或系数基数)
            BigDecimal attendanceBonusStd = BigDecimal.ZERO; // 全勤奖标准
            BigDecimal absencePenaltyDay = BigDecimal.ZERO;  // 缺勤一天扣多少
            BigDecimal insuranceRatioSum = BigDecimal.ZERO;  // 保险系数汇总

            for (SalaryStandardDetail d : stdDetails) {
                SalaryItem item = itemMapper.selectById(d.getItemId());
                if (item == null) continue;
                String name = item.getItemName();
                BigDecimal val = d.getValue();

                if ("基本工资".equals(name)) {
                    baseSalary = val;
                } else if (name.contains("补贴")) {
                    totalSubsidy = totalSubsidy.add(val);
                } else if (name.contains("KPI")) {
                    kpiUnitPrice = val;
                } else if ("全勤奖".equals(name)) {
                    attendanceBonusStd = val;
                } else if ("旷工扣除".equals(name)) {
                    absencePenaltyDay = val;
                } else if (name.contains("保险")) {
                    // 保险项目：value 为系数，后面统一乘以工资基数
                    insuranceRatioSum = insuranceRatioSum.add(val);
                }
            }

            // C. 自动计算考勤：统计该员工当月打卡天数
            YearMonth ym = YearMonth.from(master.getPayDate());
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            Long actualDaysLong = attendanceMapper.selectCount(
                    new QueryWrapper<AttendanceRecord>()
                            .eq("User_ID", emp.getUserId())
                            .between("Punch_Date", start, end)
            );
            int actualDays = (actualDaysLong != null) ? actualDaysLong.intValue() : 0;

            BigDecimal attendanceAdjustment = BigDecimal.ZERO;
            if (actualDays >= standardWorkDays) {
                // 全勤：给全勤奖
                attendanceAdjustment = attendanceBonusStd;
            } else {
                // 缺勤：扣款 = (标准-实际) * 单日扣款
                int missedDays = standardWorkDays - actualDays;
                if (missedDays > 0) {
                    attendanceAdjustment = absencePenaltyDay
                            .multiply(BigDecimal.valueOf(missedDays))
                            .negate();
                }
            }

            // D. 初始化 KPI (默认为 0，等待经理录入)
            BigDecimal kpiUnits = BigDecimal.ZERO;
            BigDecimal kpiBonus = BigDecimal.ZERO; // 录入 KPI 后再更新

            // E. 计算保险扣除 & 实发工资
            // 保险 = 工资基数 * 保险系数之和，这里先用基本工资作为基数
            BigDecimal insuranceBase = baseSalary;
            BigDecimal insuranceFee = insuranceBase
                    .multiply(insuranceRatioSum)
                    .setScale(2, RoundingMode.HALF_UP);

            // 实发工资 = 基本工资 + 补贴 + 绩效奖金 - 保险 + 考勤奖惩
            BigDecimal grossMoney = baseSalary
                    .add(totalSubsidy)
                    .add(kpiBonus)
                    .add(attendanceAdjustment)
                    .subtract(insuranceFee);

            // F. 存入明细
            SalaryRegisterDetail detail = new SalaryRegisterDetail();
            detail.setRegisterId(registerId);          // 这里用的就是上面定义的 registerId
            detail.setUserId(emp.getUserId());
            detail.setStandardIdUsed(standard.getStandardId());

            detail.setAttendanceCount(actualDays);
            detail.setKpiUnits(kpiUnits);

            detail.setBaseSalary(baseSalary);
            detail.setTotalSubsidy(totalSubsidy);
            detail.setKpiBonus(kpiBonus);
            detail.setAttendanceAdjustment(attendanceAdjustment);
            detail.setInsuranceFee(insuranceFee);     // 新增：保险扣除
            detail.setGrossMoney(grossMoney);
            detail.setPayrollMonth(master.getPayDate());

            registerDetailMapper.insert(detail);
            totalAmount = totalAmount.add(grossMoney);
        }

        // 4. 更新主表统计
        master.setTotalPeople(employees.size()); // 这里简化，实际应为插入条数
        master.setTotalAmount(totalAmount);
        registerMasterMapper.updateById(master);
    }


    /**
     * 新增功能：录入 KPI 分数并重算工资
     * @param detailId 工资明细ID
     * @param kpiItems 指标列表 (ItemName -> Score)
     */
    @Transactional
    public void updateEmployeeKPI(Integer detailId, List<KPIItemRecord> kpiItems) {
        // 1. 获取当前明细和主表
        SalaryRegisterDetail detail = registerDetailMapper.selectById(detailId);
        SalaryRegisterMaster master = registerMasterMapper.selectById(detail.getRegisterId());

        // 2. 保存/更新 KPI 分项记录
        // 先删旧的 (简单粗暴)
        kpiItemMapper.delete(new QueryWrapper<KPIItemRecord>().eq("Register_Detail_ID", detailId));

        BigDecimal totalScoreWeighted = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (KPIItemRecord item : kpiItems) {
            item.setRegisterDetailId(detailId);
            kpiItemMapper.insert(item);

            // 累加加权分: Score * Weight
            totalScoreWeighted = totalScoreWeighted.add(item.getScore().multiply(item.getWeight()));
            totalWeight = totalWeight.add(item.getWeight());
        }

        // 3. 计算基础 KPI Units (假设满分100对应系数1.0，或者直接用总分)
        // 策略：KPI_Units = 加权总分 / 100
        BigDecimal kpiBaseUnits = BigDecimal.ZERO;
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            kpiBaseUnits = totalScoreWeighted.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        // 4. 应用考勤门槛规则
        // 规则：出勤 < 80% (标准天数 * 0.8)，KPI Units 打8折
        BigDecimal finalKpiUnits = kpiBaseUnits;

        double standard = master.getStandardWorkDays();
        double actual = detail.getAttendanceCount();

        if (standard > 0 && actual < (standard * 0.8)) {
            finalKpiUnits = finalKpiUnits.multiply(new BigDecimal("0.8"));
        }

        // 5. 重新计算 KPI 金额
        SalaryStandardDetail stdDetailKpi = standardDetailMapper.selectOne(
                new QueryWrapper<SalaryStandardDetail>()
                        .eq("Standard_ID", detail.getStandardIdUsed())
                        .inSql("Item_ID", "SELECT Item_ID FROM T_Salary_Item WHERE Item_Name LIKE '%KPI%'")
                        .last("LIMIT 1")
        );
        BigDecimal kpiPrice = (stdDetailKpi != null) ? stdDetailKpi.getValue() : BigDecimal.ZERO;
        BigDecimal kpiBonus = finalKpiUnits.multiply(kpiPrice);

        // 6. 更新明细表 (Gross Money 也要重算)
        BigDecimal insuranceFee = detail.getInsuranceFee() != null ? detail.getInsuranceFee() : BigDecimal.ZERO;

        BigDecimal newGross = detail.getBaseSalary()
                .add(detail.getTotalSubsidy())
                .add(kpiBonus)
                .subtract(insuranceFee);
        // 如果考勤奖惩也要计入实发，可在这里 + detail.getAttendanceAdjustment()

        detail.setKpiUnits(finalKpiUnits);
        detail.setKpiBonus(kpiBonus);
        detail.setInsuranceFee(insuranceFee);
        detail.setGrossMoney(newGross);

        registerDetailMapper.updateById(detail);

        // 7. 重新汇总主表总金额
        recalculateMasterTotal(master.getRegisterId());
    }

    // -------------------------------------------------------------------------
    // 私有辅助方法：重新计算并更新工资单主表的统计数据（总人数、总金额）
    // -------------------------------------------------------------------------
    private void recalculateMasterTotal(Integer registerId) {
        if (registerId == null) return;

        // 1. 查询该工资单下的所有明细记录
        List<SalaryRegisterDetail> details = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>()
                        .eq("Register_ID", registerId)
        );

        // 2. 准备更新对象
        SalaryRegisterMaster masterUpdate = new SalaryRegisterMaster();
        masterUpdate.setRegisterId(registerId);

        if (details == null || details.isEmpty()) {
            // 如果没有明细，说明被清空了，直接归零
            masterUpdate.setTotalPeople(0);
            masterUpdate.setTotalAmount(BigDecimal.ZERO);
        } else {
            // 3. 内存计算总金额 (防止 GrossMoney 为 null)
            BigDecimal totalAmount = details.stream()
                    .map(SalaryRegisterDetail::getGrossMoney)
                    .filter(amount -> amount != null) // 过滤掉潜在的 null 值
                    .reduce(BigDecimal.ZERO, BigDecimal::add); // 累加

            // 4. 设置统计值
            masterUpdate.setTotalPeople(details.size());
            masterUpdate.setTotalAmount(totalAmount);
        }

        // 5. 执行更新
        registerMasterMapper.updateById(masterUpdate);
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




}
