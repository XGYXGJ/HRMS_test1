package com.example.hrms.controller;

import com.example.hrms.entity.Organization;
import com.example.hrms.service.OrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/org")
public class OrgApiController {

    @Autowired
    private OrgService orgService;

    @GetMapping("/children/{parentId}")
    public List<Organization> getChildren(@PathVariable Integer parentId) {
        return orgService.getOrgsByParent(parentId);
    }
}