package com.example.hrms.config;

import com.example.hrms.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("/");
            return false;
        }

        String uri = request.getRequestURI();

        // 简单的权限判断
        if (uri.startsWith("/admin") && user.getPositionId() != 1) {
            response.sendError(403, "无权访问系统管理员界面");
            return false;
        }
        if (uri.startsWith("/manage") && user.getPositionId() != 2) {
            response.sendError(403, "无权访问管理部门界面");
            return false;
        }
        if (uri.startsWith("/hr") && user.getPositionId() != 3) {
            response.sendError(403, "无权访问人事经理界面");
            return false;
        }
        if (uri.startsWith("/salary") && user.getPositionId()    != 4) { // Position_ID 4: 薪酬经理
            response.sendError(403, "无权访问薪酬经理界面");
            return false;
        }

        return true;
    }
}