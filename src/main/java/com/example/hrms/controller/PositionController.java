// src/main/java/com/example/hrms/controller/PositionController.java

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hr/positions") // 路径归属到人事经理下
public class PositionController {

    @Autowired
    private PositionMapper positionMapper;

    @Autowired // 新增注入
    private UserMapper userMapper;

    @Autowired // 新增注入
    private PersonnelFileMapper personnelFileMapper;

    /**
     * 职位列表页面 (已增强)
     */
    @GetMapping
    public String listPositions(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || currentUser.getL3OrgId() == null) {
            model.addAttribute("error", "无法获取您的机构信息");
            return "hr/position_list";
        }

        List<Position> positions = positionMapper.selectList(
                new QueryWrapper<Position>().eq("L3_Org_ID", currentUser.getL3OrgId())
        );

        // --- 新增逻辑：查询职位人数 ---
        if (!positions.isEmpty()) {
            List<Integer> positionIds = positions.stream().map(Position::getPositionId).collect(Collectors.toList());
            List<Map<String, Object>> counts = positionMapper.countEmployeesByPositionIds(positionIds);

            // 将人数转换为 Map<PositionID, Count> 以便快速查找
            Map<Integer, Long> countMap = counts.stream()
                    .collect(Collectors.toMap(
                            map -> (Integer) map.get("positionId"),
                            map -> (Long) map.get("employeeCount")
                    ));
            model.addAttribute("employeeCounts", countMap);
        }
        // --- 结束新增逻辑 ---

        model.addAttribute("positions", positions);
        model.addAttribute("newPosition", new Position()); // 用于新建表单
        return "hr/position_list";
    }

    /**
     * 新增：查看某个职位下的所有员工
     */
    @GetMapping("/{id}/employees")
    public String viewPositionEmployees(@PathVariable("id") Integer positionId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        Position position = positionMapper.selectById(positionId);

        // 安全校验：确保职位存在且属于当前人事经理的机构
        if (position == null || !position.getL3OrgId().equals(currentUser.getL3OrgId())) {
            return "redirect:/hr/positions?error=access_denied";
        }

        // 查询该职位下的所有有效用户
        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .eq("Position_ID", positionId)
                .eq("L3_Org_ID", currentUser.getL3OrgId())
                .eq("Is_Deleted", 0));

        // 根据用户ID列表查询对应的档案信息
        List<PersonnelFile> employeeFiles = new ArrayList<>();
        if (!users.isEmpty()) {
            List<Integer> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
            employeeFiles = personnelFileMapper.selectList(new QueryWrapper<PersonnelFile>().in("User_ID", userIds));
        }

        model.addAttribute("position", position);
        model.addAttribute("employeeFiles", employeeFiles);
        return "hr/position_employees"; // 指向新增的视图文件
    }


    /**
     * 保存新职位 (无需修改)
     */
    @PostMapping("/save")
    public String savePosition(@ModelAttribute Position newPosition, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || currentUser.getL3OrgId() == null) {
            model.addAttribute("error", "非法操作，无法获取您的机构信息");
            return "redirect:/hr/positions";
        }
        newPosition.setL3OrgId(currentUser.getL3OrgId());
        newPosition.setAuthLevel("Employee");
        positionMapper.insert(newPosition);
        return "redirect:/hr/positions?success";
    }

    /**
     * 删除职位 (无需修改)
     */
    @GetMapping("/delete/{id}")
    public String deletePosition(@PathVariable Integer id, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        Position position = positionMapper.selectById(id);
        if (position != null && position.getL3OrgId().equals(currentUser.getL3OrgId())) {
            positionMapper.deleteById(id);
        }
        return "redirect:/hr/positions";
    }
}
