// src/main/java/com/example/hrms/controller/PositionController.java
package com.example.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.Position;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.PositionMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/hr/positions") // 路径归属到人事经理下
public class PositionController {

    @Autowired
    private PositionMapper positionMapper;

    /**
     * 职位列表页面
     */
    @GetMapping
    public String listPositions(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || currentUser.getL3OrgId() == null) {
            model.addAttribute("error", "无法获取您的机构信息");
            return "hr/position_list";
        }

        // 查询当前人事经理所在机构下的所有职位
        List<Position> positions = positionMapper.selectList(
                new QueryWrapper<Position>().eq("L3_Org_ID", currentUser.getL3OrgId())
        );

        model.addAttribute("positions", positions);
        model.addAttribute("newPosition", new Position()); // 用于新建表单
        return "hr/position_list";
    }

    /**
     * 保存新职位
     */
    @PostMapping("/save")
    public String savePosition(@ModelAttribute Position newPosition, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || currentUser.getL3OrgId() == null) {
            model.addAttribute("error", "非法操作，无法获取您的机构信息");
            return "redirect:/hr/positions";
        }
        // 关键：将新职位与当前人事经理的机构绑定
        newPosition.setL3OrgId(currentUser.getL3OrgId());
        // 默认新职位都是普通员工级别
        newPosition.setAuthLevel("Employee");

        positionMapper.insert(newPosition);
        return "redirect:/hr/positions?success";
    }

    /**
     * 删除职位 (通过GET请求简化实现，生产环境建议用DELETE)
     */
    @GetMapping("/delete/{id}")
    public String deletePosition(@PathVariable Integer id, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        Position position = positionMapper.selectById(id);

        // 安全校验：只能删除自己机构下的职位
        if (position != null && position.getL3OrgId().equals(currentUser.getL3OrgId())) {
            positionMapper.deleteById(id);
        }
        return "redirect:/hr/positions";
    }
}
