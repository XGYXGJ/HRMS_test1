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

    // ... (dashboard 等其他方法保持不变) ...
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        return "salary/dashboard";
    }

    @GetMapping("/dashboard/home")
    public String dashboardHome() {
        return "salary/dashboard_home";
    }

    // ================== 薪酬标准管理 ==================

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

    // 2. 提交薪酬标准 (AJAX POST) - 【微调】
    @PostMapping("/standard/save")
    public String saveStandard(SalaryStandardDTO dto, HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");

        // 注意：这里需要确保 Service 层处理了 dto.getStandardCode() 并保存到数据库
        // 如果 Service 没改，你需要修改 Service 或者在这里手动处理 Entity 转换
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

    @PostMapping("/register/create")
    public String registerSalary(HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");
        try {
            salaryService.createMonthlyRegister(salary.getL3OrgId());
            model.addAttribute("msg", "本月工资单已成功生成！");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        List<SalaryRegisterMaster> registers = registerMasterMapper.selectList(
                new QueryWrapper<SalaryRegisterMaster>()
                        .eq("L3_Org_ID", salary.getL3OrgId())
                        .orderByDesc("Pay_Date")
        );
        model.addAttribute("registers", registers);
        return "salary/register_list";
    }

    @GetMapping("/register/list")
    public String listRegisters(HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");
        List<SalaryRegisterMaster> registers = registerMasterMapper.selectList(
                new QueryWrapper<SalaryRegisterMaster>()
                        .eq("L3_Org_ID", salary.getL3OrgId())
                        .orderByDesc("Pay_Date")
        );
        model.addAttribute("registers", registers);
        return "salary/register_list";
    }

    @GetMapping("/register/detail/{id}")
    public String viewRegisterDetail(@PathVariable Integer id, Model model) {
        SalaryRegisterMaster master = registerMasterMapper.selectById(id);
        // 增加安全检查：如果查不到单据，直接返回列表页，防止模板报空指针
        if (master == null) {
            return "redirect:/salary/register/list";
        }
        model.addAttribute("master", master);

        // 1. 获取所有薪酬项目 (用于表头)
        List<SalaryItem> allItems = itemMapper.selectList(null);
        model.addAttribute("allItems", allItems);

        // 2. 获取发放详情
        List<SalaryRegisterDetail> details = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>().eq("Register_ID", id)
        );

        // --- 【修复开始：增加空列表和Null值的安全检查】 ---

        // 预定义空 Map，防止后续逻辑报错
        Map<Integer, User> userMap = new HashMap<>();
        Map<Integer, Position> positionMap = new HashMap<>();

        if (details != null && !details.isEmpty()) {
            // A. 安全获取 User IDs (去重)
            List<Integer> userIds = details.stream()
                    .map(SalaryRegisterDetail::getUserId)
                    .filter(uid -> uid != null) // 过滤掉可能的 null
                    .distinct()
                    .collect(Collectors.toList());

            // B. 只有当 ID 列表不为空时才查询数据库
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectBatchIds(userIds);
                // 转换为 Map
                userMap = users.stream().collect(Collectors.toMap(User::getUserId, u -> u));

                // C. 安全获取 Position IDs (过滤 null + 去重)
                List<Integer> positionIds = users.stream()
                        .map(User::getPositionId)
                        .filter(pid -> pid != null) // 【关键点】必须过滤掉 null，否则 selectBatchIds 会报错
                        .distinct()
                        .collect(Collectors.toList());

                // D. 只有当 Position ID 列表不为空时才查询
                if (!positionIds.isEmpty()) {
                    List<Position> positions = positionMapper.selectBatchIds(positionIds);
                    positionMap = positions.stream().collect(Collectors.toMap(Position::getPositionId, p -> p));
                }
            }
        }
        // --- 【修复结束】 ---

        // 5. 构建最终的明细列表
        // 需要使用 final 变量或者重新赋值给临时变量以供 lambda 使用
        Map<Integer, User> finalUserMap = userMap;
        Map<Integer, Position> finalPositionMap = positionMap;

        List<Map<String, Object>> finalDetails = details.stream().map(detail -> {
            Map<String, Object> empDetail = new HashMap<>();

            User user = finalUserMap.get(detail.getUserId());
            Position position = (user != null && user.getPositionId() != null)
                    ? finalPositionMap.get(user.getPositionId())
                    : null;

            empDetail.put("userId", detail.getUserId());
            empDetail.put("userName", user != null ? user.getUsername() : "未知用户"); // 防止 user 为 null
            empDetail.put("positionName", position != null ? position.getPositionName() : "-"); // 防止 position 为 null
            empDetail.put("grossMoney", detail.getGrossMoney());

            Map<Integer, BigDecimal> itemValues = new HashMap<>();
            for (SalaryItem item : allItems) {
                if ("基本工资".equals(item.getItemName())) {
                    itemValues.put(item.getItemId(), detail.getBaseSalary());
                } else {
                    itemValues.put(item.getItemId(), BigDecimal.ZERO);
                }
            }
            empDetail.put("itemValues", itemValues);

            return empDetail;
        }).collect(Collectors.toList());

        model.addAttribute("finalDetails", finalDetails);

        return "salary/register_detail";
    }
}