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
import jakarta.servlet.http.HttpServletResponse;
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
        model.addAttribute("orgId", orgId);
        return "admin/personnel_list";
    }

    @GetMapping("/deleted-list")
    public String listDeletedFiles(Model model) {
        model.addAttribute("files", personnelService.listDeletedFiles());
        return "admin/deleted_list";
    }

    @GetMapping("/restore/{id}")
    public String restoreFile(@PathVariable Integer id, Model model, HttpServletResponse response) {
        personnelService.restoreFile(id);
        model.addAttribute("success", "档案恢复成功！");
        response.addHeader("X-Location", "/admin/deleted-list");
        return listDeletedFiles(model);
    }

    @GetMapping("/personnel/new")
    public String newFilePage(Model model) {
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
        model.addAttribute("file", new PersonnelFile());
        return "admin/personnel_form";
    }

    @PostMapping("/personnel/new")
    @Transactional
    public String createFile(@RequestParam String role,
                             @ModelAttribute PersonnelFile file,
                             @RequestParam(required = false) Integer l3OrgId,
                             Model model, HttpServletResponse response) {

        // Validation
        if (file.getIdNumber() != null && !file.getIdNumber().isEmpty() && file.getIdNumber().length() != 18) {
            model.addAttribute("error", "身份证号必须为18位");
            return newFilePage(model); // Reload form with error
        }
        if (file.getPhoneNumber() != null && !file.getPhoneNumber().isEmpty() && file.getPhoneNumber().length() != 11) {
            model.addAttribute("error", "手机号必须为11位");
            return newFilePage(model); // Reload form with error
        }

        User user = new User();
        user.setUsername("temp_" + System.currentTimeMillis());
        user.setPasswordHash("123");

        switch (role) {
            case "management":
                user.setPositionId(2);
                user.setL3OrgId(1);
                break;
            case "hr":
                user.setPositionId(3);
                user.setL3OrgId(l3OrgId);
                break;
            case "salary":
                user.setPositionId(4);
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
        file.setL3OrgId(user.getL3OrgId());
        file.setArchiveNo(account);
        file.setAuditStatus("Approved");
        file.setHrSubmitterId(1);

        personnelFileMapper.insert(file);

        model.addAttribute("success", "账号 " + account + " 创建成功，初始密码为 123");
        response.addHeader("X-Location", "/admin/personnel/list");
        return listAllFiles(null, null, model);
    }


    @GetMapping("/personnel/view/{id}")
    public String viewFile(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        if (file == null) {
            model.addAttribute("error", "档案未找到");
            return listAllFiles(null, null, model);
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
            model.addAttribute("positions", positionMapper.selectList(null));
        }

        return "admin/personnel_edit";
    }

    @PostMapping("/personnel/edit/{id}")
    public String updateFile(@PathVariable Integer id, @ModelAttribute PersonnelFile file, @RequestParam(required = false) Integer positionId, @RequestParam(required = false) String newPassword, Model model, HttpServletResponse response) {
        // 1. Save the data
        file.setFileId(id);
        personnelFileMapper.updateById(file);

        PersonnelFile pf = personnelFileMapper.selectById(id);
        if (pf != null && pf.getUserId() != null) {
            User empUser = userMapper.selectById(pf.getUserId());
            if (empUser != null) {
                if (positionId != null) {
                    empUser.setPositionId(positionId);
                }
                if (newPassword != null && !newPassword.isEmpty()) {
                    // In a real application, you should hash the password
                    empUser.setPasswordHash(newPassword);
                }
                userMapper.updateById(empUser);
            }
        }

        // 2. Add success message to the model
        model.addAttribute("success", "档案更新成功！");

        // 3. Reload the data for the edit page and return the same view
        return editFilePage(id, model);
    }
    
    @GetMapping("/personnel/delete/{id}")
    public String deleteFile(@PathVariable Integer id, Model model, HttpServletResponse response) {
        personnelService.deleteFile(id, "Deleted by admin");
        model.addAttribute("success", "档案删除成功！");
        response.addHeader("X-Location", "/admin/personnel/list");
        return listAllFiles(null, null, model);
    }
}