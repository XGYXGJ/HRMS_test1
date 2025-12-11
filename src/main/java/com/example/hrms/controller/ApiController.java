package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.Organization;
import com.example.hrms.entity.Position;
import com.example.hrms.mapper.OrganizationMapper;
import com.example.hrms.mapper.PositionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private PositionMapper positionMapper;

    @GetMapping("/orgs/{id}/children")
    public List<Organization> getChildOrgs(@PathVariable Integer id) {
        return organizationMapper.selectList(new QueryWrapper<Organization>().eq("parent_id", id));
    }

    @GetMapping("/orgs/{id}/positions")
    public List<Position> getPositionsInOrg(@PathVariable Integer id) {
        return positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", id));
    }

    @GetMapping("/orgs/level3")
    public List<Organization> getLevel3Orgs() {
        return organizationMapper.selectList(new QueryWrapper<Organization>().eq("level", 3));
    }
}
