package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.SalaryStandardDTO;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import com.example.hrms.service.SalaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/salary")
public class SalaryMgrController {

    @Autowired private UserMapper userMapper; // 注入 UserMapper 来获取员工信息
    @Autowired private SalaryService salaryService;
    @Autowired private SalaryItemMapper itemMapper;
    @Autowired private SalaryRegisterMasterMapper registerMasterMapper;
    @Autowired private SalaryRegisterDetailMapper registerDetailMapper;
    @Autowired private SalaryStandardMasterMapper standardMasterMapper; // 注入标准Mapper用于查重
    @Autowired private PositionMapper positionMapper; // 注入职位Mapper
    @Autowired private PersonnelFileMapper personnelFileMapper;

    // =========================================
    // 基础页面 (保持不变)
    // =========================================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        return "salary/dashboard";
    }

    @GetMapping("/dashboard/home")
    public String dashboardHome() {
        return "salary/dashboard_home";
    }

    // =========================================
    // 薪酬标准管理 (保持不变)
    // =========================================

    // 1. 制定薪酬标准页面
    @GetMapping("/standard/new")
    public String createStandardPage(HttpSession session, Model model) {
        // A. 获取当前用户
        User user = (User) session.getAttribute("user");
        model.addAttribute("currentUser", user);

        // B. 生成自动编号
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "FAT" + dateStr;

        // 查询数据库中当天已有的最大编号
        QueryWrapper<SalaryStandardMaster> query = new QueryWrapper<>();
        query.likeRight("Standard_Code", prefix);
        query.orderByDesc("Standard_Code");
        query.last("LIMIT 1");

        SalaryStandardMaster lastMaster = standardMasterMapper.selectOne(query);
        String newCode;
        if (lastMaster != null) {
            // 取最后两位数字 + 1
            String lastCode = lastMaster.getStandardCode();
            String numStr = lastCode.substring(lastCode.length() - 2);
            int num = Integer.parseInt(numStr) + 1;
            newCode = prefix + String.format("%02d", num);
        } else {
            newCode = prefix + "01";
        }
        model.addAttribute("generatedCode", newCode);

        // C. 当前时间
        model.addAttribute("currentTime", LocalDateTime.now());

        // D. 获取所有薪酬项目 (供复选框使用)
        model.addAttribute("items", itemMapper.selectList(null));

        // E. 获取当前机构下「适用职位」列表（关键修复）
        Integer l3OrgId = (user != null) ? user.getL3OrgId() : null;
        List<Position> positions = salaryService.getApplicablePositionsByOrg(l3OrgId);
        model.addAttribute("positions", positions);
        return "salary/standard_add";
    }

    // 2. 提交薪酬标准 (AJAX POST)
    @PostMapping("/standard/save")
    public String saveStandard(SalaryStandardDTO dto, HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");

        // 注意：这里需要确保 Service 层处理了 dto.getStandardCode() 并保存到数据库
        salaryService.submitStandard(dto, salary.getUserId(), salary.getL3OrgId());

        model.addAttribute("msg", "薪酬标准 " + dto.getStandardCode() + " 已提交，等待审核。");
        model.addAttribute("standards", salaryService.getStandardsByOrg(salary.getL3OrgId()));
        return "salary/standard_list";
    }

    @GetMapping("/standard/list")
    public String listStandards(HttpSession session, Model model,
                                @RequestParam(value = "standardCode", required = false) String standardCode,
                                @RequestParam(value = "positionName", required = false) String positionName)
    {
        User salary = (User) session.getAttribute("user");
        Integer l3OrgId = salary.getL3OrgId();

        // --- 1. 构建查询条件 ---
        QueryWrapper<SalaryStandardMaster> standardQuery = new QueryWrapper<>();
        standardQuery.eq("L3_Org_ID", l3OrgId); // 仅显示本部门标准
        standardQuery.orderByDesc("Submission_Time");

        // A. 薪酬标准编号模糊查询
        if (standardCode != null && !standardCode.trim().isEmpty()) {
            standardQuery.like("Standard_Code", standardCode.trim());
        }

        // B. 职位名称模糊查询 (需要联表查询，此处采用分步查询以兼容MyBatis-Plus)
        if (positionName != null && !positionName.trim().isEmpty()) {
            // 1. 根据模糊名称查询出所有符合条件的 Position ID
            List<Position> matchedPositions = positionMapper.selectList(
                    new QueryWrapper<Position>().like("Position_Name", positionName.trim())
            );

            if (matchedPositions.isEmpty()) {
                // 如果找不到任何匹配的职位，则设置一个不可能匹配的条件
                standardQuery.eq("Standard_ID", -1);
            } else {
                // 2. 将 ID 列表作为 IN 条件加入 Standard Master 查询
                List<Integer> filteredPositionIds = matchedPositions.stream()
                        .map(Position::getPositionId)
                        .collect(Collectors.toList());
                standardQuery.in("Position_ID", filteredPositionIds);
            }
        }

        // --- 2. 执行查询 ---
        List<SalaryStandardMaster> standards = standardMasterMapper.selectList(standardQuery);

        // --- 3. 批量获取职位名称映射 ---
        List<Integer> positionIdsToFetch = standards.stream()
                .map(SalaryStandardMaster::getPositionId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> positionNameMap = Map.of(); // 默认空Map
        if (!positionIdsToFetch.isEmpty()) {
            List<Map<String, Object>> posNameMaps = positionMapper.selectPositionNamesByIds(positionIdsToFetch);
            // 将 List<Map> 转换为 Map<Integer, String>
            positionNameMap = posNameMaps.stream()
                    .collect(Collectors.toMap(
                            map -> (Integer) map.get("Position_ID"),
                            map -> (String) map.get("Position_Name")
                    ));
        }

        // --- 4. 传入前端数据 ---
        model.addAttribute("standards", standards);
        model.addAttribute("positionNameMap", positionNameMap); // 【✅ 传入职位名称映射】

        // 传入查询参数，用于前端回显
        model.addAttribute("standardCode", standardCode);
        model.addAttribute("positionName", positionName);

        return "salary/standard_list";
    }

    @GetMapping("/standard/detail/{id}")
    public String viewStandardDetail(@PathVariable Integer id, Model model) {
        List<SalaryStandardDetail> details = salaryService.getStandardDetails(id);
        model.addAttribute("details", details);
        Map<Integer, String> itemMap = itemMapper.selectList(null).stream()
                .collect(Collectors.toMap(item -> item.getItemId(), item -> item.getItemName()));
        model.addAttribute("itemMap", itemMap);
        return "salary/standard_detail";
    }

    // =========================================
    // 工资登记与发放 (新的工作流程)
    // =========================================

    /**
     * 【新方法 1】执行一键登记，并跳转到新生成的草稿详情页。
     * 替换了原有的 /register/create (POST) 方法
     */
    @GetMapping("/register/pre-create")
    public String preCreateRegister(HttpSession session, Model model) {
        User salaryUser = (User) session.getAttribute("user");
        if (salaryUser == null || salaryUser.getL3OrgId() == null) {
            model.addAttribute("error", "用户会话或组织信息缺失。");
            // 这里我们调用 listRegisters 方法来返回 register_list 页面
            return listRegisters(session, model);
        }
        Integer l3OrgId = salaryUser.getL3OrgId();

        try {
            // 1. 调用 Service 执行登记逻辑
            salaryService.createMonthlyRegister(l3OrgId);

            // 2. 查询本月最新生成的单据 ID (假设 Audit_Status 为 'Pending' 表示刚生成)
            SalaryRegisterMaster latestRegister = registerMasterMapper.selectOne(
                    new QueryWrapper<SalaryRegisterMaster>()
                            .eq("L3_Org_ID", l3OrgId)
                            // 假设刚生成的单据状态是 Pending
                            .eq("Audit_Status", "Pending")
                            .orderByDesc("Register_ID")
                            .last("LIMIT 1")
            );

            if (latestRegister != null) {
                // 3. 重定向到新的草稿详情页
                return "redirect:/salary/register/draft-detail/" + latestRegister.getRegisterId();
            } else {
                model.addAttribute("error", "工资单生成失败，请重试或检查是否有员工信息。");
                return listRegisters(session, model); // 返回列表页
            }
        } catch (RuntimeException e) {
            model.addAttribute("error", "生成工资单时发生错误: " + e.getMessage());
            return listRegisters(session, model); // 返回列表页
        }
    }

    /**
     * 【新方法 2】展示待确认的工资单草稿详情。
     */
    @GetMapping("/register/draft-detail/{id}")
    public String viewRegisterDraftDetail(@PathVariable Integer id, Model model) {
        SalaryRegisterMaster master = registerMasterMapper.selectById(id);
        if (master == null) {
            return "redirect:/salary/register/list";
        }
        model.addAttribute("master", master);

        // 1. 获取所有薪酬项目 (用于表头)
        List<SalaryItem> allItems = itemMapper.selectList(null);
        model.addAttribute("allItems", allItems);

        // 2. 构建明细列表，使用私有方法
        List<Map<String, Object>> finalDetails = buildFinalDetails(id, allItems);
        model.addAttribute("finalDetails", finalDetails);

        // 加载新的模板，该模板包含发送审核按钮
        return "salary/register_detail_draft"; // 需要新建此 HTML 文件
    }

    /**
     * 【新方法 3】处理“发送审核”请求，更新工资单状态。
     */
    @PostMapping("/register/send-for-audit/{id}")
    public String sendRegisterForAudit(@PathVariable Integer id, HttpSession session, Model model) {
        User salaryUser = (User) session.getAttribute("user");
        if (salaryUser == null) {
            model.addAttribute("error", "用户会话信息缺失，请重新登录。");
            return "redirect:/salary/register/draft-detail/" + id;
        }

        // 1. 更新状态
        SalaryRegisterMaster master = new SalaryRegisterMaster();
        master.setRegisterId(id);
        master.setAuditStatus("Pending"); // 状态保持为 Pending (待审核)
        master.setSubmitterId(salaryUser.getUserId()); // 如果 Master 表中有这个字段

        registerMasterMapper.updateById(master);

        model.addAttribute("msg", "工资单 " + id + " 已成功提交审核！");
        // 2. 重定向到工资单列表页
        return "redirect:/salary/register/list";
    }

    /**
     * 查询历史工资单列表 (保持不变)
     */
    @GetMapping("/register/list")
    public String listRegisters(HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");
        // 增加 null 检查
        if (salary == null) {
            model.addAttribute("error", "用户会话已过期，请重新登录。");
            return "redirect:/salary/dashboard/home";
        }

        List<SalaryRegisterMaster> registers = registerMasterMapper.selectList(
                new QueryWrapper<SalaryRegisterMaster>()
                        .eq("L3_Org_ID", salary.getL3OrgId())
                        .orderByDesc("Pay_Date")
        );
        model.addAttribute("registers", registers);
        return "salary/register_list";
    }

    /**
     * 查看已提交/已审核的工资单明细
     */
    @GetMapping("/register/detail/{id}")
    public String viewRegisterDetail(@PathVariable Integer id, Model model) {
        SalaryRegisterMaster master = registerMasterMapper.selectById(id);
        if (master == null) {
            return "redirect:/salary/register/list";
        }
        model.addAttribute("master", master);

        // 1. 获取所有薪酬项目 (用于表头)
        List<SalaryItem> allItems = itemMapper.selectList(null);
        model.addAttribute("allItems", allItems);

        // 2. 构建明细列表
        List<Map<String, Object>> finalDetails = buildFinalDetails(id, allItems);
        model.addAttribute("finalDetails", finalDetails);

        return "salary/register_detail";
    }

    // =========================================
    // 私有辅助方法
    // =========================================
    /**
     * 辅助方法：构建工资单明细的表格数据
     * 【关键修复：根据 ItemName 读取明细表字段】
     */
    private List<Map<String, Object>> buildFinalDetails(Integer registerId, List<SalaryItem> allItems) {

        List<SalaryRegisterDetail> details = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>().eq("Register_ID", registerId)
        );
        if (details == null || details.isEmpty()) {
            return List.of();
        }

        // 1) 取所有 userId
        List<Integer> userIds = details.stream()
                .map(SalaryRegisterDetail::getUserId)
                .filter(uid -> uid != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, User> userMap = new HashMap<>();
        Map<Integer, Position> positionMap = new HashMap<>();
        Map<Integer, String> realNameMap = new HashMap<>();  // <--- 新增：真实姓名映射

        if (!userIds.isEmpty()) {
            // 2) 批量查 user
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) userMap.put(u.getUserId(), u);

            // 3) 批量查 position
            List<Integer> positionIds = users.stream()
                    .map(User::getPositionId)
                    .filter(pid -> pid != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (!positionIds.isEmpty()) {
                List<Position> positions = positionMapper.selectBatchIds(positionIds);
                for (Position p : positions) positionMap.put(p.getPositionId(), p);
            }

            // 4) 批量查 personnel file（真实姓名）
            List<PersonnelFile> files = personnelFileMapper.selectList(
                    new QueryWrapper<PersonnelFile>()
                            .in("User_ID", userIds)
                            .eq("Is_Deleted", 0)   // 你表里有软删字段就加；没有这列就删掉这行
            );
            for (PersonnelFile f : files) {
                realNameMap.put(f.getUserId(), f.getName());
            }
        }

        // 5) 组装最终明细
        return details.stream().map(d -> {
            Map<String, Object> row = new HashMap<>();
            row.put("userId", d.getUserId());

            User u = userMap.get(d.getUserId());
            if (u != null) {
                // 优先真实姓名；查不到就降级用 username
                String realName = realNameMap.get(d.getUserId());
                row.put("userName", realName != null ? realName : u.getUsername());

                Position p = positionMap.get(u.getPositionId());
                row.put("positionName", p != null ? p.getPositionName() : "");
            } else {
                row.put("userName", "未知用户");
                row.put("positionName", "");
            }

            Map<Integer, BigDecimal> itemValues = new HashMap<>();
            for (SalaryItem item : allItems) {
                String itemName = item.getItemName();
                BigDecimal value = BigDecimal.ZERO;

                if ("基本工资".equals(itemName)) {
                    value = d.getBaseSalary();
                } else if ("交通补贴".equals(itemName) || "补贴".equals(itemName)) {
                    value = d.getTotalSubsidy();
                } else if ("全勤奖".equals(itemName) || "奖金".equals(itemName) || "KPI 单价".equals(itemName)) {
                    value = d.getKpiBonus();
                } else if ("加班费".equals(itemName)) {
                    value = d.getOvertimePay();
                } else if ("考勤调整".equals(itemName)) {
                    value = d.getAttendanceAdjustment();
                } else {
                    value = BigDecimal.ZERO;
                }

                itemValues.put(item.getItemId(), value != null ? value : BigDecimal.ZERO);
            }

            row.put("itemValues", itemValues);
            row.put("grossMoney", d.getGrossMoney());
            return row;

        }).collect(Collectors.toList());
    }


}
