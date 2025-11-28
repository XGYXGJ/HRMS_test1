package com.example.hrms.controller;

import com.example.hrms.entity.SalaryRegisterDetail;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.SalaryRegisterDetailMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/emp")
public class EmployeeController {

    @Autowired private SalaryRegisterDetailMapper registerDetailMapper;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        // 【修改点】: 这里不再使用 selectList，而是调用自定义方法
        // 只查询关联 Master 表状态为 'Approved' 的记录
        List<SalaryRegisterDetail> salaryList = registerDetailMapper.selectApprovedDetailsByUserId(user.getUserId());

        model.addAttribute("salaryList", salaryList);
        return "emp/home";
    }
}