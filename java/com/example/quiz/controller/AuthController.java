package com.example.quiz.controller;

import com.example.quiz.entity.User;
import com.example.quiz.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        User user = userRepository.findByUsername(username);

        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("error", "Sai tài khoản hoặc mật khẩu!");
            return "login";
        }

        session.setAttribute("user", user);

        // chuyển hướng theo role
        if (user.getRole().equals("ROLE_TEACHER")) {
            return "redirect:/teacher";
        } else {
            return "redirect:/student";
        }
    }



    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }


    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String role,
                           Model model) {

        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "Username đã tồn tại!");
            return "register";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);

        userRepository.save(user);

        model.addAttribute("success", "Đăng ký thành công!");
        return "redirect:/login";
    }


    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }


    @GetMapping("/teacher")
    public String teacherPage(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.getRole().equals("ROLE_TEACHER")) {
            return "redirect:/login";
        }

        return "teacher";
    }


    @GetMapping("/student")
    public String studentPage(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.getRole().equals("ROLE_STUDENT")) {
            return "redirect:/login";
        }

        return "student";
    }
}