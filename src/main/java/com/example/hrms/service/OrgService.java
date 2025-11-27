package com.example.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.Organization;
import com.example.hrms.mapper.OrganizationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrgService {

    @Autowired
    private OrganizationMapper orgMapper;

    // 获取所有一级机构
    public List<Organization> getLevel1Orgs() {
        return orgMapper.selectList(new QueryWrapper<Organization>().eq("Level", 1));
    }

    // 根据父ID获取下级机构
    public List<Organization> getOrgsByParent(Integer parentId) {
        return orgMapper.selectList(new QueryWrapper<Organization>().eq("Parent_Org_ID", parentId));
    }

    // 添加机构
    @Transactional
    public void addOrg(Organization org) {
        // 根据Level处理层级名称逻辑 (简化版)
        if (org.getLevel() == 1) {
            org.setL1OrgName(org.getOrgName());
        } else if (org.getLevel() == 2) {
            Organization parent = orgMapper.selectById(org.getParentOrgId());
            org.setL1OrgName(parent.getL1OrgName());
            org.setL2OrgName(org.getOrgName());
        } else {
            Organization parent = orgMapper.selectById(org.getParentOrgId());
            org.setL1OrgName(parent.getL1OrgName());
            org.setL2OrgName(parent.getL2OrgName());
            org.setL3OrgName(org.getOrgName());
        }
        orgMapper.insert(org);
    }
}