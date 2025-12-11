package com.example.hrms.controller;

import com.example.hrms.dto.UserDTO;
import com.example.hrms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EmployeeApiController {

    @Autowired
    private UserService userService;

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
