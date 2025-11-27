package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

        // 查询属于该员工的所有工资明细
        // 严谨做法：还应关联查询 Master 表，确保 Audit_Status = 'Approved'
        List<SalaryRegisterDetail> salaryList = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>().eq("User_ID", user.getUserId())
        );

        model.addAttribute("salaryList", salaryList);
        return "emp/home";
    }
}