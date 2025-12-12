package com.example.hrms.dto;

import lombok.Data;

@Data
public class OrgDTO {
    private Integer orgId;
    private String orgName;
    private String l1OrgName;
    private String l2OrgName;
    private String l3OrgName;
    private String hrManagerName;
    private String salaryManagerName;
    private Integer employeeCount;

    public String getFormattedName() {
        if (l3OrgName != null) return String.format("%s / %s / %s", l1OrgName, l2OrgName, l3OrgName);
        if (l2OrgName != null) return String.format("%s / %s", l1OrgName, l2OrgName);
        return l1OrgName;
    }
}
