package com.example.hrms.controller;

import com.example.hrms.dto.SalaryStandardDTO;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.SalaryItemMapper;
import com.example.hrms.service.SalaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/hr")
public class HRController {

    @Autowired private SalaryService salaryService;
    @Autowired private SalaryItemMapper itemMapper;

    @GetMapping("/dashboard")
    public String dashboard() { return "hr/dashboard"; }

    // 1. 制定薪酬标准页面
    @GetMapping("/standard/new")
    public String createStandardPage(Model model) {
        model.addAttribute("items", itemMapper.selectList(null)); // 加载所有薪酬项目
        return "hr/standard_add";
    }

    // 2. 提交薪酬标准
    @PostMapping("/standard/save")
    public String saveStandard(SalaryStandardDTO dto, HttpSession session) {
        User hr = (User) session.getAttribute("user");
        salaryService.submitStandard(dto, hr.getUserId(), hr.getL3OrgId());
        return "redirect:/hr/dashboard?msg=StandardSubmitted";
    }

    // 3. 一键登记本月工资
    @PostMapping("/salary/register")
    public String registerSalary(HttpSession session) {
        User hr = (User) session.getAttribute("user");
        salaryService.createMonthlyRegister(hr.getL3OrgId());
        return "redirect:/hr/dashboard?msg=SalaryRegistered";
    }
}