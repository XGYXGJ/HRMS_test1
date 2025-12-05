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
import java.util.HashMap;
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

    // 1. 提交薪酬标准 - 【已修复】
    @Transactional
    public void submitStandard(SalaryStandardDTO dto, Integer submitterId, Integer l3OrgId) {
        // 保存 Master
        SalaryStandardMaster master = new SalaryStandardMaster();

        // 【关键修复点】从 DTO 获取 Standard_Code 并赋值
        master.setStandardCode(dto.getStandardCode());

        master.setStandardName(dto.getStandardName());
        master.setL3OrgId(l3OrgId);
        master.setPositionId(dto.getPositionId());
        master.setSubmitterId(submitterId);
        master.setAuditStatus("Pending");
        master.setSubmissionTime(LocalDateTime.now());

        // 插入主表
        standardMasterMapper.insert(master);

        // 保存 Details
        if (dto.getItems() != null) {
            // master.getStandardId() 在 insert 后会被 Mybatis-Plus 回填
            Integer standardId = master.getStandardId();

            for (Map.Entry<Integer, BigDecimal> entry : dto.getItems().entrySet()) {
                // 确保值不为空
                BigDecimal value = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;

                SalaryStandardDetail detail = new SalaryStandardDetail();
                detail.setStandardId(standardId);
                detail.setItemId(entry.getKey());
                detail.setValue(value);
                standardDetailMapper.insert(detail);
            }
        }
    }

    // 2. 审核薪酬标准 (保持不变)
    public void auditStandard(Integer standardId, boolean pass) {
        SalaryStandardMaster master = standardMasterMapper.selectById(standardId);
        if (master != null) {
            master.setAuditStatus(pass ? "Approved" : "Rejected");
            standardMasterMapper.updateById(master);
        }
    }

    // 3. 一键登记本月工资 (保持不变)
    @Transactional
    public void createMonthlyRegister(Integer l3OrgId) {
        LocalDate now = LocalDate.now();

        Long count = registerMasterMapper.selectCount(new QueryWrapper<SalaryRegisterMaster>()
                .eq("L3_Org_ID", l3OrgId)
                .apply("DATE_FORMAT(Pay_Date, '%Y-%m') = {0}", now.toString().substring(0, 7)));

        if (count > 0) {
            throw new RuntimeException("该部门本月工资已登记，请勿重复操作！");
        }

        List<User> employees = userMapper.selectList(new QueryWrapper<User>().eq("L3_Org_ID", l3OrgId));
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

        BigDecimal totalAmount = BigDecimal.ZERO;
        int peopleCount = 0;

        for (User emp : employees) {
            SalaryStandardMaster validStandard = standardMasterMapper.selectOne(new QueryWrapper<SalaryStandardMaster>()
                    .eq("L3_Org_ID", l3OrgId)
                    .eq("Position_ID", emp.getPositionId())
                    .eq("Audit_Status", "Approved")
                    .orderByDesc("Submission_Time")
                    .last("LIMIT 1"));

            if (validStandard == null) {
                continue;
            }

            List<SalaryStandardDetail> standardDetails = standardDetailMapper.selectList(
                    new QueryWrapper<SalaryStandardDetail>().eq("Standard_ID", validStandard.getStandardId())
            );

            BigDecimal grossMoney = BigDecimal.ZERO;
            BigDecimal baseSalary = BigDecimal.ZERO;

            // 第一步：先找出基本工资
            for (SalaryStandardDetail itemDetail : standardDetails) {
                String type = itemTypeMap.get(itemDetail.getItemId());
                if ("Base".equalsIgnoreCase(type)) {
                    baseSalary = itemDetail.getValue();
                    grossMoney = grossMoney.add(baseSalary);
                    break;
                }
            }

            // 第二步：处理其他项目
            for (SalaryStandardDetail itemDetail : standardDetails) {
                String type = itemTypeMap.get(itemDetail.getItemId());
                BigDecimal val = itemDetail.getValue();

                if (type == null || "Base".equalsIgnoreCase(type)) continue;

                if ("Ratio".equalsIgnoreCase(type)) {
                    // *** 核心修改：Ratio 作为系数项 (例如保险、公积金、绩效比例) ***
                    // val 是系数，计算金额 = baseSalary * 系数
                    BigDecimal calculatedAmount = baseSalary.multiply(val);
                    // Ratio项根据系数正负自动加减
                    grossMoney = grossMoney.add(calculatedAmount);
                } else if ("Penalty".equalsIgnoreCase(type)) {
                    // Penalty 作为固定金额扣款
                    grossMoney = grossMoney.subtract(val);
                } else {
                    // Bonus, Subsidy 等作为固定金额加项
                    grossMoney = grossMoney.add(val);
                }
            }

            // 保存明细 Detail
            SalaryRegisterDetail regDetail = new SalaryRegisterDetail();
            regDetail.setRegisterId(regMaster.getRegisterId());
            regDetail.setUserId(emp.getUserId());
            regDetail.setUserName(emp.getUsername());
            regDetail.setBaseSalary(baseSalary);
            regDetail.setKpiBonus(BigDecimal.ZERO);
            regDetail.setGrossMoney(grossMoney);

            registerDetailMapper.insert(regDetail);

            totalAmount = totalAmount.add(grossMoney);
            peopleCount++;
        }

        regMaster.setTotalAmount(totalAmount);
        regMaster.setTotalPeople(peopleCount);
        registerMasterMapper.updateById(regMaster);
    }

    // 4. 供HR查询历史标准 (保持不变)
    public List<SalaryStandardMaster> getStandardsByOrg(Integer orgId) {
        return standardMasterMapper.selectList(
                new QueryWrapper<SalaryStandardMaster>().eq("L3_Org_ID", orgId).orderByDesc("Submission_Time")
        );
    }

    // 5. 查询标准详情 (保持不变)
    public List<SalaryStandardDetail> getStandardDetails(Integer standardId) {
        return standardDetailMapper.selectList(
                new QueryWrapper<SalaryStandardDetail>().eq("Standard_ID", standardId)
        );
    }
}