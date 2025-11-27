package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("T_Organization")
public class Organization {
    @TableId(type = IdType.AUTO)
    private Integer orgId;
    private String orgName;
    private Integer level;
    private Integer parentOrgId;
    private String l1OrgName;
    private String l2OrgName;
    private String l3OrgName;
}