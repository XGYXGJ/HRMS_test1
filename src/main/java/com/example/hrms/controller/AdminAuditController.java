package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.SalaryRegisterMaster;
import com.example.hrms.entity.SalaryStandardMaster;
import com.example.hrms.mapper.SalaryRegisterMasterMapper;
import com.example.hrms.mapper.SalaryStandardMasterMapper;
import com.example.hrms.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    @Autowired private SalaryStandardMasterMapper standardMasterMapper;
    @Autowired private SalaryService salaryService;
    @Autowired private SalaryRegisterMasterMapper registerMasterMapper;

    // 列表：待审核的薪酬标准
    @GetMapping("/standards")
    public String listPendingStandards(Model model) {
        model.addAttribute("standards", standardMasterMapper.selectList(
                new QueryWrapper<SalaryStandardMaster>().eq("Audit_Status", "Pending")
        ));
        return "admin/audit_standards";
    }

    // 动作：通过/拒绝
    @GetMapping("/standard/{id}/{action}")
    public String auditStandard(@PathVariable Integer id, @PathVariable String action) {
        boolean pass = "pass".equals(action);
        salaryService.auditStandard(id, pass);
        return "redirect:/admin/audit/standards";
    }



    // 1. 查看待审核的工资单列表
    @GetMapping("/registers")
    public String listPendingRegisters(Model model) {
        // 联表查询最好写XML，这里为了演示简单，直接查单表，页面上可能只能显示OrgID
        model.addAttribute("registers", registerMasterMapper.selectList(
                new QueryWrapper<SalaryRegisterMaster>().eq("Audit_Status", "Pending")
        ));
        return "admin/audit_registers";
    }

    // 2. 审核通过/驳回
    @GetMapping("/register/{id}/{action}")
    public String auditRegister(@PathVariable Integer id, @PathVariable String action) {
        SalaryRegisterMaster reg = registerMasterMapper.selectById(id);
        reg.setAuditStatus("pass".equals(action) ? "Approved" : "Rejected");
        // 实际场景：如果Rejected，可能需要级联删除Detail或通知HR
        registerMasterMapper.updateById(reg);
        return "redirect:/admin/audit/registers";
    }
}