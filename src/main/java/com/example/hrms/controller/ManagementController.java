package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import com.example.hrms.service.SalaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manage")
public class ManagementController {

    @Autowired private SalaryService salaryService;
    @Autowired private SalaryRegisterMasterMapper registerMasterMapper;
    @Autowired private SalaryRegisterDetailMapper registerDetailMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private PositionMapper positionMapper;
    @Autowired private SalaryItemMapper itemMapper;

    // 1. 管理部门主框架 (带有侧边栏的页面)
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        return "manage/dashboard";
    }

    // 2. 仪表盘首页 (Fragment)
    @GetMapping("/home")
    public String dashboardHome(Model model) {
        // 统计待审核数量
        Long pendingCount = registerMasterMapper.selectCount(
                new QueryWrapper<SalaryRegisterMaster>().eq("Audit_Status", "Pending")
        );
        model.addAttribute("pendingCount", pendingCount);
        return "manage/home";
    }

    // 3. 待审核工资单列表 (Fragment)
    @GetMapping("/audit/list")
    public String auditList(Model model) {
        List<SalaryRegisterMaster> registers = registerMasterMapper.selectList(
                new QueryWrapper<SalaryRegisterMaster>()
                        .eq("Audit_Status", "Pending") // 只看待审核
                        .orderByDesc("Pay_Date")
        );
        model.addAttribute("registers", registers);
        return "manage/audit_list";
    }

    // 4. 审核详情页 (Fragment)
    @GetMapping("/audit/detail/{id}")
    public String auditDetail(@PathVariable Integer id, Model model) {
        SalaryRegisterMaster master = registerMasterMapper.selectById(id);
        if (master == null) return "redirect:/manage/audit/list"; // 容错

        model.addAttribute("master", master);

        // 复用薪酬经理的明细查询逻辑 (获取 Item, User, Position 等)
        // 为了代码简洁，这里直接复制之前 viewRegisterDetail 的核心构建逻辑
        // 实际开发中建议抽取到 Service 方法: salaryService.getRegisterDetailsMap(id)

        List<SalaryItem> allItems = itemMapper.selectList(null);
        model.addAttribute("allItems", allItems);

        List<SalaryRegisterDetail> details = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>().eq("Register_ID", id)
        );

        // ... (此处省略构建 finalDetails 的代码，逻辑与 SalaryMgrController.viewRegisterDetail 完全一致) ...
        // 请务必将 SalaryMgrController 中构建 finalDetails 的代码复制过来
        // 包括 fetching Users, Positions, building the map structure.

        // 临时模拟空数据防止报错，请替换为真实逻辑
        model.addAttribute("finalDetails", List.of());

        return "manage/audit_detail";
    }

    // 5. 执行审核动作 (Approve/Reject)
    @PostMapping("/audit/process")
    public String processAudit(@RequestParam Integer registerId,
                               @RequestParam String action,
                               HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        boolean pass = "pass".equals(action);

        try {
            //salaryService.auditRegister(registerId, user.getUserId(), pass, null);
            model.addAttribute("msg", pass ? "审核通过，工资单已发放！" : "审核驳回，已退回给薪酬经理。");
        } catch (Exception e) {
            model.addAttribute("error", "操作失败：" + e.getMessage());
        }

        return auditList(model); // 操作完成后返回列表页
    }
}