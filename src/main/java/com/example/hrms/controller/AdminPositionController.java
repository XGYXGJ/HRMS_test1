// src/main/java/com/example/hrms/controller/AdminPositionController.java
package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.Organization;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.OrganizationMapper;
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
@RequestMapping("/admin/positions") // 管理员的职位管理路径
public class AdminPositionController {

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    @Autowired
    private OrganizationService organizationService;

    /**
     * 管理员查看所有职位列表
     */
    @GetMapping
    public String listAllPositions(Model model) {
        // 1. 查询所有职位，排除“管理员”
        List<Position> positions = positionMapper.selectList(
            new QueryWrapper<Position>().ne("Position_Name", "管理员")
        );

        // 2. 批量获取机构名称
        Map<Integer, String> orgNameMap = positions.stream()
                .map(Position::getL3OrgId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> organizationService.getFullOrgName(id)));
        model.addAttribute("orgNameMap", orgNameMap);


        // 3. 批量查询所有职位的人数
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

    /**
     * 管理员查看某个职位下的所有员工
     */
    @GetMapping("/{id}/employees")
    public String viewPositionEmployees(@PathVariable("id") Integer positionId, Model model) {
        Position position = positionMapper.selectById(positionId);
        if (position == null) {
            return "redirect:/admin/positions?error=not_found";
        }

        // 直接查询该职位下的所有用户
        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .eq("Position_ID", positionId)
                .eq("Is_Deleted", 0));

        // 根据用户ID列表查询对应的档案信息
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
