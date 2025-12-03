package com.example.hrms.controller;

import com.example.hrms.mapper.SalaryRegisterMasterMapper;
import com.example.hrms.mapper.SalaryStandardMasterMapper;
import com.example.hrms.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage/audit")
public class ManagementAuditController {

    @Autowired
    private SalaryStandardMasterMapper standardMasterMapper;
    @Autowired
    private SalaryService salaryService;
    @Autowired
    private SalaryRegisterMasterMapper registerMasterMapper;
}
