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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

        // D. 获取**当前用户所属机构**的所有职位 (供下拉列表使用)
        // 从 User 对象中获取 L3 机构 ID
        Integer l3OrgId = user.getL3OrgId();

        // 调用 PositionMapper 中根据机构 ID 查询的方法
        // 注意：T_Position 表需要有 L3_Org_ID 字段，否则会查询失败或查出所有职位
        List<Position> positions = positionMapper.selectPositionsInL3Org(l3OrgId); //
        model.addAttribute("positions", positions);

        // E. 获取所有薪酬项目 (供复选框使用)
        model.addAttribute("items", itemMapper.selectList(null));

        return "salary/standard_add";
    }

    // 2. 提交薪酬标准 (AJAX POST)
    @PostMapping("/standard/save")
    public String saveStandard(SalaryStandardDTO dto, HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");
        if (salary == null) {
            model.addAttribute("error", "用户会话已过期，请重新登录。");
            return "redirect:/salary/dashboard/home";
        }
        // 执行保存逻辑
        salaryService.submitStandard(dto, salary.getUserId(), salary.getL3OrgId());

        model.addAttribute("msg", "薪酬标准 " + dto.getStandardCode() + " 已提交，等待审核。");
        model.addAttribute("standards", salaryService.getStandardsByOrg(salary.getL3OrgId()));
        return "redirect:/salary/standard/list";
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
        if (standards == null) {
            standards = List.of();
        }

        // --- 3. 批量获取职位名称映射 ---
        List<Integer> positionIdsToFetch = standards.stream()
                .map(SalaryStandardMaster::getPositionId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> positionNameMap = Map.of(); // 默认空Map
        if (!positionIdsToFetch.isEmpty()) {
            List<Map<String, Object>> posNameMaps =
                    positionMapper.selectPositionNamesByIds(positionIdsToFetch);

            positionNameMap = posNameMaps.stream()
                    .collect(Collectors.toMap(
                            map -> (Integer) map.get("Position_ID"),
                            map -> (String) map.get("Position_Name")
                    ));
        }

// --- 4. 传入前端数据 ---
        model.addAttribute("standards", standards);
        model.addAttribute("positionNameMap", positionNameMap);

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

    @GetMapping("/register/pre-create")
    public String preCreateRegisterPage() {
        return "salary/register_pre_create";
    }
    /**
     * 【新方法 1】执行一键登记，并跳转到新生成的草稿详情页。
     * 替换了原有的 /register/create (POST) 方法
     */
    @PostMapping("/register/create")
    public String createRegister(@RequestParam("days") Integer days,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        User user = (User) session.getAttribute("user");
        if (user == null || user.getL3OrgId() == null) {
            ra.addFlashAttribute("error", "用户未登录或机构信息缺失。");
            return "redirect:/login";
        }

        if (days == null || days <= 0 || days > 31) {
            ra.addFlashAttribute("error", "请录入有效的本月标准工作天数 (1-31)。");
            return "redirect:/salary/register/pre-create";
        }

        try {
            // 调用 Service 层的新方法，传入标准工作天数
            salaryService.createMonthlyRegister(user.getL3OrgId(), days);
            ra.addFlashAttribute("msg", "工资单草稿已生成，请进入详情页录入绩效。");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "生成工资单失败: " + e.getMessage());
        }

        // 简单跳转到列表页，让用户自己选择最新的 Draft
        return "redirect:/salary/register/list";
    }

    // ----------------------------------------------------------------
    // 新增：录入 KPI 分数并重算工资
    // ----------------------------------------------------------------
    /**
     * 处理薪酬经理提交的 KPI 分数，并触发工资重算。
     * @param detailId 正在编辑的工资明细 ID
     * @param scores KPI 分项得分 (这里假设按顺序传递：质量, 效率, 协作)
     * @param registerId 工资主表 ID，用于跳转回详情页
     */
    @PostMapping("/register/save-kpi")
    public String saveKPI(@RequestParam("detailId") Integer detailId,
                          @RequestParam("scores") List<BigDecimal> scores,
                          @RequestParam("registerId") Integer registerId,
                          RedirectAttributes ra) {

        if (scores.size() < 3) {
            ra.addFlashAttribute("error", "KPI 分项分数不足，请至少录入3项分数。");
            return "redirect:/salary/register/draft-detail/" + registerId;
        }

        try {
            // 构造 KPI Item 列表 (硬编码示例：质量40%, 效率30%, 协作30%)
            List<KPIItemRecord> items = new ArrayList<>();
            items.add(createKPI("工作质量", new BigDecimal("0.4"), scores.get(0)));
            items.add(createKPI("工作效率", new BigDecimal("0.3"), scores.get(1)));
            items.add(createKPI("团队协作", new BigDecimal("0.3"), scores.get(2)));

            // 调用 Service 层进行保存和重算
            salaryService.updateEmployeeKPI(detailId, items);
            ra.addFlashAttribute("msg", "绩效分数已更新，工资已自动重新计算。");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "录入绩效失败: " + e.getMessage());
        }

        return "redirect:/salary/register/draft-detail/" + registerId;
    }

    /**
     * 辅助方法：创建 KPI Item 实体
     */
    private KPIItemRecord createKPI(String name, BigDecimal weight, BigDecimal score) {
        KPIItemRecord r = new KPIItemRecord();
        r.setItemName(name);
        r.setWeight(weight);
        r.setScore(score);
        return r;
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

        // 2. 构建明细列表
        List<Map<String, Object>> finalDetails = buildFinalDetails(id, allItems);

        // 【修正】前端模板 register_detail_draft.html 使用的是 ${employeeDetails}
        model.addAttribute("employeeDetails", finalDetails);

        return "salary/register_detail_draft";
    }

    /**
     * 【新方法 3】处理“发送审核”请求，更新工资单状态。
     */
    @PostMapping("/register/send-for-audit/{id}")
    public String sendRegisterForAudit(@PathVariable Integer id,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        User salaryUser = (User) session.getAttribute("user");
        if (salaryUser == null) {
            redirectAttributes.addFlashAttribute("error", "用户会话信息缺失，请重新登录。");
            return "redirect:/salary/register/draft-detail/" + id;
        }

        // 0) 查询当前工资单
        SalaryRegisterMaster current = registerMasterMapper.selectById(id);
        if (current == null) {
            redirectAttributes.addFlashAttribute("error", "工资单不存在，无法提交审核。");
            return "redirect:/salary/register/list";
        }

        String status = current.getAuditStatus();
        Integer l3OrgId = current.getL3OrgId();
        Object payDate = current.getPayDate(); // 兼容 payDate 为 String/Date/LocalDate 等

        // 1) bug1：本单 Pending，驳回前不能重复发送
        if ("Pending".equals(status)) {
            redirectAttributes.addFlashAttribute("error", "该工资单已处于待审核状态，驳回前不能重复提交。");
            return "redirect:/salary/register/detail/" + id;
        }

        // 2) bug2：本单 Approved，本月已发放，不允许再发送审核
        if ("Approved".equals(status)) {
            redirectAttributes.addFlashAttribute("error", "该工资单已发放，不能再次提交审核。");
            return "redirect:/salary/register/detail/" + id;
        }

        // 3) 跨单校验：同机构同月份存在 Pending 或 Approved（排除自己）时禁止提交
        Long conflictCount = registerMasterMapper.selectCount(
                new QueryWrapper<SalaryRegisterMaster>()
                        .eq("L3_Org_ID", l3OrgId)
                        .eq("Pay_Date", payDate)
                        .ne("Register_ID", id)
                        .in("Audit_Status", Arrays.asList("Pending", "Approved"))
        );

        if (conflictCount != null && conflictCount > 0) {
            Long pendingCount = registerMasterMapper.selectCount(
                    new QueryWrapper<SalaryRegisterMaster>()
                            .eq("L3_Org_ID", l3OrgId)
                            .eq("Pay_Date", payDate)
                            .ne("Register_ID", id)
                            .eq("Audit_Status", "Pending")
            );

            if (pendingCount != null && pendingCount > 0) {
                redirectAttributes.addFlashAttribute("error", "本月已有待审核工资单，驳回前不能再次发送审核。");
            } else {
                redirectAttributes.addFlashAttribute("error", "本月工资单已发放，不能再发送审核。");
            }
            return "redirect:/salary/register/detail/" + id;
        }

        // 4) 允许提交：Draft / Rejected -> Pending
        SalaryRegisterMaster update = new SalaryRegisterMaster();
        update.setRegisterId(id);
        update.setAuditStatus("Pending");
        update.setSubmitterId(salaryUser.getUserId());
        registerMasterMapper.updateById(update);

        redirectAttributes.addFlashAttribute("msg", "工资单 " + id + " 已成功提交审核！");
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
            // 这里应该返回登录页面的视图名称，但根据现有代码，我们重定向到 /dashboard/home，让 Thymeleaf 捕获错误
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
     * 查看已提交/已审核的工资单明细 (已修改，调用私有方法)
     */
    @GetMapping("/register/detail/{id}")
    public String viewRegisterDetail(@PathVariable Integer id, Model model) {
        SalaryRegisterMaster master = registerMasterMapper.selectById(id);
        if (master == null) {
            return "redirect:/salary/register/list";
        }
        model.addAttribute("master", master);

        List<SalaryItem> allItems = itemMapper.selectList(null);
        model.addAttribute("allItems", allItems);

        List<Map<String, Object>> finalDetails = buildFinalDetails(id, allItems);

        // 原有只读详情页模板 register_detail.html 使用的是 ${finalDetails}
        // 保持一致，无需修改
        model.addAttribute("finalDetails", finalDetails);

        return "salary/register_detail";
    }

    // =========================================
    // 私有辅助方法：提取自原 viewRegisterDetail
    // =========================================

    /**
     * 辅助方法：构建工资单明细的表格数据
     */
    private List<Map<String, Object>> buildFinalDetails(Integer registerId, List<SalaryItem> allItems) {
        // 1. 获取发放详情
        List<SalaryRegisterDetail> details = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>().eq("Register_ID", registerId)
        );

        if (details == null || details.isEmpty()) {
            return List.of(); // 返回空列表而不是 null，防止前端报错
        }

        // 2. 获取所有员工和职位信息
        List<Integer> userIds = details.stream()
                .map(SalaryRegisterDetail::getUserId)
                .filter(uid -> uid != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, User> userMap = new HashMap<>();
        Map<Integer, Position> positionMap = new HashMap<>();

        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getUserId, u -> u));

            List<Integer> positionIds = users.stream()
                    .map(User::getPositionId)
                    .filter(pid -> pid != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (!positionIds.isEmpty()) {
                List<Position> positions = positionMapper.selectBatchIds(positionIds);
                positionMap = positions.stream().collect(Collectors.toMap(Position::getPositionId, p -> p));
            }
        }

        // 3. 构造最终展示的列表
        Map<Integer, User> finalUserMap = userMap;
        Map<Integer, Position> finalPositionMap = positionMap;

        return details.stream().map(detail -> {
            Map<String, Object> empDetail = new HashMap<>();

            User user = finalUserMap.get(detail.getUserId());
            Position position = (user != null && user.getPositionId() != null)
                    ? finalPositionMap.get(user.getPositionId())
                    : null;

            // --- 基础信息 ---
            empDetail.put("userId", detail.getUserId());
            empDetail.put("userName", user != null ? user.getUsername() : "未知用户");
            empDetail.put("positionName", position != null ? position.getPositionName() : "-");

            // --- 【关键修正】添加草稿页和KPI功能所需的字段 ---
            empDetail.put("detailId", detail.getDetailId()); // 必须！用于KPI录入弹窗
            empDetail.put("attendanceCount", detail.getAttendanceCount()); // 必须！显示出勤天数
            empDetail.put("kpiUnits", detail.getKpiUnits()); // 必须！显示KPI系数
            empDetail.put("attendanceAdjustment", detail.getAttendanceAdjustment());
            empDetail.put("kpiBonus", detail.getKpiBonus());
            empDetail.put("baseSalary", detail.getBaseSalary());
            empDetail.put("totalSubsidy", detail.getTotalSubsidy());
            empDetail.put("grossMoney", detail.getGrossMoney());

            // --- 动态列 (兼容旧模板) ---
            Map<Integer, BigDecimal> itemValues = new HashMap<>();
            // 如果您的 allItems 为空，这里的逻辑不会执行，但为了兼容性保留
            if (allItems != null) {
                for (SalaryItem item : allItems) {
                    // 这里可以根据实际情况完善映射，但在草稿页我们主要用上面的直接字段
                    if ("基本工资".equals(item.getItemName())) {
                        itemValues.put(item.getItemId(), detail.getBaseSalary());
                    } else if ("全勤奖".equals(item.getItemName())) {
                        itemValues.put(item.getItemId(), detail.getAttendanceAdjustment()); // 示例映射
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