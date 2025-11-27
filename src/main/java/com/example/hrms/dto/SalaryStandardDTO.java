package com.example.hrms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

// 用于接收前端提交的薪酬标准
@Data
public class SalaryStandardDTO {
    private String standardName;
    private Integer positionId;
    // Map<ItemId, Value>，例如 {1: 5000.00, 2: 200.00}
    private Map<Integer, BigDecimal> items;
}