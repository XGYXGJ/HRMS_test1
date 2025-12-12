package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
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
        model.addAttribute("level1Orgs", orgService.getLevel1Orgs());
        return "admin/personnel_form";
    }

    @PostMapping("/personnel/new")
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
        return "redirect:/admin/personnel/list?success_create";
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
        if (user != null) {
            position = positionMapper.selectById(user.getPositionId());
        }

        model.addAttribute("file", file);
        model.addAttribute("orgName", orgName);
        model.addAttribute("positionName", position != null ? position.getPositionName() : "-");
        return "admin/personnel_view";
    }

    @GetMapping("/personnel/edit/{id}")
    public String editFilePage(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        model.addAttribute("file", file);

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("User_ID", file.getUserId()));
        if (user != null) {
            model.addAttribute("currentPositionId", user.getPositionId());
            model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", user.getL3OrgId())));
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