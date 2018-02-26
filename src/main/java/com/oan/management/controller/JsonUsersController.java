package com.oan.management.controller;

import com.oan.management.model.User;
import com.oan.management.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oan on 16/02/2018.
 * This is the API for the website to get user data
 */
@RestController
public class JsonUsersController {
    @Autowired
    UserService userService;

    @GetMapping("/api/users")
    public List<User> getUsers() {
        List<User> users = userService.findAll();
        return users;
    }

    @GetMapping("/api/users/usernames")
    public List<String> getUsernames() {
        List<User> users = userService.findAll();
        ArrayList<String> usernames = new ArrayList<String>();
        for (User user : users) {
            usernames.add(user.getUsername());
        }
        return usernames;
    }

    @GetMapping("/api/users/id/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/api/users/username/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.findByUser(username);
    }
}
