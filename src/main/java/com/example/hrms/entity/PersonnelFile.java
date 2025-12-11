package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("T_Personnel_File")
public class PersonnelFile {
    @TableId(type = IdType.AUTO)
    private Integer fileId;

    // 新增档案号字段（Archive_No）
    @TableField("Archive_No")
    private String archiveNo;

    // 关联系统用户（可为空）
    @TableField("User_ID")
    private Integer userId;

    private String name;
    private String gender;
    private String idNumber;
    private String phoneNumber;
    private String address;

    @TableField("L3_Org_ID")
    private Integer l3OrgId;

    @TableField("HR_Submitter_ID")
    private Integer hrSubmitterId;

    private String auditStatus; // Pending, Approved, Rejected

    @TableField("Auditor_ID")
    private Integer auditorId; // 审核人ID

    @TableField("Audit_Time")
    private LocalDateTime auditTime; // 审核时间
    
    @TableField("submission_time")
    private LocalDateTime submissionTime; // 提交时间

    private Integer isDeleted;
    private LocalDateTime creationTime;
}
