package com.example.hrms.controller;

import com.example.hrms.entity.Organization;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.service.OrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrgService orgService;

    @Autowired
    private PersonnelFileMapper fileMapper;

    // 1. 管理员主页
    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    // 2. 机构添加页面
    @GetMapping("/org/add")
    public String addOrgPage(Model model) {
        // 加载一级机构供选择
        model.addAttribute("level1Orgs", orgService.getLevel1Orgs());
        model.addAttribute("org", new Organization());
        return "admin/org_add";
    }

    // 3. 处理机构添加请求
    @PostMapping("/org/save")
    public String saveOrg(Organization org) {
        orgService.addOrg(org);
        return "redirect:/admin/org/add?success";
    }

    // 4. 档案列表查询页面 (实现模糊查询)
    @GetMapping("/files")
    public String listFiles(Model model) {
        // 这里直接调用Mapper演示，实际应在Service层
        model.addAttribute("files", fileMapper.selectFilesWithOrgName());
        return "admin/file_list";
    }
}