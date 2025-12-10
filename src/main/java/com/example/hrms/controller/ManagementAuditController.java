package com.example.hrms.controller;

import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.SalaryRegisterMasterMapper;
import com.example.hrms.mapper.SalaryStandardMasterMapper;
import com.example.hrms.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/manage/audit")
public class ManagementAuditController {

    @Autowired
    private SalaryStandardMasterMapper standardMasterMapper;
    @Autowired
    private SalaryService salaryService;
    @Autowired
    private SalaryRegisterMasterMapper registerMasterMapper;
    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    @GetMapping("/personnel/list")
    public String showPersonnelAuditList(Model model) {
        List<PersonnelFile> files = personnelFileMapper.findByAuditStatus("Pending");
        model.addAttribute("files", files);
        return "manage/audit_list_personnel";
    }

    @GetMapping("/detail/personnel/{id}")
    public String showPersonnelAuditDetail(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        model.addAttribute("file", file);
        return "manage/audit_detail_personnel";
    }

    @PostMapping("/process/personnel")
    @ResponseBody
    public ResponseEntity<Void> processPersonnelAudit(@RequestParam Integer id, @RequestParam String action) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        if (file != null) {
            if ("pass".equals(action)) {
                file.setAuditStatus("Approved");
            } else if ("reject".equals(action)) {
                file.setAuditStatus("Rejected");
            }
            personnelFileMapper.updateById(file);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}