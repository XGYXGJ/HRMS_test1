package com.example.hrms.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("T_Attendance_Record")
public class AttendanceRecord {
    @TableId(value = "Record_ID", type = IdType.AUTO)
    private Integer recordId;
    private Integer userId;
    private LocalDateTime punchTime;
    private LocalDate punchDate;
}