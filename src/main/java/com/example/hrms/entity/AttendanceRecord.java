package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("T_Attendance_Record")
public class AttendanceRecord {

    @TableId(value = "Record_ID", type = IdType.AUTO)
    private Integer recordId;

    @TableField("User_ID")
    private Integer userId;

    @TableField("Punch_Date")
    private LocalDate attendanceDate; // 映射到 Punch_Date

    @TableField("Punch_Time")
    private LocalDateTime punchInTime; // 映射到 Punch_Time

    /**
     * 打卡类型，例如：Normal, Proxy
     * 注意：数据库中似乎没有这个字段，如果需要存储打卡类型，请在数据库中添加该列，
     * 或者在此处使用 @TableField(exist = false) 忽略它。
     * 暂时假设数据库中没有这个字段，将其标记为非数据库字段，以免报错。
     */
    @TableField(exist = false)
    private String punchInType;
}
