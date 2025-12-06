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

    private String auditStatus; // Pending, Approved, Rejected
    private Integer isDeleted;
    private LocalDateTime creationTime;
}