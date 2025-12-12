package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
import com.example.hrms.mapper.UserMapper;
import com.example.hrms.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/position")
public class AdminPositionController {

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    @Autowired
    private OrganizationService organizationService;

    @GetMapping("/list")
    public String listAllPositions(Model model) {
        List<Position> positions = positionMapper.selectList(
            new QueryWrapper<Position>().ne("Position_Name", "管理员")
        );

        Map<Integer, String> orgNameMap = positions.stream()
                .map(Position::getL3OrgId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> organizationService.getFullOrgName(id)));
        model.addAttribute("orgNameMap", orgNameMap);

        if (!positions.isEmpty()) {
            List<Integer> positionIds = positions.stream().map(Position::getPositionId).collect(Collectors.toList());
            List<Map<String, Object>> counts = positionMapper.countEmployeesByPositionIds(positionIds);
            Map<Integer, Long> countMap = counts.stream()
                    .collect(Collectors.toMap(
                            map -> (Integer) map.get("positionId"),
                            map -> (Long) map.get("employeeCount")
                    ));
            model.addAttribute("employeeCounts", countMap);
        }

        model.addAttribute("positions", positions);
        return "admin/position_list";
    }

    @GetMapping("/{id}/employees")
    public String viewPositionEmployees(@PathVariable("id") Integer positionId, Model model) {
        Position position = positionMapper.selectById(positionId);
        if (position == null) {
            return "redirect:/admin/position/list?error=not_found";
        }

        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .eq("Position_ID", positionId)
                .eq("Is_Deleted", 0));

        List<PersonnelFile> employeeFiles = new ArrayList<>();
        if (!users.isEmpty()) {
            List<Integer> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
            employeeFiles = personnelFileMapper.selectList(new QueryWrapper<PersonnelFile>().in("User_ID", userIds));
        }

        model.addAttribute("position", position);
        model.addAttribute("employeeFiles", employeeFiles);
        return "admin/position_employees";
    }
}