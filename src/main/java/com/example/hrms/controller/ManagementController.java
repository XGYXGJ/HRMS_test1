package com.example.hrms.controller;


import com.example.hrms.mapper.SalaryItemMapper;
import com.example.hrms.service.SalaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage")
public class ManagementController {
    @Autowired
    private SalaryService salaryService;
    @Autowired
    private SalaryItemMapper itemMapper;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // 在仪表盘简单展示一些信息，或者直接返回视图
        return "manage/dashboard";
    }
}
