package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.AttendanceRecord;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.SalaryItem;
import com.example.hrms.mapper.SalaryItemMapper;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.SalaryRegisterDetail;
import com.example.hrms.entity.SalaryRegisterMaster;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.AttendanceRecordMapper;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
import com.example.hrms.mapper.SalaryRegisterDetailMapper;
import com.example.hrms.mapper.SalaryRegisterMasterMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        return "emp/emp_dashboard";
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
}

