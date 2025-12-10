package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.SalaryRegisterMaster;
import com.example.hrms.entity.SalaryStandardMaster;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.SalaryRegisterMasterMapper;
import com.example.hrms.mapper.SalaryStandardMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage")
public class ManagementController {

    @Autowired
    private SalaryRegisterMasterMapper registerMasterMapper;

    @Autowired
    private SalaryStandardMasterMapper standardMasterMapper;

    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "manage/dashboard";
    }

    @GetMapping("/home")
    public String home(Model model) {
        // 待审核工资单数量
        long pendingCount = registerMasterMapper.selectCount(
                new QueryWrapper<SalaryRegisterMaster>().eq("audit_status", "Pending")
        );
        model.addAttribute("pendingCount", pendingCount);

        // 待审核薪酬标准数量
        long pendingStandardCount = standardMasterMapper.selectCount(
                new QueryWrapper<SalaryStandardMaster>().eq("audit_status", "Pending")
        );
        model.addAttribute("pendingStandardCount", pendingStandardCount);

        // 待审核档案数量
        long pendingPersonnelCount = personnelFileMapper.selectCount(
                new QueryWrapper<PersonnelFile>().eq("audit_status", "Pending")
        );
        model.addAttribute("pendingPersonnelCount", pendingPersonnelCount);

        return "manage/dashboard_home";
    }
}
