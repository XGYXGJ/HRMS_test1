package com.example.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.dto.UserDTO;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.UserMapper;
import com.example.hrms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<UserDTO> getUsersByOrgAndPosition(Integer orgId, Integer positionId) {
        return userMapper.findUsersByOrgAndPosition(orgId, positionId);
    }

    @Override
    public List<UserDTO> searchUsers(String query) {
        return userMapper.searchUsers(query);
    }

    @Override
    public boolean transferEmployee(Integer employeeId, Integer targetOrgId, Integer targetPositionId) {
        User user = userMapper.selectById(employeeId);
        if (user == null) {
            return false;
        }
        user.setL3OrgId(targetOrgId);
        user.setPositionId(targetPositionId);
        return userMapper.updateById(user) > 0;
    }
}
