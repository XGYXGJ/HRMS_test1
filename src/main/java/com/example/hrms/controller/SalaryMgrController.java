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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/salary")
public class SalaryMgrController {

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

    // 1. 制定薪酬标准页面 (片段) - 【已修改】
    @GetMapping("/standard/new")
    public String createStandardPage(HttpSession session, Model model) {
        // A. 获取当前用户
        User user = (User) session.getAttribute("user");
        model.addAttribute("currentUser", user);

        // B. 生成自动编号: FAT + yyyyMMdd + XX
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

        // D. 获取所有职位 (供下拉列表使用)
        List<Position> positions = positionMapper.selectAll();
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

    // ... (后续方法 standard/list, standard/detail, register/* 等保持不变) ...
    @GetMapping("/standard/list")
    public String listStandards(HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");
        model.addAttribute("standards", salaryService.getStandardsByOrg(salary.getL3OrgId()));
        return "salary/standard_list";
    }

    // ... 其他方法省略 ...
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
        model.addAttribute("master", master);
        List<SalaryRegisterDetail> details = registerDetailMapper.selectList(
                new QueryWrapper<SalaryRegisterDetail>().eq("Register_ID", id)
        );
        model.addAttribute("details", details);
        return "salary/register_detail";
    }
}