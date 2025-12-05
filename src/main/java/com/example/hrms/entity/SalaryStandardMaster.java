package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data; // 假设你使用了Lombok，如果没有请手动加Getter/Setter
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("T_Salary_Standard_Master")
public class SalaryStandardMaster implements Serializable {

    @TableId(value = "Standard_ID", type = IdType.AUTO)
    private Integer standardId;

    // 【新增】标准编号
    @TableField("Standard_Code")
    private String standardCode;

    @TableField("Standard_Name")
    private String standardName;

    @TableField("L3_Org_ID")
    private Integer l3OrgId;

    @TableField("Position_ID")
    private Integer positionId;

    @TableField("Submitter_ID")
    private Integer submitterId;

    @TableField("Submission_Time")
    private LocalDateTime submissionTime;

    @TableField("Audit_Status")
    private String auditStatus;

    @TableField("Auditor_ID")
    private Integer auditorId;

    @TableField("Audit_Time")
    private LocalDateTime auditTime;
}