package com.internship.tool.controller;

import com.internship.tool.entity.User;
import com.internship.tool.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CurrentUserController {
    private final UserRepository users;
    public CurrentUserController(UserRepository users) { this.users = users; }
    @GetMapping("/me")
    public UserAdminController.UserSummary me(Authentication auth) {
        User user = users.findByUsernameIgnoreCase(auth.getName()).orElseThrow();
        return UserAdminController.UserSummary.from(user);
    }
}
