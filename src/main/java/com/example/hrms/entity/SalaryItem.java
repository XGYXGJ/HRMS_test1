package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// 2. 薪酬项目
@Data
@TableName("T_Salary_Item")
public class SalaryItem {
    @TableId(type = IdType.AUTO)
    private Integer itemId;
    private String itemName;
    private String itemType; // Base, Bonus, Subsidy
}