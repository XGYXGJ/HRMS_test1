package com.example.hrms.service;

import com.example.hrms.entity.Organization;
import com.example.hrms.mapper.OrganizationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class OrganizationService {

    @Autowired
    private OrganizationMapper organizationMapper;

    public String getFullOrgName(Integer orgId) {
        if (orgId == null) {
            return "";
        }

        List<String> names = new ArrayList<>();
        Organization current = organizationMapper.selectById(orgId);

        while (current != null) {
            names.add(current.getOrgName());
            if (current.getParentOrgId() == null || current.getParentOrgId() == 0) {
                break;
            }
            current = organizationMapper.selectById(current.getParentOrgId());
        }

        Collections.reverse(names);
        return String.join("/", names);
    }
}
