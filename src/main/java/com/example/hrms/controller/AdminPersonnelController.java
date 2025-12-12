package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.Organization;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.OrganizationMapper;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
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
public class AdminPersonnelController {

    @Autowired
    private PersonnelService personnelService;
    @Autowired
    private OrgService orgService;
    @Autowired
    private PositionMapper positionMapper;
    @Autowired
    private PersonnelFileMapper personnelFileMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationMapper organizationMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @GetMapping("/personnel/list")
    public String listAllFiles(@RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "orgId", required = false) Integer orgId,
                               Model model) {
        model.addAttribute("files", personnelService.listFiles(orgId, q));
        model.addAttribute("q", q);
        return "admin/personnel_list";
    }

    @GetMapping("/deleted-list")
    public String listDeletedFiles(Model model) {
        model.addAttribute("files", personnelService.listDeletedFiles());
        return "admin/deleted_list";
    }

    @GetMapping("/restore/{id}")
    public String restoreFile(@PathVariable Integer id, Model model) {
        personnelService.restoreFile(id);
        model.addAttribute("success", "档案恢复成功！");
        return listDeletedFiles(model); // Return the fragment
    }

    @GetMapping("/personnel/new")
    public String newFilePage(Model model) {
        // This is the same logic as in AdminController's newUserForm
        List<Organization> level3Orgs = organizationMapper.selectList(new QueryWrapper<Organization>().eq("level", 3));
        List<Map<String, Object>> orgsWithFullName = level3Orgs.stream()
                .map(o -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orgId", o.getOrgId());
                    map.put("fullName", organizationService.getFullOrgName(o.getOrgId()));
                    return map;
                })
                .collect(Collectors.toList());
        model.addAttribute("level3Orgs", orgsWithFullName);
        model.addAttribute("file", new PersonnelFile()); // Add an empty file object
        return "admin/personnel_form";
    }

    @PostMapping("/personnel/new")
    @Transactional
    public String createFile(@RequestParam String role,
                             @ModelAttribute PersonnelFile file,
                             @RequestParam(required = false) Integer l3OrgId,
                             Model model) {

        // This logic is adapted from AdminController's createUser method
        User user = new User();
        user.setUsername("temp_" + System.currentTimeMillis());
        user.setPasswordHash("123"); // Default password

        switch (role) {
            case "management":
                user.setPositionId(2); // 2 is "管理部门"
                user.setL3OrgId(1);     // Belongs to top-level org
                break;
            case "hr":
                user.setPositionId(3); // 3 is "人事经理"
                user.setL3OrgId(l3OrgId);
                break;
            case "salary":
                user.setPositionId(4); // 4 is "薪酬经理"
                user.setL3OrgId(l3OrgId);
                break;
        }

        userMapper.insert(user);
        int uid = user.getUserId();
        String dateStr = LocalDate.now().format(DATE_FMT);
        String account = dateStr + String.format("%04d", uid % 10000);
        user.setUsername(account);
        userMapper.updateById(user);

        file.setUserId(uid);
        file.setL3OrgId(user.getL3OrgId()); // Ensure file's orgId matches user's
        file.setArchiveNo(account);
        file.setAuditStatus("Approved");
        file.setHrSubmitterId(1); // Admin's ID

        personnelFileMapper.insert(file);

        // Instead of redirecting, forward to the list view with a success message
        model.addAttribute("success", "账号 " + account + " 创建成功，初始密码为 123");
        return listAllFiles(null, null, model);
    }


    @GetMapping("/personnel/view/{id}")
    public String viewFile(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        if (file == null) {
            return "redirect:/admin/personnel/list?error_notfound";
        }

        String orgName = organizationService.getFullOrgName(file.getL3OrgId());
        User user = userMapper.selectById(file.getUserId());
        Position position = null;
        if (user != null && user.getPositionId() != null) {
            position = positionMapper.selectById(user.getPositionId());
        }

        model.addAttribute("file", file);
        model.addAttribute("orgName", orgName);
        model.addAttribute("positionName", position != null ? position.getPositionName() : "-");
        model.addAttribute("account", user != null ? user.getUsername() : "N/A");
        return "admin/personnel_view";
    }


    @GetMapping("/personnel/edit/{id}")
    public String editFilePage(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        model.addAttribute("file", file);

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("User_ID", file.getUserId()));
        if (user != null) {
            model.addAttribute("currentPositionId", user.getPositionId());
            // Provide all positions for potential changes, maybe grouped by org later
            model.addAttribute("positions", positionMapper.selectList(null));
        }

        return "admin/personnel_edit";
    }

    @PostMapping("/personnel/edit/{id}")
    public String updateFile(@PathVariable Integer id, @ModelAttribute PersonnelFile file, @RequestParam(required = false) Integer positionId) {
        file.setFileId(id);
        personnelFileMapper.updateById(file);

        PersonnelFile pf = personnelFileMapper.selectById(id);
        if (pf != null && pf.getUserId() != null && positionId != null) {
            User empUser = userMapper.selectById(pf.getUserId());
            if (empUser != null) {
                empUser.setPositionId(positionId);
                userMapper.updateById(empUser);
            }
        }

        return "redirect:/admin/personnel/list";
    }
    
    @GetMapping("/personnel/delete/{id}")
    public String deleteFile(@PathVariable Integer id, Model model) {
        personnelService.deleteFile(id, "Deleted by admin");
        model.addAttribute("success", "档案删除成功！");
        return listAllFiles(null, null, model); // Return the fragment
    }
}