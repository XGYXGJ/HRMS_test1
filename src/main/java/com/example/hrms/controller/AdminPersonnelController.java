// src/main/java/com/example/hrms/controller/AdminPersonnelController.java
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin/personnel") // 新的URL前缀
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

    // 1. 档案列表 (管理员视角：查看全部)
    @GetMapping
    public String listAllFiles(@RequestParam(value = "q", required = false) String q, Model model) {
        // 管理员的 l3OrgId 为 null，可以查询全部
        model.addAttribute("files", personnelService.listFiles(null, q));
        model.addAttribute("q", q);
        return "admin/personnel_list"; // 指向新的 admin 视图
    }

    // 2. 新建档案页面 (管理员视角：分级选择机构)
    @GetMapping("/new")
    public String newFilePage(Model model) {
        // 提供一级机构列表用于联动选择
        model.addAttribute("level1Orgs", orgService.getLevel1Orgs());
        return "admin/personnel_form"; // 指向新的 admin 视图
    }

    // 3. 处理新建档案提交 (管理员视角)
    @PostMapping("/create")
    public String createFile(@ModelAttribute PersonnelFile file,
                             @RequestParam Integer positionId,
                             Model model) {
        try {
            personnelService.createPersonnelAuto(file, positionId);
        } catch (Exception e) {
            model.addAttribute("error", "创建失败: " + e.getMessage());
            model.addAttribute("level1Orgs", orgService.getLevel1Orgs());
            return "admin/personnel_form";
        }
        return "redirect:/admin/personnel?success_create";
    }

    // 4. 查看档案详情 (管理员无限制)
    @GetMapping("/{id}")
    public String viewFile(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        if (file == null) {
            return "redirect:/admin/personnel?error_notfound";
        }

        String orgName = organizationService.getFullOrgName(file.getL3OrgId());
        User user = userMapper.selectById(file.getUserId());
        Position position = null;
        if (user != null) {
            position = positionMapper.selectById(user.getPositionId());
        }

        model.addAttribute("file", file);
        model.addAttribute("orgName", orgName);
        model.addAttribute("positionName", position != null ? position.getPositionName() : "-");
        return "admin/personnel_view";
    }

    // 5. 编辑档案页面 (管理员视角)
    @GetMapping("/edit/{id}")
    public String editFilePage(@PathVariable Integer id, Model model) {
        Map<String, Object> fileData = personnelService.getFileById(id);
        if (fileData == null) {
            return "redirect:/admin/personnel?error_notfound";
        }

        Integer l3OrgId = fileData.get("L3_Org_ID") != null ? ((Number) fileData.get("L3_Org_ID")).intValue() : null;
        if (l3OrgId != null) {
            model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", l3OrgId)));
        }

        PersonnelFile file = personnelFileMapper.selectById(id);
        if (file != null && file.getUserId() != null) {
            User user = userMapper.selectById(file.getUserId());
            if (user != null) {
                model.addAttribute("currentPositionId", user.getPositionId());
            }
        }

        model.addAttribute("file", fileData);
        return "admin/personnel_edit";
    }

    // 6. 处理编辑档案提交 (管理员视角)
    @PostMapping("/update/{id}")
    public String updateFile(@PathVariable Integer id,
                             @RequestParam Map<String, String> allParams,
                             @RequestParam(required = false) Integer positionId) {
        // 更新档案基本信息
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("name", allParams.get("name"));
        payload.put("gender", allParams.get("gender"));
        payload.put("idNumber", allParams.get("idNumber"));
        payload.put("phoneNumber", allParams.get("phoneNumber"));
        payload.put("address", allParams.get("address"));
        personnelService.updatePersonnel(id, payload);

        // 更新职位
        PersonnelFile pf = personnelFileMapper.selectById(id);
        if (pf != null && pf.getUserId() != null && positionId != null) {
            User user = userMapper.selectById(pf.getUserId());
            if (user != null) {
                user.setPositionId(positionId);
                userMapper.updateById(user);
            }
        }
        return "redirect:/admin/personnel/" + id;
    }
}
