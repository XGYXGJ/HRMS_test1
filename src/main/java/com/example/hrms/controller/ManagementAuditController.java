package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.PersonnelFileDTO;
import com.example.hrms.entity.Organization;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.*;
import com.example.hrms.service.OrganizationService;
import com.example.hrms.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PositionMapper positionMapper;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationMapper organizationMapper;

    @GetMapping("/personnel/list")
    public String showPersonnelAuditList(Model model) {
        List<PersonnelFile> files = personnelFileMapper.findByAuditStatus("Pending");
        List<Integer> submitterIds = files.stream()
                .map(PersonnelFile::getHrSubmitterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> submitterMap = Collections.emptyMap();
        if (!submitterIds.isEmpty()) {
            submitterMap = userMapper.selectBatchIds(submitterIds).stream()
                    .collect(Collectors.toMap(User::getUserId, user -> user));
        }

        Map<Integer, String> orgNameMap = files.stream()
                .map(PersonnelFile::getL3OrgId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> organizationService.getFullOrgName(id)));

        model.addAttribute("files", files);
        model.addAttribute("submitterMap", submitterMap);
        model.addAttribute("orgNameMap", orgNameMap);
        return "manage/audit_list_personnel";
    }

    @GetMapping("/personnel/history")
    public String showPersonnelAuditHistory(@RequestParam(value = "q", required = false) String q, Model model) {
        List<PersonnelFileDTO> files = personnelFileMapper.selectAuditHistory(q);
        model.addAttribute("files", files);
        model.addAttribute("q", q);
        return "manage/audit_history_personnel";
    }

    @GetMapping("/detail/personnel/{id}")
    public String showPersonnelAuditDetail(@PathVariable Integer id, Model model) {
        PersonnelFile file = personnelFileMapper.selectById(id);
        User user = userMapper.selectById(file.getUserId());
        Position position = positionMapper.selectById(user.getPositionId());

        model.addAttribute("file", file);
        model.addAttribute("position", position);
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
