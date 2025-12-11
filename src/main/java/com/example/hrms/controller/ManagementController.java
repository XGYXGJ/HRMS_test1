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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
        // 1. 只查待审核的工资单，按登记时间倒序
        List<SalaryRegisterMaster> registers = registerMasterMapper.selectList(
                new QueryWrapper<SalaryRegisterMaster>()
                        .eq("audit_status", "Pending")
                        .orderByDesc("register_time")
        );

        // 2. 提交人 Map：Submitter_ID -> User
        List<Integer> submitterIds = registers.stream()
                .map(SalaryRegisterMaster::getSubmitterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> submitterMap = submitterIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectBatchIds(submitterIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        // 3. 所在机构 Map：L3_Org_ID -> Org_Name
        Set<Integer> orgIds = registers.stream()
                .map(SalaryRegisterMaster::getL3OrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> orgNameMap = new HashMap<>();
        if (!orgIds.isEmpty()) {
            // 你可以用 organizationService.getFullOrgName，也可以直接查表取 Org_Name
            List<Organization> orgs = organizationMapper.selectBatchIds(orgIds);
            orgNameMap = orgs.stream()
                    .collect(Collectors.toMap(Organization::getOrgId, Organization::getOrgName));
        }

        model.addAttribute("registers", registers);
        model.addAttribute("submitterMap", submitterMap);
        model.addAttribute("orgNameMap", orgNameMap);

        return "manage/audit_list_register";
    }

    // 3b. 薪酬标准审核列表
    @GetMapping("/audit/standard/list")
    public String standardAuditList(Model model) {
        // 1. 只查待审核的标准，按提交时间倒序
        List<SalaryStandardMaster> standards = standardMasterMapper.selectList(
                new QueryWrapper<SalaryStandardMaster>()
                        .eq("audit_status", "Pending")
                        .orderByDesc("submission_time")
        );

        // 2. 职位 Map：Position_ID -> Position
        List<Integer> positionIds = standards.stream()
                .map(SalaryStandardMaster::getPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Position> positionMap = positionIds.isEmpty()
                ? new HashMap<>()
                : positionMapper.selectBatchIds(positionIds).stream()
                .collect(Collectors.toMap(Position::getPositionId, p -> p));

        // 3. 提交人 Map：Submitter_ID -> User
        List<Integer> submitterIds = standards.stream()
                .map(SalaryStandardMaster::getSubmitterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> userMap = submitterIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectBatchIds(submitterIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        // 4. 所在机构 Map：L3_Org_ID -> Org_Name
        Set<Integer> orgIds = standards.stream()
                .map(SalaryStandardMaster::getL3OrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> orgNameMap = new HashMap<>();
        if (!orgIds.isEmpty()) {
            List<Organization> orgs = organizationMapper.selectBatchIds(orgIds);
            orgNameMap = orgs.stream()
                    .collect(Collectors.toMap(Organization::getOrgId, Organization::getOrgName));
        }

        model.addAttribute("standards", standards);
        model.addAttribute("positionMap", positionMap);
        model.addAttribute("userMap", userMap);
        model.addAttribute("orgNameMap", orgNameMap);

        return "manage/audit_list_standard";
    }

    // 4. 审核详情（仅薪酬相关）
    @GetMapping("/audit/detail/{id}")
    public String auditDetail(@PathVariable Integer id,
                              @RequestParam String type,
                              Model model) {
        if ("Register".equalsIgnoreCase(type)) {
            // 工资单主表
            SalaryRegisterMaster master = registerMasterMapper.selectById(id);
            if (master == null) {
                return "redirect:/manage/audit/register/list";
            }

            // 提交人
            User submitter = master.getSubmitterId() == null
                    ? null
                    : userMapper.selectById(master.getSubmitterId());

            // 使用跟薪酬经理端相同的明细结构（finalDetails）
            List<SalaryItem> allItems = itemMapper.selectList(null);
            List<Map<String, Object>> finalDetails = buildFinalDetails(id, allItems);

            model.addAttribute("master", master);
            model.addAttribute("finalDetails", finalDetails);
            model.addAttribute("submitter", submitter);
            return "manage/audit_detail_register";
        }

        // 下面保留你原来的“标准工资单审核详情”逻辑，不动
        SalaryStandardMaster master = standardMasterMapper.selectById(id);
        if (master == null) return "redirect:/manage/audit/standard/list";
        List<SalaryStandardDetail> details = standardDetailMapper.selectList(
                new QueryWrapper<SalaryStandardDetail>().eq("standard_id", id));
        Position position = positionMapper.selectById(master.getPositionId());
        User submitter = master.getSubmitterId() == null ? null : userMapper.selectById(master.getSubmitterId());
        Map<Integer, String> itemMap = itemMapper.selectList(null).stream()
                .collect(Collectors.toMap(SalaryItem::getItemId, SalaryItem::getItemName));
        model.addAttribute("master", master);
        model.addAttribute("details", details);
        model.addAttribute("position", position);
        model.addAttribute("itemMap", itemMap);
        model.addAttribute("submitter", submitter);
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

    /**
     * 构建工资单明细展示数据（管理员审核页复用薪酬经理视图结构）
     * - username 来自 T_User.Username
     * - userName 来自 T_Personnel_File.Name
     * - positionName 来自 T_Position.Position_Name
     * - 各种金额直接来自 T_Salary_Register_Detail
     */
    private List<Map<String, Object>> buildFinalDetails(Integer registerId, List<SalaryItem> allItems) {
        // 1. 获取发放详情
        List<SalaryRegisterDetail> details = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>().eq("Register_ID", registerId)
        );
        if (details == null || details.isEmpty()) {
            return List.of();
        }

        // 2. 准备所有相关的 userId
        List<Integer> userIds = details.stream()
                .map(SalaryRegisterDetail::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, User> userMap = Collections.emptyMap();
        Map<Integer, Position> positionMap = Collections.emptyMap();
        Map<Integer, PersonnelFile> personnelMap = Collections.emptyMap();

        if (!userIds.isEmpty()) {
            // 2-1 用户（拿账号、职位ID）
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));

            // 2-2 职位
            Set<Integer> positionIds = users.stream()
                    .map(User::getPositionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!positionIds.isEmpty()) {
                List<Position> positions = positionMapper.selectBatchIds(positionIds);
                positionMap = positions.stream()
                        .collect(Collectors.toMap(Position::getPositionId, p -> p));
            }

            // 2-3 人员档案（姓名）
            List<PersonnelFile> pFiles = personnelFileMapper.selectList(
                    new QueryWrapper<PersonnelFile>().in("User_ID", userIds)
            );
            personnelMap = pFiles.stream()
                    .collect(Collectors.toMap(PersonnelFile::getUserId, pf -> pf));
        }

        Map<Integer, User> finalUserMap = userMap;
        Map<Integer, Position> finalPositionMap = positionMap;
        Map<Integer, PersonnelFile> finalPersonnelMap = personnelMap;

        // 3. 组装返回给前端的 List<Map>
        return details.stream().map(detail -> {
            Map<String, Object> empDetail = new HashMap<>();

            User user = finalUserMap.get(detail.getUserId());
            Position position = (user != null && user.getPositionId() != null)
                    ? finalPositionMap.get(user.getPositionId())
                    : null;
            PersonnelFile pf = finalPersonnelMap.get(detail.getUserId());

            // 账号 / 姓名 / 职位
            empDetail.put("username", user != null ? user.getUsername() : "未知账号");
            empDetail.put("userName",
                    pf != null ? pf.getName()
                            : (user != null ? user.getUsername() : "未知用户"));
            empDetail.put("positionName",
                    position != null ? position.getPositionName() : "-");

            // 金额字段
            empDetail.put("detailId", detail.getDetailId());
            empDetail.put("attendanceCount", detail.getAttendanceCount());
            empDetail.put("kpiUnits", detail.getKpiUnits());
            empDetail.put("attendanceAdjustment", detail.getAttendanceAdjustment());
            empDetail.put("kpiBonus", detail.getKpiBonus());
            empDetail.put("baseSalary", detail.getBaseSalary());
            empDetail.put("totalSubsidy", detail.getTotalSubsidy());
            empDetail.put("insuranceFee", detail.getInsuranceFee());
            empDetail.put("grossMoney", detail.getGrossMoney());

            // 动态项目列（审核模板暂时不用，但保持结构一致）
            Map<Integer, BigDecimal> itemValues = new HashMap<>();
            if (allItems != null) {
                for (SalaryItem item : allItems) {
                    if ("基本工资".equals(item.getItemName())) {
                        itemValues.put(item.getItemId(), detail.getBaseSalary());
                    } else if ("全勤奖".equals(item.getItemName())) {
                        itemValues.put(item.getItemId(), detail.getAttendanceAdjustment());
                    } else {
                        itemValues.put(item.getItemId(), BigDecimal.ZERO);
                    }
                }
            }
            empDetail.put("itemValues", itemValues);

            return empDetail;
        }).collect(Collectors.toList());
    }
}
