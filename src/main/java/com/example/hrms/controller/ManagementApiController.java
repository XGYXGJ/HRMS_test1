package com.example.hrms.controller;

import com.example.hrms.dto.UserDTO;
import com.example.hrms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manage/api")
public class ManagementApiController {

    @Autowired
    private UserService userService;

    @GetMapping("/employees/search")
    public List<UserDTO> searchEmployees(@RequestParam(required = false) String query) {
        return userService.searchUsers(query);
    }
    
    // 如果将来有其他专门针对管理部门的API，也可以放在这里
}
