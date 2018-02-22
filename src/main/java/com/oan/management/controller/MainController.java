package com.oan.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oan.management.model.*;
import com.oan.management.repository.TaskRepository;
import com.oan.management.service.image.ImageService;
import com.oan.management.service.message.MessageService;
import com.oan.management.service.task.TaskService;
import com.oan.management.service.user.RankService;
import com.oan.management.service.user.UserService;
import com.oan.management.utility.CustomTimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@Controller
public class MainController {
    @Autowired
    private UserService userService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private RankService rankService;

    @GetMapping("/")
    public String root(HttpServletRequest req, Model model, Authentication authentication) {
        User userLogged = userService.findByUser(authentication.getName());
        List<Task> taskList = taskRepository.findByUserAndCompletedIsFalse(userLogged);
        // Get list of unread messages and get last message
        List<Message> unreadMessages = messageService.findByReceiverAndOpenedIs(userLogged, 0);
        if (unreadMessages.size() > 0) {
            Message lastMessage = unreadMessages.get(unreadMessages.size()-1);
            model.addAttribute("lastMessage", lastMessage);
        }
        // loggedUser and motivational texts
        if (userLogged != null) {
            model.addAttribute("loggedUser", userLogged);
            // Get the custom greeting by time
            CustomTimeMessage customTimeMessage = new CustomTimeMessage();
            model.addAttribute("timeGreeting", customTimeMessage.getMessage());
            // Tasks and unread messages
            req.getSession().setAttribute("tasksLeftSession", taskList.size());
            req.getSession().setAttribute("unreadMessages", unreadMessages.size());
            // Motivational messages
            String motivationMessage = taskService.getMotivationalMessage(taskList, userLogged);
            model.addAttribute("taskMotivation", motivationMessage);
        }

        // JSON to Object mapper
        ObjectMapper mapper = new ObjectMapper();
        try {
            Quote quote = mapper.readValue(new URL("https://talaikis.com/api/quotes/random/"), Quote.class);
            model.addAttribute("quote", quote);
        } catch (IOException e) {
            // This is just added so I could work offline in the train
            Quote quote = new Quote("A late start with motivation is better than an earlier start without any motivation", "Oan Stultjens", "motivation");
            model.addAttribute("quote", quote);
        }

        // Save profile picture for navigation bar
        Image avatar_of_id = imageService.findByTitle(userLogged.getId()+".png");
        if (avatar_of_id != null) {
            req.getSession().setAttribute("myAvatar", "/img"+avatar_of_id.getUrl());
        } else {
            req.getSession().setAttribute("myAvatar", "/img/avatar/0.png");
        }

        // Update user rank
        if (rankService.findByUser(userLogged) != null) {
            // Update rank if changes were made
            rankService.checkRank(userLogged);
            // Save rank to model
            Rank userRank = rankService.findByUser(userLogged);
            model.addAttribute("userRank", userRank);
        } else {
            Rank userRank = rankService.setRank(userLogged, "Noob", 1);
            rankService.checkRank(userLogged);
            model.addAttribute("userRank", userRank);
        }
        return "index";
    }
}
