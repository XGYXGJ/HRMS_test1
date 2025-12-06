package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
// 6. 薪酬发放详情
@Data
@TableName("T_Salary_Register_Detail")
public class SalaryRegisterDetail {
    @TableId(type = IdType.AUTO)
    private Integer detailId;
    private Integer registerId;
    private Integer userId;
    private BigDecimal baseSalary;
    private BigDecimal kpiBonus;
    private BigDecimal grossMoney;
    private LocalDate payrollMonth;

    public LocalDate getPayrollMonth() { return payrollMonth; }
    public void setPayrollMonth(LocalDate payrollMonth) { this.payrollMonth = payrollMonth; }
}