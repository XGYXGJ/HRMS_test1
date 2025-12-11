package com.example.hrms.controller;

import com.example.hrms.dto.UserDTO;
import com.example.hrms.entity.User;
import com.example.hrms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EmployeeApiController {

    @Autowired
    private UserService userService;

    @GetMapping("/employees")
    public List<UserDTO> getEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String position,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null || user.getL3OrgId() == null) {
            return Collections.emptyList();
        }
        
        return userService.searchUsers(name, position, user.getL3OrgId());
    }

    @GetMapping("/orgs/{orgId}/positions/{positionId}/employees")
    public List<UserDTO> getEmployeesByOrgAndPosition(
            @PathVariable Integer orgId,
            @PathVariable Integer positionId) {
        return userService.getUsersByOrgAndPosition(orgId, positionId);
    }

    @GetMapping("/employees/search")
    public List<UserDTO> searchEmployees(@RequestParam String query) {
        return userService.searchUsers(query);
    }
}
