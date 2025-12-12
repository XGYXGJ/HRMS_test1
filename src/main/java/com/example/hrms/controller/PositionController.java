package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.PositionMapper;
import com.example.hrms.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hr/position")
public class PositionController {

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    @GetMapping("/list")
    public String listPositions(HttpSession session, Model model) {
        // Add newPosition to the model at the beginning to prevent template errors
        model.addAttribute("newPosition", new Position());

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || currentUser.getL3OrgId() == null) {
            model.addAttribute("error", "无法获取您的机构信息，或您不属于任何一个三级机构。");
            // Still provide an empty list for positions to ensure the page renders correctly
            model.addAttribute("positions", new ArrayList<Position>());
            model.addAttribute("employeeCounts", new java.util.HashMap<Integer, Long>());
            return "hr/position_list";
        }

        List<Position> positions = positionMapper.selectList(
                new QueryWrapper<Position>().eq("L3_Org_ID", currentUser.getL3OrgId())
        );

        if (!positions.isEmpty()) {
            List<Integer> positionIds = positions.stream().map(Position::getPositionId).collect(Collectors.toList());
            List<Map<String, Object>> counts = positionMapper.countEmployeesByPositionIds(positionIds);

            Map<Integer, Long> countMap = counts.stream()
                    .collect(Collectors.toMap(
                            map -> (Integer) map.get("positionId"),
                            map -> (Long) map.get("employeeCount")
                    ));
            model.addAttribute("employeeCounts", countMap);
        } else {
            // Ensure employeeCounts is not null even if there are no positions
            model.addAttribute("employeeCounts", new java.util.HashMap<Integer, Long>());
        }

        model.addAttribute("positions", positions);
        return "hr/position_list";
    }

    @GetMapping("/{id}/employees")
    public String viewPositionEmployees(@PathVariable("id") Integer positionId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        Position position = positionMapper.selectById(positionId);

        if (position == null || !position.getL3OrgId().equals(currentUser.getL3OrgId())) {
            return "redirect:/hr/position/list?error=access_denied";
        }

        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .eq("Position_ID", positionId)
                .eq("L3_Org_ID", currentUser.getL3OrgId())
                .eq("Is_Deleted", 0));

        List<PersonnelFile> employeeFiles = new ArrayList<>();
        if (!users.isEmpty()) {
            List<Integer> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
            employeeFiles = personnelFileMapper.selectList(new QueryWrapper<PersonnelFile>().in("User_ID", userIds));
        }

        model.addAttribute("position", position);
        model.addAttribute("employeeFiles", employeeFiles);
        return "hr/position_employees";
    }

    @PostMapping("/save")
    public String savePosition(@ModelAttribute Position newPosition, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || currentUser.getL3OrgId() == null) {
            model.addAttribute("error", "非法操作，无法获取您的机构信息");
            return "redirect:/hr/position/list";
        }
        newPosition.setL3OrgId(currentUser.getL3OrgId());
        newPosition.setAuthLevel("Employee");
        positionMapper.insert(newPosition);
        return "redirect:/hr/position/list?success";
    }

    @GetMapping("/delete/{id}")
    public String deletePosition(@PathVariable Integer id, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        Position position = positionMapper.selectById(id);

        if (position != null && position.getL3OrgId().equals(currentUser.getL3OrgId())) {
            // Check if any user is assigned to this position
            long userCount = userMapper.selectCount(new QueryWrapper<User>().eq("Position_ID", id).eq("Is_Deleted", 0));
            if (userCount > 0) {
                model.addAttribute("error", "无法删除，该职位下还有 " + userCount + " 名员工。请先调整员工职位。");
            } else {
                positionMapper.deleteById(id);
                model.addAttribute("success", "职位删除成功！");
            }
        } else {
             model.addAttribute("error", "权限不足或职位不存在！");
        }

        // Instead of redirecting, call the list method to return the updated fragment
        return listPositions(session, model);
    }

    @GetMapping("/change")
    public String showPositionChangePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        List<String> excludedPositions = Arrays.asList("人事经理", "薪酬经理");
        List<Integer> excludedPositionIds = positionMapper.selectList(
                new QueryWrapper<Position>().in("Position_Name", excludedPositions)
        ).stream().map(Position::getPositionId).collect(Collectors.toList());

        QueryWrapper<User> userQuery = new QueryWrapper<User>()
                .eq("L3_Org_ID", user.getL3OrgId());
        if (!excludedPositionIds.isEmpty()) {
            userQuery.notIn("Position_ID", excludedPositionIds);
        }
        List<User> users = userMapper.selectList(userQuery);

        List<Integer> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
        List<PersonnelFile> employees = new ArrayList<>();
        if(!userIds.isEmpty()){
            employees = personnelFileMapper.selectList(new QueryWrapper<PersonnelFile>().in("User_ID", userIds));
        }

        List<Position> positions = positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", user.getL3OrgId()));

        model.addAttribute("employees", employees);
        model.addAttribute("positions", positions);
        return "hr/position_change";
    }

    @PostMapping("/change")
    public String changePosition(@RequestParam Integer userId, @RequestParam Integer positionId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        User employee = userMapper.selectById(userId);

        if (employee == null || !employee.getL3OrgId().equals(user.getL3OrgId())) {
            model.addAttribute("error", "无法操作非本机构的员工");
        } else {
            employee.setPositionId(positionId);
            userMapper.updateById(employee);
            model.addAttribute("success", "职位调整成功");
        }

        // reload data for the page
        List<String> excludedPositions = Arrays.asList("人事经理", "薪酬经理");
        List<Integer> excludedPositionIds = positionMapper.selectList(
                new QueryWrapper<Position>().in("Position_Name", excludedPositions)
        ).stream().map(Position::getPositionId).collect(Collectors.toList());

        QueryWrapper<User> userQuery = new QueryWrapper<User>()
                .eq("L3_Org_ID", user.getL3OrgId());
        if (!excludedPositionIds.isEmpty()) {
            userQuery.notIn("Position_ID", excludedPositionIds);
        }
        List<User> users = userMapper.selectList(userQuery);

        List<Integer> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
        List<PersonnelFile> employees = new ArrayList<>();
        if(!userIds.isEmpty()){
            employees = personnelFileMapper.selectList(new QueryWrapper<PersonnelFile>().in("User_ID", userIds));
        }
        List<Position> positions = positionMapper.selectList(new QueryWrapper<Position>().eq("L3_Org_ID", user.getL3OrgId()));

        model.addAttribute("employees", employees);
        model.addAttribute("positions", positions);

        return "hr/position_change";
    }
}