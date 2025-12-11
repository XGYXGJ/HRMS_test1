package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.AttendanceRecord;
import com.example.hrms.mapper.AttendanceRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hr/api")
public class HRApiController {

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @GetMapping("/attendance/{userId}")
    public List<AttendanceRecord> getAttendanceRecords(@PathVariable Integer userId) {
        return attendanceRecordMapper.selectList(
                new QueryWrapper<AttendanceRecord>().eq("User_ID", userId)
        );
    }

    @PostMapping("/attendance/proxy")
    public Map<String, Object> proxyPunch(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer userId = (Integer) payload.get("userId");
            LocalDate date = LocalDate.parse(payload.get("date").toString());

            // Check if a record already exists
            AttendanceRecord existingRecord = attendanceRecordMapper.selectOne(
                    new QueryWrapper<AttendanceRecord>()
                            .eq("User_ID", userId)
                            .eq("Punch_Date", date) // Corrected column name
            );

            if (existingRecord != null) {
                // Record exists, so delete it (cancel punch)
                attendanceRecordMapper.deleteById(existingRecord.getRecordId());
                response.put("success", true);
                response.put("message", "已取消打卡");
            } else {
                // Record does not exist, so create it (proxy punch)
                AttendanceRecord newRecord = new AttendanceRecord();
                newRecord.setUserId(userId);
                newRecord.setAttendanceDate(date);
                newRecord.setPunchInTime(date.atStartOfDay()); // Or use current time
                // newRecord.setPunchInType("Proxy"); // Removed as the field does not exist in DB
                attendanceRecordMapper.insert(newRecord);
                response.put("success", true);
                response.put("message", "代打卡成功");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
