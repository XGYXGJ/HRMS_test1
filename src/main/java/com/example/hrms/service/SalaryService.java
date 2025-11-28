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

    // 1. 提交薪酬标准 (保持不变，但为了完整性列出)
    @Transactional
    public void submitStandard(SalaryStandardDTO dto, Integer submitterId, Integer l3OrgId) {
        // 保存 Master
        SalaryStandardMaster master = new SalaryStandardMaster();
        master.setStandardName(dto.getStandardName());
        master.setL3OrgId(l3OrgId);
        master.setPositionId(dto.getPositionId());
        master.setSubmitterId(submitterId);
        master.setAuditStatus("Pending");
        master.setSubmissionTime(LocalDateTime.now());
        standardMasterMapper.insert(master);

        // 保存 Details
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

    // 2. 审核薪酬标准
    public void auditStandard(Integer standardId, boolean pass) {
        SalaryStandardMaster master = standardMasterMapper.selectById(standardId);
        if (master != null) {
            master.setAuditStatus(pass ? "Approved" : "Rejected");
            standardMasterMapper.updateById(master);
        }
    }

    // 3. 【核心修改】一键登记本月工资
    @Transactional
    public void createMonthlyRegister(Integer l3OrgId) {
        LocalDate now = LocalDate.now();
        // 简单处理：假设每月1号发上个月工资，或者当月工资
        // 这里的逻辑是检查本月是否已经为该部门创建过工资单，防止重复发放
        Long count = registerMasterMapper.selectCount(new QueryWrapper<SalaryRegisterMaster>()
                .eq("L3_Org_ID", l3OrgId)
                .apply("DATE_FORMAT(Pay_Date, '%Y-%m') = {0}", now.toString().substring(0, 7))); // 检查 YYYY-MM

        if (count > 0) {
            throw new RuntimeException("该部门本月工资已登记，请勿重复操作！");
        }

        // 1. 获取该部门所有员工
        List<User> employees = userMapper.selectList(new QueryWrapper<User>().eq("L3_Org_ID", l3OrgId));
        if (employees.isEmpty()) return;

        // 2. 准备基础数据：所有薪酬项目类型 (用于判断是加钱还是扣钱)
        Map<Integer, String> itemTypeMap = itemMapper.selectList(null).stream()
                .collect(Collectors.toMap(SalaryItem::getItemId, SalaryItem::getItemType));

        // 3. 创建总表 Master
        SalaryRegisterMaster regMaster = new SalaryRegisterMaster();
        regMaster.setL3OrgId(l3OrgId);
        regMaster.setAuditStatus("Pending");
        regMaster.setPayDate(now);
        regMaster.setTotalAmount(BigDecimal.ZERO); // 稍后累加
        regMaster.setTotalPeople(0);
        registerMasterMapper.insert(regMaster);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int peopleCount = 0;

        // 4. 为每个员工计算工资
        for (User emp : employees) {
            // 【逻辑修复】：查找该员工对应职位、且状态为 Approved 的最新薪酬标准
            SalaryStandardMaster validStandard = standardMasterMapper.selectOne(new QueryWrapper<SalaryStandardMaster>()
                    .eq("L3_Org_ID", l3OrgId)
                    .eq("Position_ID", emp.getPositionId())
                    .eq("Audit_Status", "Approved") // 必须是已审核通过的
                    .orderByDesc("Submission_Time") // 取最新的
                    .last("LIMIT 1"));

            if (validStandard == null) {
                // 如果没有定标准，跳过该员工或记录错误 (这里选择跳过)
                continue;
            }

            // 计算该标准的总金额
            List<SalaryStandardDetail> standardDetails = standardDetailMapper.selectList(
                    new QueryWrapper<SalaryStandardDetail>().eq("Standard_ID", validStandard.getStandardId())
            );

            BigDecimal grossMoney = BigDecimal.ZERO;
            BigDecimal baseSalary = BigDecimal.ZERO; // 用于记录基本工资方便展示

            for (SalaryStandardDetail itemDetail : standardDetails) {
                String type = itemTypeMap.get(itemDetail.getItemId());
                BigDecimal val = itemDetail.getValue();

                if (type == null) type = "Bonus"; // 默认加项

                // 【逻辑修复】：根据类型判断加减
                if ("Base".equalsIgnoreCase(type)) {
                    grossMoney = grossMoney.add(val);
                    baseSalary = val;
                } else if ("Deduction".equalsIgnoreCase(type) || "Tax".equalsIgnoreCase(type)) {
                    grossMoney = grossMoney.subtract(val);
                } else {
                    // Bonus, Subsidy 等
                    grossMoney = grossMoney.add(val);
                }
            }

            // 保存明细 Detail
            SalaryRegisterDetail regDetail = new SalaryRegisterDetail();
            regDetail.setRegisterId(regMaster.getRegisterId());
            regDetail.setUserId(emp.getUserId());
            regDetail.setUserName(emp.getUsername()); // 需要确保User实体里有这个字段，或者通过Join查
            regDetail.setBaseSalary(baseSalary);
            regDetail.setKpiBonus(BigDecimal.ZERO); // 暂时没有KPI模块，设为0
            regDetail.setGrossMoney(grossMoney);

            registerDetailMapper.insert(regDetail);

            totalAmount = totalAmount.add(grossMoney);
            peopleCount++;
        }

        // 5. 更新总表数据
        regMaster.setTotalAmount(totalAmount);
        regMaster.setTotalPeople(peopleCount);
        registerMasterMapper.updateById(regMaster);
    }

    // 4. 【新增功能】供HR查询历史标准
    public List<SalaryStandardMaster> getStandardsByOrg(Integer orgId) {
        return standardMasterMapper.selectList(
                new QueryWrapper<SalaryStandardMaster>().eq("L3_Org_ID", orgId).orderByDesc("Submission_Time")
        );
    }

    // 5. 【新增功能】查询标准详情
    public List<SalaryStandardDetail> getStandardDetails(Integer standardId) {
        return standardDetailMapper.selectList(
                new QueryWrapper<SalaryStandardDetail>().eq("Standard_ID", standardId)
        );
    }
}