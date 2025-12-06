// src/main/java/com/example/hrms/controller/EmployeeController.java (修改)
package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.SalaryRegisterDetail;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
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
    @Autowired private PersonnelFileMapper personnelFileMapper;
    @Autowired private PositionMapper positionMapper;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        // 1. 查询个人档案信息
        PersonnelFile profile = personnelFileMapper.selectOne(
                new QueryWrapper<PersonnelFile>().eq("User_ID", user.getUserId())
        );
        model.addAttribute("profile", profile);

        // 2. 查询职位信息
        if (user.getPositionId() != null) {
            Position position = positionMapper.selectById(user.getPositionId());
            model.addAttribute("position", position);
        }

        // 3. 查询已批准的工资单
       // List<SalaryRegisterDetail> salaryList = registerDetailMapper.selectApprovedDetailsByUserId(user.getUserId());
        //model.addAttribute("salaryList", salaryList);

        return "emp/home";
    }
}
