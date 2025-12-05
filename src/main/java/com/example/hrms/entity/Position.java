package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("T_Position")
public class Position implements Serializable {

    @TableId(value = "Position_ID", type = IdType.AUTO)
    private Integer positionId;

    @TableField("Position_Name")
    private String positionName;

    @TableField("Auth_Level")
    private String authLevel;
}