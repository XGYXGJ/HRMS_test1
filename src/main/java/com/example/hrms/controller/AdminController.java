package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.Organization;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.OrganizationMapper;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.UserMapper;
import com.example.hrms.service.OrgService;
import com.example.hrms.service.OrganizationService;
import com.example.hrms.service.PersonnelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrgService orgService;

    @Autowired
    private PersonnelFileMapper fileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PersonnelService personnelService;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private OrganizationService organizationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

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
    public String listFiles(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("files", fileMapper.selectFilesWithOrgName(null, q));
        return "admin/file_list";
    }

    // 5. 新建账号页面
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        List<Organization> level3Orgs = organizationMapper.selectList(new QueryWrapper<Organization>().eq("level", 3));
        List<Map<String, Object>> orgsWithFullName = level3Orgs.stream()
                .map(org -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orgId", org.getOrgId());
                    map.put("fullName", organizationService.getFullOrgName(org.getOrgId()));
                    return map;
                })
                .collect(Collectors.toList());
        model.addAttribute("level3Orgs", orgsWithFullName);
        return "admin/user_form";
    }

    // 6. 处理新建账号
    @PostMapping("/users/create")
    @Transactional
    public String createUser(@RequestParam String role,
                             @ModelAttribute PersonnelFile file,
                             @RequestParam(required = false) Integer l3OrgId,
                             @RequestParam(required = false) Integer positionId,
                             Model model) {

        User user = new User();
        user.setUsername("temp_" + System.currentTimeMillis());
        user.setPasswordHash("123"); // Default password

        switch (role) {
            case "management":
                user.setPositionId(2);
                break;
            case "hr":
                user.setPositionId(3);
                user.setL3OrgId(l3OrgId);
                break;
            case "salary":
                user.setPositionId(4);
                user.setL3OrgId(l3OrgId);
                break;
            case "employee":
                user.setL3OrgId(l3OrgId);
                user.setPositionId(positionId);
                break;
        }

        userMapper.insert(user);
        int uid = user.getUserId();
        String dateStr = LocalDate.now().format(DATE_FMT);
        String account = dateStr + String.format("%04d", uid % 10000);
        user.setUsername(account);
        userMapper.updateById(user);

        file.setUserId(uid);
        file.setArchiveNo(account);
        file.setAuditStatus("Approved"); // Admin created files are auto-approved
        file.setHrSubmitterId(1); // Set a default submitter ID
        
        fileMapper.insert(file);

        model.addAttribute("success", "账号 " + account + " 创建成功，初始密码为 123");
        List<Organization> level3Orgs = organizationMapper.selectList(new QueryWrapper<Organization>().eq("level", 3));
        List<Map<String, Object>> orgsWithFullName = level3Orgs.stream()
                .map(org -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orgId", org.getOrgId());
                    map.put("fullName", organizationService.getFullOrgName(org.getOrgId()));
                    return map;
                })
                .collect(Collectors.toList());
        model.addAttribute("level3Orgs", orgsWithFullName);
        return "admin/user_form";
    }
}
