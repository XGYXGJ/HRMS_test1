package com.example.hrms.controller;

import com.example.hrms.dto.SalaryStandardDTO;
import com.example.hrms.entity.SalaryStandardDetail;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.SalaryItemMapper;
import com.example.hrms.service.SalaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/salary")
public class SalaryMgrController {

    @Autowired
    private SalaryService salaryService;
    @Autowired
    private SalaryItemMapper itemMapper;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // 在仪表盘简单展示一些信息，或者直接返回视图
        return "salary/dashboard";
    }

    // 1. 制定薪酬标准页面
    @GetMapping("/standard/new")
    public String createStandardPage(Model model) {
        model.addAttribute("items", itemMapper.selectList(null));
        return "salary/standard_add";
    }

    // 2. 提交薪酬标准
    @PostMapping("/standard/save")
    public String saveStandard(SalaryStandardDTO dto, HttpSession session) {
        User salary = (User) session.getAttribute("user");
        salaryService.submitStandard(dto, salary.getUserId(), salary.getL3OrgId());
        return "redirect:/salary/standard/list?msg=Submitted"; // 提交后跳转到列表页
    }

    // 【新增】3. 薪酬标准列表页
    @GetMapping("/standard/list")
    public String listStandards(HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");
        model.addAttribute("standards", salaryService.getStandardsByOrg(salary.getL3OrgId()));
        return "salary/standard_list"; // 需要新建这个 HTML
    }

    // 【新增】4. 查看标准详情
    @GetMapping("/standard/detail/{id}")
    public String viewStandardDetail(@PathVariable Integer id, Model model) {
        List<SalaryStandardDetail> details = salaryService.getStandardDetails(id);
        model.addAttribute("details", details);

        // 为了显示项目名称，我们可以查出所有Item传过去，或者在Service组装好VO
        // 这里简单处理：传ItemMap给前端
        Map<Integer, String> itemMap = itemMapper.selectList(null).stream()
                .collect(Collectors.toMap(item -> item.getItemId(), item -> item.getItemName()));
        model.addAttribute("itemMap", itemMap);

        return "salary/standard_detail"; // 需要新建这个 HTML
    }

    // 5. 一键登记本月工资
    @PostMapping("/salary/register")
    public String registerSalary(HttpSession session, Model model) {
        User salary = (User) session.getAttribute("user");
        try {
            salaryService.createMonthlyRegister(salary.getL3OrgId());
        } catch (RuntimeException e) {
            // 捕获重复登记的异常，返回错误信息
            return "redirect:/salary/dashboard?error=" + e.getMessage();
        }
        return "redirect:/salary/dashboard?msg=SalaryRegistered";
    }

}
