package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.AttendanceRecord;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.AttendanceRecordMapper;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
import com.example.hrms.mapper.SalaryRegisterDetailMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/emp")
public class EmployeeController {

    @Autowired private SalaryRegisterDetailMapper registerDetailMapper;
    @Autowired private PersonnelFileMapper personnelFileMapper;
    @Autowired private PositionMapper positionMapper;
    @Autowired private AttendanceRecordMapper attendanceRecordMapper;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        LocalDate today = LocalDate.now();

        // 1. 查询个人档案信息
        PersonnelFile profile = personnelFileMapper.selectOne(
                new QueryWrapper<PersonnelFile>().eq("User_ID", user.getUserId())
        );
        model.addAttribute("profile", profile);

        // 2. 查询职位信息
        if (user.getPositionId() != null) {
            Position position = positionMapper.selectById(user.getPositionId());
            model.addAttribute("position", position);
        }
        
        // 3. 查询今日是否已打卡
        Long count = attendanceRecordMapper.selectCount(
                new QueryWrapper<AttendanceRecord>()
                        .eq("User_ID", user.getUserId())
                        .eq("Punch_Date", today) // Corrected column name
        );
        boolean hasPunchedIn = count > 0;
        model.addAttribute("hasPunchedIn", hasPunchedIn);
        model.addAttribute("currentDate", today);

        return "emp/home";
    }

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
            // record.setPunchInType("Normal"); // Removed as the field does not exist in DB
            attendanceRecordMapper.insert(record);
            attrs.addFlashAttribute("msg", "打卡成功！时间：" + LocalDateTime.now());
        }
        return "redirect:/emp/home";
    }
}
