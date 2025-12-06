package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("T_Salary_Register_Detail")
public class SalaryRegisterDetail {

    @TableId(value = "Detail_ID", type = IdType.AUTO)
    private Integer detailId;

    private Integer registerId;
    private Integer userId;
    private Integer standardIdUsed;

    private BigDecimal kpiUnits;
    private Integer attendanceCount;
    private BigDecimal overtimeHours;

    private BigDecimal baseSalary;
    private BigDecimal totalSubsidy;
    private BigDecimal kpiBonus;
    private BigDecimal attendanceAdjustment;
    private BigDecimal overtimePay;
    private BigDecimal grossMoney;

    private LocalDate payrollMonth;

    // ===== getter/setter 省略的话自己IDE生成 =====

    public Integer getDetailId() { return detailId; }
    public void setDetailId(Integer detailId) { this.detailId = detailId; }

    public Integer getRegisterId() { return registerId; }
    public void setRegisterId(Integer registerId) { this.registerId = registerId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getStandardIdUsed() { return standardIdUsed; }
    public void setStandardIdUsed(Integer standardIdUsed) { this.standardIdUsed = standardIdUsed; }

    public BigDecimal getKpiUnits() { return kpiUnits; }
    public void setKpiUnits(BigDecimal kpiUnits) { this.kpiUnits = kpiUnits; }

    public Integer getAttendanceCount() { return attendanceCount; }
    public void setAttendanceCount(Integer attendanceCount) { this.attendanceCount = attendanceCount; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public BigDecimal getTotalSubsidy() { return totalSubsidy; }
    public void setTotalSubsidy(BigDecimal totalSubsidy) { this.totalSubsidy = totalSubsidy; }

    public BigDecimal getKpiBonus() { return kpiBonus; }
    public void setKpiBonus(BigDecimal kpiBonus) { this.kpiBonus = kpiBonus; }

    public BigDecimal getAttendanceAdjustment() { return attendanceAdjustment; }
    public void setAttendanceAdjustment(BigDecimal attendanceAdjustment) { this.attendanceAdjustment = attendanceAdjustment; }

    public BigDecimal getOvertimePay() { return overtimePay; }
    public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }

    public BigDecimal getGrossMoney() { return grossMoney; }
    public void setGrossMoney(BigDecimal grossMoney) { this.grossMoney = grossMoney; }

    public LocalDate getPayrollMonth() { return payrollMonth; }
    public void setPayrollMonth(LocalDate payrollMonth) { this.payrollMonth = payrollMonth; }
}
