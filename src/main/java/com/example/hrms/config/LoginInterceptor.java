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
            response.sendError(403, "无权访问管理员界面");
            return false;
        }
        if (uri.startsWith("/hr") && user.getPositionId() != 2) {
            response.sendError(403, "无权访问HR界面");
            return false;
        }

        return true;
    }
}