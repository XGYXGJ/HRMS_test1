package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.SalaryRegisterMaster;
import com.example.hrms.entity.SalaryStandardMaster;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.SalaryRegisterMasterMapper;
import com.example.hrms.mapper.SalaryStandardMasterMapper;
import com.example.hrms.service.SalaryService;
import jakarta.servlet.http.HttpSession;
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

    @GetMapping("/standards")
    public String listPendingStandards(Model model) {
        model.addAttribute("standards", standardMasterMapper.selectList(
                new QueryWrapper<SalaryStandardMaster>().eq("Audit_Status", "Pending")
        ));
        return "admin/audit_standards";
    }

    @GetMapping("/standard/{id}/{action}")
    public String auditStandard(@PathVariable Integer id, @PathVariable String action, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null) {
            return "redirect:/login"; // Or handle error
        }
        boolean pass = "pass".equals(action);
        salaryService.auditStandard(id, admin.getUserId(), pass);
        return "redirect:/admin/audit/standards";
    }

    @GetMapping("/registers")
    public String listPendingRegisters(Model model) {
        model.addAttribute("registers", registerMasterMapper.selectList(
                new QueryWrapper<SalaryRegisterMaster>().eq("Audit_Status", "Pending")
        ));
        return "admin/audit_registers";
    }

    @GetMapping("/register/{id}/{action}")
    public String auditRegister(@PathVariable Integer id, @PathVariable String action) {
        SalaryRegisterMaster reg = registerMasterMapper.selectById(id);
        reg.setAuditStatus("pass".equals(action) ? "Approved" : "Rejected");
        registerMasterMapper.updateById(reg);
        return "redirect:/admin/audit/registers";
    }
}