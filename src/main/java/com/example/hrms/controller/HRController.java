package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.PersonnelFileDTO;
import com.example.hrms.dto.UserDTO;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
import com.example.hrms.mapper.UserMapper;
import com.example.hrms.service.PersonnelService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/hr")
public class HRController {

    private static final Logger logger = LoggerFactory.getLogger(HRController.class);

    @Autowired
    private PersonnelService personnelService;
    @Autowired
    private PositionMapper positionMapper;
    @Autowired
    private PersonnelFileMapper personnelFileMapper;
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "hr/dashboard";
    }

    @GetMapping("/dashboard/home")
    public String dashboardHome() {
        return "hr/dashboard_home";
    }

    @GetMapping("/personnel/list")
    public String personnelListPage(@RequestParam(value = "q", required = false) String q,
                                    HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        Integer l3OrgId = user != null ? user.getL3OrgId() : null;
        logger.info("Fetching files for HR user. l3OrgId: {}", l3OrgId);

        model.addAttribute("files", personnelService.listFiles(l3OrgId, q));
        model.addAttribute("q", q);
        return "hr/personnel_list";
    }

    @GetMapping("/personnel/form")
    public String personnelNewPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", user.getL3OrgId())));
        return "hr/personnel_form";
    }

    @PostMapping("/personnel/new")
    public String createPersonnel(@ModelAttribute PersonnelFile file,
                                  @RequestParam Integer positionId,
                                  HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        file.setL3OrgId(currentUser.getL3OrgId());
        file.setAuditStatus("Pending");
        file.setHrSubmitterId(currentUser.getUserId());

        try {
            personnelService.createPersonnelAuto(file, positionId);
        } catch (Exception ex) {
            model.addAttribute("error", "创建失败: " + ex.getMessage());
            model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", currentUser.getL3OrgId())));
            return "hr/personnel_form";
        }
        return "redirect:/hr/personnel/list?msg=success_create";
    }

    @GetMapping("/personnel/view/{id}")
    public String personnelViewPage(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        PersonnelFileDTO file = personnelService.getFileById(id);
        if (file == null) {
            return "redirect:/hr/personnel/list?error=notfound";
        }
        if (!file.getL3OrgId().equals(user.getL3OrgId())) {
            return "redirect:/hr/personnel/list?error=access_denied";
        }
        model.addAttribute("file", file);
        return "hr/personnel_view";
    }

    @GetMapping("/personnel/edit/{id}")
    public String personnelEditPage(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        PersonnelFileDTO fileData = personnelService.getFileById(id);
        if (fileData == null) {
            return "redirect:/hr/personnel/list?error=notfound";
        }
        if (!fileData.getL3OrgId().equals(user.getL3OrgId())) {
            return "redirect:/hr/personnel/list?error=access_denied";
        }

        model.addAttribute("positions", positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", user.getL3OrgId())));

        if (fileData.getUserId() != null) {
            User empUser = userMapper.selectById(fileData.getUserId());
            model.addAttribute("currentPositionId", empUser.getPositionId());
        }

        model.addAttribute("file", fileData);
        return "hr/personnel_edit";
    }

    @PostMapping("/personnel/edit/{id}")
    public String updatePersonnel(@PathVariable Integer id,
                                  @RequestParam Map<String, String> allParams,
                                  @RequestParam(required = false) Integer positionId,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        PersonnelFileDTO fileData = personnelService.getFileById(id);
        if (!fileData.getL3OrgId().equals(user.getL3OrgId())) {
            return "redirect:/hr/personnel/list?error=access_denied";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", allParams.get("name"));
        payload.put("gender", allParams.get("gender"));
        payload.put("idNumber", allParams.get("idNumber"));
        payload.put("phoneNumber", allParams.get("phoneNumber"));
        payload.put("address", allParams.get("address"));
        personnelService.updatePersonnel(id, payload);

        PersonnelFile pf = personnelFileMapper.selectById(id);
        if (pf != null && pf.getUserId() != null && positionId != null) {
            User empUser = userMapper.selectById(pf.getUserId());
            if (empUser != null) {
                empUser.setPositionId(positionId);
                userMapper.updateById(empUser);
            }
        }
        return "redirect:/hr/personnel/view/" + id;
    }

    @PostMapping("/personnel/resubmit")
    public String resubmitPersonnel(@RequestParam Integer id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        PersonnelFileDTO fileDTO = personnelService.getFileById(id);
        
        if (fileDTO != null && fileDTO.getL3OrgId().equals(user.getL3OrgId())) {
            PersonnelFile file = personnelFileMapper.selectById(id);
            if (file != null) {
                file.setAuditStatus("Pending");
                file.setHrSubmitterId(user.getUserId());
                file.setCreationTime(LocalDateTime.now());
                personnelFileMapper.updateById(file);
            }
        } else {
             return "redirect:/hr/personnel/list?error=access_denied";
        }

        return "redirect:/hr/personnel/list";
    }

    @GetMapping("/attendance/management")
    public String attendanceManagement(HttpSession session, Model model) {
        User hrUser = (User) session.getAttribute("user");
        if (hrUser == null || hrUser.getL3OrgId() == null) {
            return "redirect:/login";
        }
        List<UserDTO> employees = userMapper.searchUsersByOrg(hrUser.getL3OrgId());
        model.addAttribute("employees", employees);
        return "hr/attendance_management";
    }
}