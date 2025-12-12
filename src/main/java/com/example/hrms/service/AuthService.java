package com.example.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired private UserMapper userMapper;

    public User login(String username, String password) {
        // 简单实现，实际生产环境需要加盐Hash验证
        return userMapper.selectOne(new QueryWrapper<User>()
                .eq("Username", username)
                .eq("Password_Hash", password)
                .eq("Is_Deleted", 0)); // 增加条件：只查询未被删除的用户
    }
}
