package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
// 3. 薪酬标准总表
@Data
@TableName("T_Salary_Standard_Master")
public class SalaryStandardMaster {
    @TableId(type = IdType.AUTO)
    private Integer standardId;
    private String standardName;
    private Integer l3OrgId;
    private Integer positionId;
    private Integer submitterId;
    private String auditStatus; // Pending, Approved, Rejected
    private LocalDateTime submissionTime;
}
