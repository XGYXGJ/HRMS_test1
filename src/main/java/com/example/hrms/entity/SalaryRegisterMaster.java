package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
// 5. 薪酬发放总表
@Data
@TableName("T_Salary_Register_Master")
public class SalaryRegisterMaster {
    @TableId(type = IdType.AUTO)
    private Integer registerId;
    private Integer l3OrgId;
    private BigDecimal totalAmount;
    private Integer totalPeople;
    private String auditStatus;
    private LocalDate payDate;
}