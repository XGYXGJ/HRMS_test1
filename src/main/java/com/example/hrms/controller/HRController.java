// src/main/java/com/example/hrms/controller/HRController.java
package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
import com.example.hrms.mapper.UserMapper;
import com.example.hrms.service.PersonnelService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/hr") // 路径保持不变，供人事经理使用
public class HRController {

    @Autowired
    private PersonnelService personnelService;
    @Autowired
    private PositionMapper positionMapper;
    @Autowired
    private PersonnelFileMapper personnelFileMapper;
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "hr/dashboard";
    }

    // 1. 档案列表 (人事经理视角：仅看本部门)
    @GetMapping("/files")
    public String personnelListPage(@RequestParam(value = "q", required = false) String q,
                                    HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        // 强制使用当前用户的 l3OrgId 进行过滤
        model.addAttribute("files", personnelService.listFiles(user.getL3OrgId(), q));
        model.addAttribute("q", q);
        return "hr/personnel_list";
    }

    // 2. 新建档案页面 (人事经理视角)
    @GetMapping("/files/new")
    public String personnelNewPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        // 预加载本部门的职位列表
        model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", user.getL3OrgId())));
        return "hr/personnel_form";
    }

    // 3. 处理新建档案提交 (人事经理视角)
    @PostMapping("/files/new")
    public String createPersonnel(@ModelAttribute PersonnelFile file,
                                  @RequestParam Integer positionId,
                                  HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        // 强制设置新员工的机构为当前人事经理所在的机构
        file.setL3OrgId(currentUser.getL3OrgId());
        file.setAuditStatus("Pending"); // 设置审核状态为待审核
        file.setHrSubmitterId(currentUser.getUserId()); // 记录提交人ID

        try {
            personnelService.createPersonnelAuto(file, positionId);
        } catch (Exception ex) {
            model.addAttribute("error", "创建失败: " + ex.getMessage());
            // 重新加载职位列表
            model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", currentUser.getL3OrgId())));
            return "hr/personnel_form";
        }
        return "redirect:/hr/files?success_create";
    }

    // 4. 查看档案详情 (人事经理视角：带权限检查)
    @GetMapping("/files/{id}")
    public String personnelViewPage(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        Map<String, Object> file = personnelService.getFileById(id);
        if (file == null) {
            return "redirect:/hr/files?error_notfound";
        }
        // 权限检查
        Integer fileL3 = file.get("L3_Org_ID") != null ? ((Number) file.get("L3_Org_ID")).intValue() : null;
        if (fileL3 == null || !fileL3.equals(user.getL3OrgId())) {
            return "redirect:/hr/files?error_access_denied";
        }
        model.addAttribute("file", file);
        return "hr/personnel_view";
    }

    // 5. 编辑档案页面 (人事经理视角)
    @GetMapping("/files/edit/{id}")
    public String personnelEditPage(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        Map<String, Object> fileData = personnelService.getFileById(id);
        if (fileData == null) {
            return "redirect:/hr/files?error_notfound";
        }
        // 权限检查
        Integer fileL3 = fileData.get("L3_Org_ID") != null ? ((Number) fileData.get("L3_Org_ID")).intValue() : null;
        if (fileL3 == null || !fileL3.equals(user.getL3OrgId())) {
            return "redirect:/hr/files?error_access_denied";
        }

        // 加载本部门职位
        model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", user.getL3OrgId())));

        // 获取员工当前职位ID
        PersonnelFile file = personnelFileMapper.selectById(id);
        if (file != null && file.getUserId() != null) {
            User empUser = userMapper.selectById(file.getUserId());
            model.addAttribute("currentPositionId", empUser.getPositionId());
        }

        model.addAttribute("file", fileData);
        return "hr/personnel_edit";
    }

    // 6. 处理编辑档案提交 (人事经理视角)
    @PostMapping("/files/edit/{id}")
    public String updatePersonnel(@PathVariable Integer id,
                                  @RequestParam Map<String, String> allParams,
                                  @RequestParam(required = false) Integer positionId,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        Map<String, Object> fileData = personnelService.getFileById(id);
        // 再次权限检查
        Integer fileL3 = fileData.get("L3_Org_ID") != null ? ((Number) fileData.get("L3_Org_ID")).intValue() : null;
        if (fileL3 == null || !fileL3.equals(user.getL3OrgId())) {
            return "redirect:/hr/files?error_access_denied";
        }

        // 更新档案基本信息
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", allParams.get("name"));
        payload.put("gender", allParams.get("gender"));
        payload.put("idNumber", allParams.get("idNumber"));
        payload.put("phoneNumber", allParams.get("phoneNumber"));
        payload.put("address", allParams.get("address"));
        personnelService.updatePersonnel(id, payload);

        // 更新职位
        PersonnelFile pf = personnelFileMapper.selectById(id);
        if (pf != null && pf.getUserId() != null && positionId != null) {
            User empUser = userMapper.selectById(pf.getUserId());
            if (empUser != null) {
                empUser.setPositionId(positionId);
                userMapper.updateById(empUser);
            }
        }
        return "redirect:/hr/files/" + id;
    }

    // 7. 重新提交审核
    @PostMapping("/files/resubmit")
    public String resubmitPersonnel(@RequestParam Integer id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        PersonnelFile file = personnelFileMapper.selectById(id);

        // 权限检查：确保该档案属于当前人事经理的机构
        if (file != null && file.getL3OrgId().equals(user.getL3OrgId())) {
            file.setAuditStatus("Pending");
            personnelFileMapper.updateById(file);
        }

        return "redirect:/hr/files";
    }
}
