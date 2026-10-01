package com.internship.tool.controller;

import com.internship.tool.entity.User;
import com.internship.tool.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/users/assignees")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AssigneeController {
    private final UserRepository users;
    public AssigneeController(UserRepository users) { this.users = users; }
    @GetMapping
    public List<UserAdminController.UserSummary> list() {
        return users.findAll().stream().filter(User::isEnabled).map(UserAdminController.UserSummary::from).toList();
    }
}
