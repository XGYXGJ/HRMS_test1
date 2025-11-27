package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

// 1. 用户表
@Data
@TableName("T_User")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer userId;
    private String username;
    private String passwordHash; // 实验简化，明文或简单Hash
    private Integer positionId;
    private Integer l3OrgId;
}