package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("T_Personnel_File")
public class PersonnelFile {
    @TableId(type = IdType.AUTO)
    private Integer fileId;
    private String name;
    private String gender;
    private String idNumber;
    private String phoneNumber;
    private String address;
    private Integer l3OrgId;
    private String auditStatus; // Pending, Approved
    private Integer isDeleted;
    private LocalDateTime creationTime;
}