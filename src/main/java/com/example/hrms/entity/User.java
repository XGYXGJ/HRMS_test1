// src/main/java/com/example/hrms/entity/User.java (完整修正版)
package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("T_User")
public class User {
    @TableId(value = "User_ID", type = IdType.AUTO)
    private Integer userId;

    private String username;

    @TableField("Password_Hash")
    private String passwordHash;

    @TableField("Position_ID")
    private Integer positionId;

    @TableField("L3_Org_ID")
    private Integer l3OrgId;

    // --- 新增的字段 ---
    @TableField("Is_Deleted")
    private Boolean isDeleted;
}
