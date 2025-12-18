package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工端控制器
 */
@Controller
@RequestMapping("/emp")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired private PersonnelFileMapper personnelFileMapper;
    @Autowired private PositionMapper positionMapper;
    @Autowired private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired private SalaryRegisterDetailMapper salaryRegisterDetailMapper;
    @Autowired private SalaryRegisterMasterMapper salaryRegisterMasterMapper;
    @Autowired private SalaryItemMapper salaryItemMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private OrganizationMapper organizationMapper;
    @Autowired private SalaryStandardMasterMapper standardMasterMapper;
    @Autowired private SalaryStandardDetailMapper standardDetailMapper;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        return "emp/emp_dashboard";
    }

    @PostMapping("/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(HttpSession session,
                                              @RequestParam String currentPassword,
                                              @RequestParam String newPassword) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return response;
        }

        User dbUser = userMapper.selectById(user.getUserId());
        if (!dbUser.getPasswordHash().equals(currentPassword)) {
            response.put("success", false);
            response.put("message", "当前密码不正确");
            return response;
        }

        dbUser.setPasswordHash(newPassword);
        userMapper.updateById(dbUser);

        // 更新 session 中的用户信息
        session.setAttribute("user", dbUser);

        response.put("success", true);
        return response;
    }

    @PostMapping("/punch-in")
    public String punchIn(HttpSession session, RedirectAttributes attrs) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        LocalDate today = LocalDate.now();

        Long count = attendanceRecordMapper.selectCount(
                new QueryWrapper<AttendanceRecord>()
                        .eq("User_ID", user.getUserId())
                        .eq("Punch_Date", today)
        );

        if (count != null && count > 0) {
            attrs.addFlashAttribute("error", "您今天已经打过卡了，无需重复打卡。");
        } else {
            AttendanceRecord record = new AttendanceRecord();
            record.setUserId(user.getUserId());
            record.setAttendanceDate(today);
            record.setPunchInTime(LocalDateTime.now());
            attendanceRecordMapper.insert(record);

            attrs.addFlashAttribute("msg",
                    "打卡成功！时间：" + LocalDateTime.now());
        }

        return "redirect:/emp/attendance";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        PersonnelFile profile = personnelFileMapper.selectOne(
                new QueryWrapper<PersonnelFile>()
                        .eq("User_ID", user.getUserId())
        );
        model.addAttribute("profile", profile);

        if (profile != null && profile.getL3OrgId() != null) {
            Organization org = organizationMapper.selectById(profile.getL3OrgId());
            if (org != null) {
                model.addAttribute("organizationName", org.getOrgName());
            }
        }

        Position position = null;
        if (user.getPositionId() != null) {
            position = positionMapper.selectById(user.getPositionId());
        }
        model.addAttribute("position", position);

        return "emp/emp_profile";
    }

    @GetMapping("/attendance")
    public String attendance(HttpSession session,
                             Model model,
                             @RequestParam(value = "period", required = false, defaultValue = "week") String period) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        LocalDate today = LocalDate.now();

        AttendanceRecord todayRecord = attendanceRecordMapper.selectOne(
                new QueryWrapper<AttendanceRecord>()
                        .eq("User_ID", user.getUserId())
                        .eq("Punch_Date", today)
        );
        boolean hasPunchedIn = todayRecord != null && todayRecord.getPunchInTime() != null;

        model.addAttribute("currentDate", today);
        model.addAttribute("hasPunchedIn", hasPunchedIn);
        model.addAttribute("punchInTime",
                todayRecord != null ? todayRecord.getPunchInTime() : null);

        QueryWrapper<AttendanceRecord> listWrapper =
                new QueryWrapper<AttendanceRecord>()
                        .eq("User_ID", user.getUserId());

        if (!"all".equalsIgnoreCase(period)) {
            LocalDate startDate;
            if ("month".equalsIgnoreCase(period)) {
                YearMonth ym = YearMonth.from(today);
                startDate = ym.atDay(1);
            } else {
                startDate = today.minusDays(6);
            }
            listWrapper.ge("Punch_Date", startDate)
                    .le("Punch_Date", today);
        }

        listWrapper.orderByDesc("Punch_Date");
        List<AttendanceRecord> attendanceRecords =
                attendanceRecordMapper.selectList(listWrapper);

        model.addAttribute("attendanceRecords", attendanceRecords);
        model.addAttribute("period", period);

        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate nextMonthStart = currentMonth.plusMonths(1).atDay(1);

        List<AttendanceRecord> monthRecords = attendanceRecordMapper.selectList(
                new QueryWrapper<AttendanceRecord>()
                        .eq("User_ID", user.getUserId())
                        .ge("Punch_Date", monthStart)
                        .lt("Punch_Date", nextMonthStart)
        );

        int currentMonthAttendance = monthRecords != null ? monthRecords.size() : 0;
        int fullAttendanceDays = currentMonthAttendance;
        int lateCount = 0;
        int earlyLeaveCount = 0;
        String attendanceRate = currentMonthAttendance > 0 ? "100%" : "0%";

        model.addAttribute("currentMonthAttendance", currentMonthAttendance);
        model.addAttribute("fullAttendanceDays", fullAttendanceDays);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("earlyLeaveCount", earlyLeaveCount);
        model.addAttribute("attendanceRate", attendanceRate);

        return "emp/emp_attendance";
    }

    /**
     * 查看员工自己的工资单明细
     */
    @GetMapping("/salary")
    public String salary(HttpSession session,
                         Model model,
                         @RequestParam(value = "month", required = false) String month) {

        // 1. 获取当前登录用户
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // 2. 构造查询条件：查"主表"，筛选 Status=Approved
        QueryWrapper<SalaryRegisterMaster> masterQuery = new QueryWrapper<>();
        masterQuery.eq("Audit_Status", "Approved");

        if (month != null && !month.isEmpty()) {
            try {
                YearMonth ym = YearMonth.parse(month);
                LocalDate startDate = ym.atDay(1);
                LocalDate endDate = ym.plusMonths(1).atDay(1);
                masterQuery.ge("Pay_Date", startDate).lt("Pay_Date", endDate);
            } catch (Exception e) {
                // 防止日期格式错误
            }
        }
        masterQuery.orderByDesc("Pay_Date");

        List<SalaryRegisterMaster> approvedMasters = salaryRegisterMasterMapper.selectList(masterQuery);

        // 3. 获取所有薪酬项目
        List<SalaryItem> allItems = salaryItemMapper.selectList(null);
        final List<SalaryItem> finalAllItems = allItems != null ? allItems : Collections.emptyList();
        model.addAttribute("allItems", finalAllItems);

        // 如果没有已发放的工资单，直接返回空
        if (approvedMasters.isEmpty()) {
            model.addAttribute("salaryList", Collections.emptyList());
            model.addAttribute("registerCodeMap", Collections.emptyMap());
            return "emp/emp_salary";
        }

        // 4. 准备 RegisterCodeMap
        List<Integer> registerIds = approvedMasters.stream()
                .map(SalaryRegisterMaster::getRegisterId)
                .collect(Collectors.toList());

        Map<Integer, String> registerCodeMap = approvedMasters.stream()
                .collect(Collectors.toMap(
                        SalaryRegisterMaster::getRegisterId,
                        SalaryRegisterMaster::getRegisterCode
                ));

        // 【修改点在这里】：确保 Key 名称与 HTML 中的 ${registerCodeMap} 一致
        model.addAttribute("registerCodeMap", registerCodeMap);

        // 5. 查询明细
        QueryWrapper<SalaryRegisterDetail> detailQuery = new QueryWrapper<>();
        detailQuery.eq("User_ID", user.getUserId());
        detailQuery.in("Register_ID", registerIds);
        detailQuery.orderByDesc("Payroll_Month");

        List<SalaryRegisterDetail> rawDetails = salaryRegisterDetailMapper.selectList(detailQuery);

        // 6. 数据组装
        List<Map<String, Object>> processedList = new ArrayList<>();

        for (SalaryRegisterDetail detail : rawDetails) {
            Map<String, Object> row = new HashMap<>();

            // 基础信息
            row.put("registerId", detail.getRegisterId());
            row.put("payrollMonth", detail.getPayrollMonth());
            row.put("grossMoney", detail.getGrossMoney());
            row.put("insuranceFee", detail.getInsuranceFee());
            // 补充：为了防止数字格式化报错，建议处理一下 null 值
            row.put("baseSalary", detail.getBaseSalary() != null ? detail.getBaseSalary() : BigDecimal.ZERO);
            row.put("kpiBonus", detail.getKpiBonus() != null ? detail.getKpiBonus() : BigDecimal.ZERO);

            // --- 动态映射开始 ---
            Map<Integer, BigDecimal> itemValues = new HashMap<>();

            for (SalaryItem item : finalAllItems) {
                BigDecimal value = BigDecimal.ZERO;
                String name = item.getItemName();

                if ("基本工资".equals(name) && detail.getBaseSalary() != null) {
                    value = detail.getBaseSalary();
                } else if ("全勤奖".equals(name) && detail.getAttendanceAdjustment() != null) {
                    value = detail.getAttendanceAdjustment();
                } else if ("交通补贴".equals(name) && detail.getTotalSubsidy() != null) {
                    value = detail.getTotalSubsidy();
                } else if (("绩效奖金".equals(name) || "KPI奖金".equals(name)) && detail.getKpiBonus() != null) {
                    value = detail.getKpiBonus();
                }
                itemValues.put(item.getItemId(), value);
            }
            row.put("itemValues", itemValues);
            processedList.add(row);
        }

        model.addAttribute("salaryList", processedList);
        model.addAttribute("selectedMonth", month);

        return "emp/emp_salary";
    }

    /**
     * API: 获取当前员工的最新薪酬标准详情 (返回JSON)
     */
    @GetMapping("/api/standard")
    @ResponseBody // 关键注解：表示返回数据而不是页面
    public Map<String, Object> getMyStandardApi(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        User user = (User) session.getAttribute("user");
        if (user == null) {
            result.put("success", false);
            result.put("msg", "登录已失效");
            return result;
        }

        // 1. 查询 Master (逻辑同之前：机构+职位+Approved+最新)
        SalaryStandardMaster master = standardMasterMapper.selectOne(
                new QueryWrapper<com.example.hrms.entity.SalaryStandardMaster>()
                        .eq("L3_Org_ID", user.getL3OrgId())
                        .eq("Position_ID", user.getPositionId())
                        .eq("Audit_Status", "Approved")
                        .orderByDesc("Submission_Time")
                        .last("LIMIT 1")
        );

        if (master == null) {
            result.put("success", false);
            result.put("msg", "未找到生效的薪酬标准");
            return result;
        }

        // 2. 查询明细
        List<com.example.hrms.entity.SalaryStandardDetail> details = standardDetailMapper.selectList(
                new QueryWrapper<com.example.hrms.entity.SalaryStandardDetail>()
                        .eq("Standard_ID", master.getStandardId())
        );

        // 3. 获取所有项目名称映射 (ID -> Name)
        Map<Integer, String> itemMap = salaryItemMapper.selectList(null).stream()
                .collect(Collectors.toMap(
                        com.example.hrms.entity.SalaryItem::getItemId,
                        com.example.hrms.entity.SalaryItem::getItemName
                ));

        // 4. 组装前端友好的数据列表 (包含：项目名、金额)
        List<Map<String, Object>> displayList = new ArrayList<>();
        for (com.example.hrms.entity.SalaryStandardDetail detail : details) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", itemMap.getOrDefault(detail.getItemId(), "未知项目"));
            item.put("value", detail.getValue());
            displayList.add(item);
        }

        result.put("success", true);
        result.put("standardCode", master.getStandardCode()); // 标准编号
        result.put("data", displayList); // 具体明细

        return result;
    }
}