package com.example.hrms.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Integer userId;
    private String name;
    private String orgName;
    private String positionName;
    private Integer l3OrgId;
    private Integer positionId;
    private String archiveNo;
    private String position;

    // 用于在前端下拉列表中显示 "姓名 (机构 - 岗位)"
    public String getFullNameWithDetails() {
        return name + " (" + orgName + " - " + positionName + ")";
    }
}
