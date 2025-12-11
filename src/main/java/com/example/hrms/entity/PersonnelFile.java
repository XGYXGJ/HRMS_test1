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

    @TableField("Archive_No")
    private String archiveNo;

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

    private Integer isDeleted;
    private LocalDateTime creationTime;
}
