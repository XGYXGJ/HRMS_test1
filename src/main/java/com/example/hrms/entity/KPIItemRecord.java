package com.example.hrms.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("T_KPI_Item_Record")
public class KPIItemRecord {
    @TableId(value = "Record_ID", type = IdType.AUTO)
    private Integer recordId;
    private Integer registerDetailId; // 关联到具体的工资明细行
    private String itemName;
    private BigDecimal weight;
    private BigDecimal score;
}