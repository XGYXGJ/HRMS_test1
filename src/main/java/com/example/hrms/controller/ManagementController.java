package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import com.example.hrms.service.OrganizationService;
import com.example.hrms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manage")
public class ManagementController {

    @Autowired private SalaryRegisterMasterMapper registerMasterMapper;
    @Autowired private SalaryRegisterDetailMapper registerDetailMapper;
    @Autowired private SalaryStandardMasterMapper standardMasterMapper;
    @Autowired private SalaryStandardDetailMapper standardDetailMapper;
    @Autowired private PositionMapper positionMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private SalaryItemMapper itemMapper;
    @Autowired private PersonnelFileMapper personnelFileMapper;
    @Autowired private OrganizationMapper organizationMapper;
    @Autowired private OrganizationService organizationService;
    @Autowired private UserService userService;

    // 1. 主框架
    @GetMapping("/dashboard")
    public String dashboard() {
        return "manage/dashboard";
    }

    // 2. 工作台首页（片段）
    @GetMapping("/home")
    public String home(Model model) {
        long pendingCount = registerMasterMapper.selectCount(new QueryWrapper<SalaryRegisterMaster>().eq("audit_status", "Pending"));
        model.addAttribute("pendingCount", pendingCount);
        long pendingStandardCount = standardMasterMapper.selectCount(new QueryWrapper<SalaryStandardMaster>().eq("audit_status", "Pending"));
        model.addAttribute("pendingStandardCount", pendingStandardCount);
        long pendingPersonnelCount = personnelFileMapper.selectCount(new QueryWrapper<PersonnelFile>().eq("audit_status", "Pending"));
        model.addAttribute("pendingPersonnelCount", pendingPersonnelCount);
        return "manage/dashboard_home";
    }

    // 3a. 工资单审核列表
    @GetMapping("/audit/register/list")
    public String registerAuditList(Model model) {
        List<SalaryRegisterMaster> registers = registerMasterMapper.selectList(new QueryWrapper<SalaryRegisterMaster>().eq("audit_status", "Pending").orderByDesc("register_time"));
        List<Integer> submitterIds = registers.stream().map(SalaryRegisterMaster::getSubmitterId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Integer, User> submitterMap = submitterIds.isEmpty() ? new HashMap<>() : userMapper.selectBatchIds(submitterIds).stream().collect(Collectors.toMap(User::getUserId, u -> u));
        model.addAttribute("registers", registers);
        model.addAttribute("submitterMap", submitterMap);
        return "manage/audit_list_register";
    }

    // 3b. 薪酬标准审核列表
    @GetMapping("/audit/standard/list")
    public String standardAuditList(Model model) {
        List<SalaryStandardMaster> standards = standardMasterMapper.selectList(new QueryWrapper<SalaryStandardMaster>().eq("audit_status", "Pending").orderByDesc("submission_time"));
        List<Integer> positionIds = standards.stream().map(SalaryStandardMaster::getPositionId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Integer, Position> positionMap = positionIds.isEmpty() ? new HashMap<>() : positionMapper.selectBatchIds(positionIds).stream().collect(Collectors.toMap(Position::getPositionId, p -> p));
        List<Integer> submitterIds = standards.stream().map(SalaryStandardMaster::getSubmitterId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Integer, User> userMap = submitterIds.isEmpty() ? new HashMap<>() : userMapper.selectBatchIds(submitterIds).stream().collect(Collectors.toMap(User::getUserId, u -> u));
        model.addAttribute("standards", standards);
        model.addAttribute("positionMap", positionMap);
        model.addAttribute("userMap", userMap);
        return "manage/audit_list_standard";
    }

    // 4. 审核详情（仅薪酬相关）
    @GetMapping("/audit/detail/{id}")
    public String auditDetail(@PathVariable Integer id, @RequestParam String type, Model model) {
        if ("Register".equalsIgnoreCase(type)) {
            SalaryRegisterMaster master = registerMasterMapper.selectById(id);
            if (master == null) return "redirect:/manage/audit/register/list";
            List<SalaryRegisterDetail> details = registerDetailMapper.selectList(new QueryWrapper<SalaryRegisterDetail>().eq("register_id", id));
            Map<Integer, String> itemMap = itemMapper.selectList(null).stream().collect(Collectors.toMap(SalaryItem::getItemId, SalaryItem::getItemName));
            User submitter = master.getSubmitterId() == null ? null : userMapper.selectById(master.getSubmitterId());
            model.addAttribute("master", master);
            model.addAttribute("details", details);
            model.addAttribute("itemMap", itemMap);
            model.addAttribute("submitter", submitter);
            return "manage/audit_detail_register";
        }
        // NOTE: The "Personnel" detail view is handled by ManagementAuditController
        SalaryStandardMaster master = standardMasterMapper.selectById(id);
        if (master == null) return "redirect:/manage/audit/standard/list";
        List<SalaryStandardDetail> details = standardDetailMapper.selectList(new QueryWrapper<SalaryStandardDetail>().eq("standard_id", id));
        Position position = positionMapper.selectById(master.getPositionId());
        User submitter = userMapper.selectById(master.getSubmitterId());
        Map<Integer, String> itemMap = itemMapper.selectList(null).stream().collect(Collectors.toMap(SalaryItem::getItemId, SalaryItem::getItemName));
        model.addAttribute("master", master);
        model.addAttribute("details", details);
        model.addAttribute("position", position);
        model.addAttribute("submitter", submitter);
        model.addAttribute("itemMap", itemMap);
        return "manage/audit_detail_standard";
    }

    // 5. 薪酬审核动作
    @PostMapping("/audit/process")
    public String processAudit(@RequestParam String type, @RequestParam Integer id, @RequestParam String action, HttpSession session) {
        // NOTE: The "/audit/process/personnel" is handled by ManagementAuditController
        User auditor = (User) session.getAttribute("user");
        if (auditor == null) return "redirect:/login";
        boolean pass = "pass".equalsIgnoreCase(action);
        if ("Register".equalsIgnoreCase(type)) {
            SalaryRegisterMaster master = registerMasterMapper.selectById(id);
            if (master != null) {
                master.setAuditStatus(pass ? "Approved" : "Rejected");
                master.setAuditorId(auditor.getUserId());
                master.setAuditTime(LocalDateTime.now());
                registerMasterMapper.updateById(master);
            }
            return "redirect:/manage/dashboard";
        }
        SalaryStandardMaster master = standardMasterMapper.selectById(id);
        if (master != null) {
            master.setAuditStatus(pass ? "Approved" : "Rejected");
            master.setAuditorId(auditor.getUserId());
            master.setAuditTime(LocalDateTime.now());
            standardMasterMapper.updateById(master);
        }
        return "redirect:/manage/dashboard";
    }

    // 6. 人员岗位调整页面
    @GetMapping("/transfer_employee")
    public String transferEmployee(Model model) {
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
        return "manage/transfer_employee";
    }

    // 7. 处理人员岗位调整 (AJAX)
    @PostMapping("/employee/transfer")
    @ResponseBody
    public Map<String, Object> handleTransferEmployee(@RequestParam Integer employeeId,
                                                      @RequestParam Integer targetOrgId,
                                                      @RequestParam Integer targetPositionId) {
        Map<String, Object> response = new HashMap<>();
        boolean success = userService.transferEmployee(employeeId, targetOrgId, targetPositionId);
        if (success) {
            response.put("success", true);
            response.put("message", "员工岗位调整成功！");
        } else {
            response.put("success", false);
            response.put("message", "员工岗位调整失败，请重试。");
        }
        return response;
    }
}
