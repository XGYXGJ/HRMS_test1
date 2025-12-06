package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 5. 薪酬发放总表
@Data
@TableName("T_Salary_Register_Master")
public class SalaryRegisterMaster {

    @TableId(value = "Register_ID", type = IdType.AUTO)
    private Integer registerId;

    @TableField("Register_Code")
    private String registerCode;           // 薪资发放单号

    @TableField("L3_Org_ID")
    private Integer l3OrgId;

    @TableField("Register_Time")
    private LocalDateTime registerTime;    // 登记/提交时间（原模板 creationTime）

    @TableField("Submitter_ID")
    private Integer submitterId;           // HR 提交人ID

    @TableField("Total_People")
    private Integer totalPeople;

    @TableField("Total_Amount")
    private BigDecimal totalAmount;

    @TableField("Audit_Status")
    private String auditStatus;

    @TableField("Auditor_ID")
    private Integer auditorId;

    @TableField("Audit_Time")
    private LocalDateTime auditTime;

    @TableField("Pay_Date")
    private LocalDate payDate;             // 薪酬月份/发放月份
}
