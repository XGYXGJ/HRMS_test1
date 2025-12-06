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

        LocalDate now = LocalDate.now();

        Long count = registerMasterMapper.selectCount(
                new QueryWrapper<SalaryRegisterMaster>()
                        .eq("L3_Org_ID", l3OrgId)
                        .apply("DATE_FORMAT(Pay_Date, '%Y-%m') = {0}",
                                now.toString().substring(0, 7))
        );

        if (count > 0) {
            throw new RuntimeException("该部门本月工资已登记，请勿重复操作！");
        }

        List<User> employees = userMapper.selectList(
                new QueryWrapper<User>().eq("L3_Org_ID", l3OrgId)
        );
        if (employees.isEmpty()) return;

        Map<Integer, String> itemTypeMap = itemMapper.selectList(null).stream()
                .collect(Collectors.toMap(SalaryItem::getItemId, SalaryItem::getItemType));

        SalaryRegisterMaster regMaster = new SalaryRegisterMaster();
        regMaster.setL3OrgId(l3OrgId);
        regMaster.setAuditStatus("Pending");
        regMaster.setPayDate(now);
        regMaster.setTotalAmount(BigDecimal.ZERO);
        regMaster.setTotalPeople(0);

        registerMasterMapper.insert(regMaster);

        LocalDate payrollMonth = regMaster.getPayDate();

        BigDecimal totalAmount = BigDecimal.ZERO;
        int peopleCount = 0;

        for (User emp : employees) {

            SalaryStandardMaster validStandard =
                    standardMasterMapper.selectOne(
                            new QueryWrapper<SalaryStandardMaster>()
                                    .eq("L3_Org_ID", l3OrgId)
                                    .eq("Position_ID", emp.getPositionId())
                                    .eq("Audit_Status", "Approved")
                                    .orderByDesc("Submission_Time")
                                    .last("LIMIT 1")
                    );

            if (validStandard == null) {
                continue;
            }

            List<SalaryStandardDetail> standardDetails =
                    standardDetailMapper.selectList(
                            new QueryWrapper<SalaryStandardDetail>()
                                    .eq("Standard_ID", validStandard.getStandardId())
                    );

            BigDecimal baseSalary = BigDecimal.ZERO;
            BigDecimal totalSubsidy = BigDecimal.ZERO;
            BigDecimal totalBonusAndKpi = BigDecimal.ZERO;
            BigDecimal grossMoney = BigDecimal.ZERO;

            for (SalaryStandardDetail d : standardDetails) {
                String type = itemTypeMap.get(d.getItemId());
                if ("Base".equalsIgnoreCase(type)) {
                    baseSalary = nz(d.getValue());
                    grossMoney = grossMoney.add(baseSalary);
                    break;
                }
            }

            BigDecimal kpiUnits = BigDecimal.ZERO;

            for (SalaryStandardDetail d : standardDetails) {

                String type = itemTypeMap.get(d.getItemId());
                if (type == null || "Base".equalsIgnoreCase(type)) continue;

                BigDecimal val = nz(d.getValue());

                if ("Subsidy".equalsIgnoreCase(type)) {
                    totalSubsidy = totalSubsidy.add(val);
                    grossMoney = grossMoney.add(val);
                    continue;
                }

                if ("Penalty".equalsIgnoreCase(type)) {
                    grossMoney = grossMoney.subtract(val);
                    continue;
                }

                if ("Ratio".equalsIgnoreCase(type)) {
                    BigDecimal ratioAmount = baseSalary.multiply(val);
                    totalBonusAndKpi = totalBonusAndKpi.add(ratioAmount);
                    grossMoney = grossMoney.add(ratioAmount);
                    continue;
                }

                if ("Bonus".equalsIgnoreCase(type)) {
                    totalBonusAndKpi = totalBonusAndKpi.add(val);
                    grossMoney = grossMoney.add(val);
                    continue;
                }

                if ("KPI".equalsIgnoreCase(type)) {
                    BigDecimal kpiAmount = val.multiply(kpiUnits);
                    totalBonusAndKpi = totalBonusAndKpi.add(kpiAmount);
                    grossMoney = grossMoney.add(kpiAmount);
                }
            }

            SalaryRegisterDetail regDetail = new SalaryRegisterDetail();
            regDetail.setRegisterId(regMaster.getRegisterId());
            regDetail.setUserId(emp.getUserId());
            regDetail.setStandardIdUsed(validStandard.getStandardId());

            regDetail.setBaseSalary(baseSalary);
            regDetail.setTotalSubsidy(totalSubsidy);
            regDetail.setKpiBonus(totalBonusAndKpi);

            regDetail.setAttendanceAdjustment(BigDecimal.ZERO);
            regDetail.setOvertimePay(BigDecimal.ZERO);

            regDetail.setGrossMoney(grossMoney);
            regDetail.setPayrollMonth(payrollMonth);

            registerDetailMapper.insert(regDetail);

            totalAmount = totalAmount.add(grossMoney);
            peopleCount++;
        }

        regMaster.setTotalAmount(totalAmount);
        regMaster.setTotalPeople(peopleCount);
        registerMasterMapper.updateById(regMaster);
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
