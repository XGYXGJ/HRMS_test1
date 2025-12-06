// src/main/java/com/example/hrms/controller/OrgPositionApiController.java
package com.example.hrms.controller;

import com.example.hrms.entity.Position;
import com.example.hrms.mapper.PositionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrgPositionApiController {

    @Autowired
    private PositionMapper positionMapper;

    @GetMapping("/org/{orgId}/positions")
    public List<Position> getPositionsByOrg(@PathVariable Integer orgId) {
        // 直接复用之前在Mapper中写好的方法
        return positionMapper.selectPositionsByOrgId(orgId);
    }
}
