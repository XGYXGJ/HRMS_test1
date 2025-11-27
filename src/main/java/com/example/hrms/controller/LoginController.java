package com.example.hrms.controller;

import com.example.hrms.entity.User;
import com.example.hrms.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    @Autowired private AuthService authService;

    @GetMapping("/")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(String username, String password, HttpSession session, Model model) {
        User user = authService.login(username, password);
        if (user != null) {
            session.setAttribute("user", user);
            // 根据职位ID简单跳转 (假设 1:Admin, 2:HR, 3:Employee)
            if (user.getPositionId() == 1) return "redirect:/admin/dashboard";
            if (user.getPositionId() == 2) return "redirect:/hr/dashboard";
            return "redirect:/emp/home";
        }
        model.addAttribute("error", "用户名或密码错误");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}