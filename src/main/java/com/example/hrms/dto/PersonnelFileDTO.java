package com.example.hrms.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PersonnelFileDTO {
    private Integer fileId;
    private String archiveNo;
    private String name;
    private String gender;
    private String idNumber;
    private String phoneNumber;
    private String address;
    private String auditStatus;
    private Integer l3OrgId;
    private String l1OrgName;
    private String l2OrgName;
    private String l3OrgName;
    private String positionName;
    private Integer userId;
    private String submitterName;
    private LocalDateTime creationTime;
    
    // 当前机构信息
    private String currentL1OrgName;
    private String currentL2OrgName;
    private String currentL3OrgName;

    public String getFormattedOrgName() {
        if (l3OrgName != null) return String.format("%s / %s / %s", l1OrgName, l2OrgName, l3OrgName);
        if (l2OrgName != null) return String.format("%s / %s", l1OrgName, l2OrgName);
        return l1OrgName != null ? l1OrgName : "-";
    }

    public String getFormattedCurrentOrgName() {
        if (currentL3OrgName != null) return String.format("%s / %s / %s", currentL1OrgName, currentL2OrgName, currentL3OrgName);
        if (currentL2OrgName != null) return String.format("%s / %s", currentL1OrgName, currentL2OrgName);
        return currentL1OrgName != null ? currentL1OrgName : "-";
    }
}
