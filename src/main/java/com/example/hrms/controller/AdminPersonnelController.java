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

    // 1. 档案列表 (管理员视角：查看全部，支持模糊查找和按机构筛选)
    @GetMapping
    public String listAllFiles(@RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "orgId", required = false) Integer orgId,
                               Model model) {
        model.addAttribute("files", personnelService.listFiles(orgId, q));
        model.addAttribute("q", q);
        return "admin/file_list"; // 使用 admin/file_list.html
    }

    // 2. 已删除档案列表
    @GetMapping("/deleted")
    public String listDeletedFiles(Model model) {
        model.addAttribute("files", personnelService.listDeletedFiles());
        return "admin/deleted_list";
    }

    // 3. 恢复已删除的档案
    @GetMapping("/restore/{id}")
    public String restoreFile(@PathVariable Integer id) {
        personnelService.restoreFile(id);
        return "redirect:/admin/personnel/deleted";
    }

    // 4. 新建档案页面 (管理员视角：分级选择机构)
    @GetMapping("/new")
    public String newFilePage(Model model) {
        // 提供一级机构列表用于联动选择
        model.addAttribute("level1Orgs", orgService.getLevel1Orgs());
        return "admin/personnel_form"; // 指向新的 admin 视图
    }

    // 5. 处理新建档案提交 (管理员视角)
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

    // 6. 查看档案详情 (管理员无限制)
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

    // 7. 编辑档案页面 (管理员视角)
    @GetMapping("/edit/{id}")
    public String editFilePage(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        model.addAttribute("file", file);
        return "admin/file_edit"; // 使用 admin/file_edit.html
    }

    // 8. 处理编辑档案提交 (管理员视角)
    @PostMapping("/edit/{id}")
    public String updateFile(@PathVariable Integer id, @ModelAttribute PersonnelFile file) {
        file.setFileId(id);
        personnelFileMapper.updateById(file);
        return "redirect:/admin/personnel";
    }
    
    // 9. 删除档案
    @GetMapping("/delete/{id}")
    public String deleteFile(@PathVariable Integer id) {
        personnelService.deleteFile(id, "Deleted by admin");
        return "redirect:/admin/personnel";
    }
}
