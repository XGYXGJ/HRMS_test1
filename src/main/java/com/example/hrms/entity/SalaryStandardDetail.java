package com.example.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
// 4. 薪酬标准详情
@Data
@TableName("T_Salary_Standard_Detail")
public class SalaryStandardDetail {
    @TableId(type = IdType.AUTO)
    private Integer detailId;
    private Integer standardId;
    private Integer itemId;
    private BigDecimal value;
}