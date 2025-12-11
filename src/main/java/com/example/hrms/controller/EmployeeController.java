package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.*;
import com.example.hrms.mapper.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/emp")
public class EmployeeController {

    @Autowired private PersonnelFileMapper personnelFileMapper;
    @Autowired private PositionMapper positionMapper;
    @Autowired private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired private SalaryRegisterDetailMapper SalaryRegisterDetailMapper;
    @Autowired private SalaryRegisterMasterMapper salaryRegisterMasterMapper;

    @PostMapping("/punch-in")
    public String punchIn(HttpSession session, RedirectAttributes attrs) {
        User user = (User) session.getAttribute("user");
        LocalDate today = LocalDate.now();

        // 1. 检查今日是否已打卡
        Long count = attendanceRecordMapper.selectCount(
                new QueryWrapper<AttendanceRecord>()
                        .eq("User_ID", user.getUserId())
                        .eq("Punch_Date", today) // Corrected column name
        );

        if (count > 0) {
            attrs.addFlashAttribute("error", "您今天已经打过卡了，无需重复打卡。");
        } else {
            AttendanceRecord record = new AttendanceRecord();
            record.setUserId(user.getUserId());
            record.setAttendanceDate(today);
            record.setPunchInTime(LocalDateTime.now());
            attendanceRecordMapper.insert(record);
            attrs.addFlashAttribute("msg", "打卡成功！时间：" + LocalDateTime.now());
        }
        return "redirect:/emp/home";
    }
    @GetMapping("/home")
    public String home(HttpSession session,
                       Model model,
                       @RequestParam(value = "month", required = false) String month) {
        // 1. 获取当前登录用户
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // 2. 个人档案信息
        PersonnelFile profile = personnelFileMapper.selectOne(
                new QueryWrapper<PersonnelFile>()
                        .eq("User_ID", user.getUserId())
        );
        model.addAttribute("profile", profile);

        // 3. 职位信息
        Position position = null;
        if (user.getPositionId() != null) {
            position = positionMapper.selectById(user.getPositionId());
        }
        model.addAttribute("position", position);

        // 4. 我的工资单列表：只查“自己 + 已批准(Approved)”的工资明细，
        //    如果带 month 参数，则按月份筛选
        List<SalaryRegisterDetail> salaryList;
        if (month != null && !month.isEmpty()) {
            // month 形如 "2025-12"
            YearMonth ym = YearMonth.parse(month);
            LocalDate startDate = ym.atDay(1);               // 当月第一天
            LocalDate endDate = ym.plusMonths(1).atDay(1);   // 下月第一天，用于 < 结束日期

            salaryList = SalaryRegisterDetailMapper
                    .selectApprovedDetailsByUserAndMonth(
                            user.getUserId(), startDate, endDate);
        } else {
            // 未指定月份，则查全部已发放记录
            salaryList = SalaryRegisterDetailMapper
                    .selectApprovedDetailsByUser(user.getUserId());
        }
        model.addAttribute("salaryList", salaryList);
        model.addAttribute("selectedMonth", month); // 回显到页面的 input 里

        // 4.1 为前端准备：Register_ID -> Register_Code 的映射，用来显示“工资单号”
        Map<Integer, String> registerCodeMap = new HashMap<>();
        if (salaryList != null && !salaryList.isEmpty()) {
            List<Integer> registerIds = salaryList.stream()
                    .map(SalaryRegisterDetail::getRegisterId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!registerIds.isEmpty()) {
                // 这里用的是上面 @Autowired 的字段：salaryRegisterMasterMapper
                List<SalaryRegisterMaster> masters =
                        salaryRegisterMasterMapper.selectBatchIds(registerIds);

                registerCodeMap = masters.stream()
                        .collect(Collectors.toMap(
                                SalaryRegisterMaster::getRegisterId,
                                SalaryRegisterMaster::getRegisterCode
                        ));
            }
        }
        model.addAttribute("registerCodeMap", registerCodeMap);


        // 5. 今日是否已经打卡
        LocalDate today = LocalDate.now();
        Long count = attendanceRecordMapper.selectCount(
                new QueryWrapper<AttendanceRecord>()
                        .eq("User_ID", user.getUserId())
                        .eq("Punch_Date", today)
        );
        model.addAttribute("hasPunchedIn", count != null && count > 0);
        model.addAttribute("currentDate", today);

        return "emp/home";
    }

}
