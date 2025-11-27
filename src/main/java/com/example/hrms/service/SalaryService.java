package com.example.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.SalaryStandardDTO;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SalaryService {

    @Autowired private SalaryStandardMasterMapper standardMasterMapper;
    @Autowired private SalaryStandardDetailMapper standardDetailMapper;
    @Autowired private SalaryRegisterMasterMapper registerMasterMapper;
    @Autowired private SalaryRegisterDetailMapper registerDetailMapper;
    @Autowired private UserMapper userMapper;

    // 1. HR提交薪酬标准
    @Transactional
    public void submitStandard(SalaryStandardDTO dto, Integer hrId, Integer orgId) {
        // 保存主表
        SalaryStandardMaster master = new SalaryStandardMaster();
        master.setStandardName(dto.getStandardName());
        master.setPositionId(dto.getPositionId());
        master.setL3OrgId(orgId);
        master.setSubmitterId(hrId);
        master.setAuditStatus("Pending");
        master.setSubmissionTime(LocalDateTime.now());
        standardMasterMapper.insert(master);

        // 保存详情
        if (dto.getItems() != null) {
            for (Map.Entry<Integer, BigDecimal> entry : dto.getItems().entrySet()) {
                SalaryStandardDetail detail = new SalaryStandardDetail();
                detail.setStandardId(master.getStandardId());
                detail.setItemId(entry.getKey());
                detail.setValue(entry.getValue());
                standardDetailMapper.insert(detail);
            }
        }
    }

    // 2. HR生成本月薪酬单 (核心计算)
    @Transactional
    public void createMonthlyRegister(Integer orgId) {
        // 查找该机构所有用户
        List<User> employees = userMapper.selectList(new QueryWrapper<User>().eq("L3_Org_ID", orgId));

        SalaryRegisterMaster regMaster = new SalaryRegisterMaster();
        regMaster.setL3OrgId(orgId);
        regMaster.setPayDate(LocalDate.now());
        regMaster.setAuditStatus("Pending");
        registerMasterMapper.insert(regMaster);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int count = 0;

        for (User emp : employees) {
            // 查找该员工职位对应的生效标准 (简化：假设一定有且只有一个Approved的标准)
            SalaryStandardMaster std = standardMasterMapper.selectOne(new QueryWrapper<SalaryStandardMaster>()
                    .eq("Position_ID", emp.getPositionId())
                    .eq("L3_Org_ID", orgId)
                    .eq("Audit_Status", "Approved")
                    .last("LIMIT 1"));

            if (std == null) continue;

            // 计算详情
            List<SalaryStandardDetail> details = standardDetailMapper.selectList(
                    new QueryWrapper<SalaryStandardDetail>().eq("Standard_ID", std.getStandardId()));

            BigDecimal baseSalary = BigDecimal.ZERO;
            BigDecimal bonus = BigDecimal.ZERO;

            for (SalaryStandardDetail d : details) {
                // 假设 itemId=1 是基本工资，itemId=2 是绩效基数
                if (d.getItemId() == 1) baseSalary = baseSalary.add(d.getValue());
                if (d.getItemId() == 2) bonus = bonus.add(d.getValue()); // 实际应乘以KPI系数
            }

            BigDecimal gross = baseSalary.add(bonus);

            SalaryRegisterDetail regDetail = new SalaryRegisterDetail();
            regDetail.setRegisterId(regMaster.getRegisterId());
            regDetail.setUserId(emp.getUserId());
            regDetail.setUserName(emp.getUsername());
            regDetail.setBaseSalary(baseSalary);
            regDetail.setKpiBonus(bonus);
            regDetail.setGrossMoney(gross);
            registerDetailMapper.insert(regDetail);

            totalAmount = totalAmount.add(gross);
            count++;
        }

        regMaster.setTotalAmount(totalAmount);
        regMaster.setTotalPeople(count);
        registerMasterMapper.updateById(regMaster);
    }

    // 3. 管理员审核标准
    public void auditStandard(Integer standardId, boolean pass) {
        SalaryStandardMaster master = standardMasterMapper.selectById(standardId);
        master.setAuditStatus(pass ? "Approved" : "Rejected");
        standardMasterMapper.updateById(master);
    }
}