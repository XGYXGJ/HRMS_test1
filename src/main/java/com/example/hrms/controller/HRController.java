package com.example.hrms.controller;

import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.User;
import com.example.hrms.service.PersonnelService;
import com.example.hrms.service.SalaryService;
import com.example.hrms.mapper.SalaryItemMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/hr")
public class HRController {

    @Autowired private SalaryService salaryService;
    @Autowired private SalaryItemMapper itemMapper;

    @Autowired private PersonnelService personnelService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        return "hr/dashboard";
    }

    // 服务器渲染：列出档案（可按 q 搜索），如果用户是 admin(positionId==1) 则返回全部
    @GetMapping("/files")
    public String personnelListPage(@RequestParam(value = "q", required = false) String q,
                                    HttpSession session, Model model) {
        try {
            User user = (User) session.getAttribute("user");
            Integer l3 = null;
            if (user != null && user.getPositionId() != null && user.getPositionId() != 1) {
                l3 = user.getL3OrgId();
            }

            model.addAttribute("q", q);
            model.addAttribute("files", personnelService.listFiles(l3, q));

            return "hr/personnel_list";
        } catch (Exception e) {
            model.addAttribute("error", "加载档案列表失败，请稍后重试");
            return "hr/personnel_list";
        }
    }

    // 新建档案页面（服务器渲染），在 model 中提供建议的档案号/账号（只读）
    @GetMapping("/files/new")
    public String personnelNewPage(HttpSession session, Model model) {
        long count = personnelService.countUsers();
        // 生成候选账号： yyyyMMdd + (count+1 padded 4)
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String candidate = dateStr + String.format("%04d", count + 1);
        model.addAttribute("generatedAccount", candidate);
        model.addAttribute("generatedArchiveNo", candidate);
        model.addAttribute("defaultPassword", "123");
        return "hr/personnel_form";
    }

    // 处理新建表单提交（服务器端），不依赖 AJAX
    @PostMapping("/files/new")
    public String createPersonnel(@RequestParam String name,
                                  @RequestParam(required=false) String gender,
                                  @RequestParam(required=false) String idNumber,
                                  @RequestParam(required=false) String phoneNumber,
                                  @RequestParam(required=false) String address,
                                  @RequestParam(required=false) Integer l3OrgId,
                                  Model model, HttpSession session) {
        PersonnelFile pf = new PersonnelFile();
        pf.setName(name);
        pf.setGender(gender);
        pf.setIdNumber(idNumber);
        pf.setPhoneNumber(phoneNumber);
        pf.setAddress(address);
        // l3OrgId 若未提供，取当前用户所属 L3
        User user = (User) session.getAttribute("user");
        if (l3OrgId == null && user != null) l3OrgId = user.getL3OrgId();
        pf.setL3OrgId(l3OrgId);

        // 服务端自动生成 account/archive/password
        try {
            java.util.Map<String,Object> created = personnelService.createPersonnelAuto(pf);
            // 将生成结果作为提示显示
            model.addAttribute("msg", "创建成功。账号: " + created.get("account") + " 密码: " + created.get("initPassword"));
        } catch (Exception ex) {
            model.addAttribute("error", "创建失败: " + ex.getMessage());
            return "hr/personnel_form";
        }
        return "redirect:/hr/files?msg=created";
    }

    // 查看档案（服务器渲染）
    @GetMapping("/files/{id}")
    public String personnelViewPage(@PathVariable Integer id, HttpSession session, Model model) {
        try {
            java.util.Map<String,Object> file = personnelService.getFileById(id);
            if (file == null) {
                model.addAttribute("error", "档案不存在");
                return "hr/personnel_list";
            }

            // 权限：非管理员只能查看本 L3
            User user = (User) session.getAttribute("user");
            if (user != null && user.getPositionId() != null && user.getPositionId() != 1) {
                Integer fileL3 = file.get("L3_Org_ID") != null ? ((Number)file.get("L3_Org_ID")).intValue() : null;
                if (fileL3 != null && !fileL3.equals(user.getL3OrgId())) {
                    model.addAttribute("error", "无权查看此档案");
                    return "hr/personnel_list";
                }
            }

            model.addAttribute("file", file);
            return "hr/personnel_view";
        } catch (Exception e) {
            model.addAttribute("error", "加载档案详情失败，请稍后重试");
            return "hr/personnel_list";
        }
    }

    // 编辑表单（服务器渲染）
    @GetMapping("/files/edit/{id}")
    public String personnelEditPage(@PathVariable Integer id, HttpSession session, Model model) {
        try {
            java.util.Map<String,Object> file = personnelService.getFileById(id);
            if (file == null) {
                model.addAttribute("error", "档案不存在");
                return "redirect:/hr/files";
            }

            // 权限同查看
            User user = (User) session.getAttribute("user");
            if (user != null && user.getPositionId() != null && user.getPositionId() != 1) {
                Integer fileL3 = file.get("L3_Org_ID") != null ? ((Number)file.get("L3_Org_ID")).intValue() : null;
                if (fileL3 != null && !fileL3.equals(user.getL3OrgId())) {
                    model.addAttribute("error", "无权编辑此档案");
                    return "redirect:/hr/files";
                }
            }

            model.addAttribute("file", file);
            return "hr/personnel_edit";
        } catch (Exception e) {
            model.addAttribute("error", "加载编辑页面失败，请稍后重试");
            return "hr/personnel_list";
        }
    }

    // 处理编辑表单提交（服务器端）
    @PostMapping("/files/edit/{id}")
    public String updatePersonnel(@PathVariable Integer id,
                                  @RequestParam String name,
                                  @RequestParam(required=false) String gender,
                                  @RequestParam(required=false) String idNumber,
                                  @RequestParam(required=false) String phoneNumber,
                                  @RequestParam(required=false) String address,
                                  HttpSession session, Model model) {
        try {
            java.util.Map<String,Object> file = personnelService.getFileById(id);
            if (file == null) {
                model.addAttribute("error", "档案不存在");
                return "redirect:/hr/files";
            }

            // 权限同前
            User user = (User) session.getAttribute("user");
            if (user != null && user.getPositionId() != null && user.getPositionId() != 1) {
                Integer fileL3 = file.get("L3_Org_ID") != null ? ((Number)file.get("L3_Org_ID")).intValue() : null;
                if (fileL3 != null && !fileL3.equals(user.getL3OrgId())) {
                    model.addAttribute("error", "无权编辑此档案");
                    return "redirect:/hr/files";
                }
            }

            java.util.Map<String,Object> payload = new java.util.HashMap<>();
            payload.put("name", name);
            payload.put("gender", gender);
            payload.put("idNumber", idNumber);
            payload.put("phoneNumber", phoneNumber);
            payload.put("address", address);

            personnelService.updatePersonnel(id, payload);

            return "redirect:/hr/files/" + id;
        } catch (Exception ex) {
            model.addAttribute("error", "保存失败: " + ex.getMessage());
            return "hr/personnel_edit";
        }
    }
}
