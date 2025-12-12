package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.UserDTO;
import com.example.hrms.entity.AttendanceRecord;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.AttendanceMapper;
import com.example.hrms.mapper.PositionMapper;
import com.example.hrms.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
public class HRApiController {

    private static final Logger logger = LoggerFactory.getLogger(HRApiController.class);

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private UserMapper userMapper;

    private boolean isHr(User user) {
        if (user == null) {
            logger.warn("isHr check failed: User is null");
            return false;
        }
        if (user.getPositionId() == null) {
            logger.warn("isHr check failed: User positionId is null. UserID: {}", user.getUserId());
            return false;
        }
        Position position = positionMapper.selectById(user.getPositionId());
        if (position == null) {
            logger.warn("isHr check failed: Position not found for ID: {}", user.getPositionId());
            return false;
        }
        
        logger.info("isHr check: UserID={}, PositionID={}, AuthLevel={}", user.getUserId(), user.getPositionId(), position.getAuthLevel());
        
        // Check for "HR" or "Manager" or if the position name contains "人事经理" as a fallback
        boolean isAuthLevelHr = "HR".equalsIgnoreCase(position.getAuthLevel()) || "HR_Manager".equalsIgnoreCase(position.getAuthLevel());
        boolean isPositionNameHr = position.getPositionName() != null && position.getPositionName().contains("人事经理");
        
        return isAuthLevelHr || isPositionNameHr;
    }

    @GetMapping("/attendance/{userId}")
    public ResponseEntity<List<AttendanceRecord>> getAttendanceRecords(@PathVariable Integer userId, HttpSession session) {
        User hrUser = (User) session.getAttribute("user");
        if (!isHr(hrUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        }
        List<AttendanceRecord> records = attendanceMapper.selectList(new QueryWrapper<AttendanceRecord>().eq("User_ID", userId));
        return ResponseEntity.ok(records);
    }

    @PostMapping("/attendance/proxy")
    public ResponseEntity<?> proxyPunch(@RequestBody Map<String, Object> payload, HttpSession session) {
        User hrUser = (User) session.getAttribute("user");
        if (!isHr(hrUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "无权限操作"));
        }

        Integer userId = (Integer) payload.get("userId");
        String dateStr = (String) payload.get("date");
        LocalDate date = LocalDate.parse(dateStr);

        // Corrected column name from "attendance_date" to "Punch_Date"
        AttendanceRecord existingRecord = attendanceMapper.selectOne(new QueryWrapper<AttendanceRecord>()
                .eq("User_ID", userId)
                .eq("Punch_Date", date));

        if (existingRecord != null) {
            attendanceMapper.deleteById(existingRecord.getRecordId());
        } else {
            AttendanceRecord newRecord = new AttendanceRecord();
            newRecord.setUserId(userId);
            newRecord.setAttendanceDate(date);
            attendanceMapper.insert(newRecord);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<UserDTO>> getEmployees(@RequestParam(required = false) String q, HttpSession session) {
        User hrUser = (User) session.getAttribute("user");
        if (!isHr(hrUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        }
        List<UserDTO> employees = userMapper.searchUsersByOrgAndQuery(hrUser.getL3OrgId(), q);
        return ResponseEntity.ok(employees);
    }
}