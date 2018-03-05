package com.oan.management.controller.task;

import com.oan.management.model.Message;
import com.oan.management.model.Task;
import com.oan.management.model.User;
import com.oan.management.repository.UserRepository;
import com.oan.management.service.message.MessageService;
import com.oan.management.service.rank.RankService;
import com.oan.management.service.task.TaskService;
import com.oan.management.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Oan on 18/01/2018.
 */

@Controller
public class TaskController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private RankService rankService;

    public User getLoggedUser(Authentication authentication) {
        return userService.findByUser(authentication.getName());
    }

    @GetMapping("/task-list")
    public String tasklist(HttpServletRequest req, Model model, Authentication authentication) {
        User userLogged = getLoggedUser(authentication);
        model.addAttribute("task", new Task());
        // Get uncompleted tasks and sort by date
        List<Task> taskList = taskService.findByUserAndCompletedIsFalseAndApprovedIsTrue(userLogged);
        taskList.sort(Comparator.comparing(Task::getTargetDate));
        // get completed tasks and sort by date
        List<Task> completedTasksList = taskService.findByUserAndCompletedIsTrueAndApprovedIsTrue(userLogged);
        completedTasksList.sort(Comparator.comparing(Task::getTargetDate));

        List<Message> unreadMessages = messageService.findByReceiverAndOpenedIs(userLogged, 0);

        // Check for pending tasks
        List<Task> pendingTasks = taskService.findByUserAndApprovedIsFalse(userLogged);
        req.getSession().setAttribute("pendingTasks", pendingTasks);

        if (userLogged != null) {
            model.addAttribute("loggedUser", userLogged);
            model.addAttribute("tasks", taskList);
            model.addAttribute("completedTasks", completedTasksList);
            req.getSession().setAttribute("tasksLeft", taskList.size());
            req.getSession().setAttribute("unreadMessages", unreadMessages.size());
        }
        String  today = new Date(Calendar.getInstance().getTime().getTime()).toString();
        model.addAttribute("today", today);
        System.out.println(taskService.getOne(8L).getTargetDate().toString());
        return "task-list";
    }

    @GetMapping("/tasks-pending")
    public String getPendingTasksPage(Model model, Authentication authentication) {
        User userLogged = getLoggedUser(authentication);
        model.addAttribute("loggedUser", userLogged);
        List<Task> pendingTasks = taskService.findByUserAndApprovedIsFalse(userLogged);
        model.addAttribute("pendingTasks", pendingTasks);
        return "tasks-pending";
    }

    @PostMapping("/task-list")
    public String newTask(Model model, @Valid Task task, @RequestParam("targetDate") String date, Authentication authentication) {
        // Get logged in user and his tasks
        User userLogged = getLoggedUser(authentication);
        List<Task> taskList = taskService.findByUser(userLogged);
        if (userLogged != null) {
            model.addAttribute("loggedUser", userLogged);
            model.addAttribute("tasks", taskList);
        }

        // HTML String date to Date
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        try {
            java.sql.Date sqlDate = new java.sql.Date(format.parse(date).getTime());
            task.setTargetDate(sqlDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        taskService.save(new Task(userLogged, task.getDescription(), task.getTargetDate(), task.isCompleted(), userLogged, true ));
        userService.incrementTasksCreated(userLogged);
        userRepository.save(userLogged); // TODO This can be removed I guess
        return "redirect:/task-list";
    }

    @GetMapping("/task-assign")
    public String assignTaskPage(Model model, Authentication authentication, Task task, @RequestParam Long id) {
        User userLogged = getLoggedUser(authentication);
        User recepient = userService.findById(id);
        if (recepient != null) {
            if (userLogged.getId() != id) {
                model.addAttribute("recepient",recepient);
                model.addAttribute("loggedUser", userLogged);
                model.addAttribute("task", new Task());
                task.setUser(recepient);
                return "task-assign";
            } else {
                return "redirect:task-list?self";
            }
        } else {
            return "redirect:task-list?usernotfound";
        }
    }

    @PostMapping("/task-assign")
    public String assignTask(Model model, Task task, Authentication authentication, @RequestParam Long id, @RequestParam("targetDate") String date) {
        User userLogged = getLoggedUser(authentication);
        if (userLogged != null) {
            model.addAttribute("loggedUser", userLogged);
        }

        // HTML String date to Date
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy"); //TODO: Make utility
        try {
            java.sql.Date sqlDate = new java.sql.Date(format.parse(date).getTime());
            task.setTargetDate(sqlDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        User target = userService.findById(id);
        if (target != null) {
            taskService.save(new Task(target, task.getDescription(), task.getTargetDate(), false, userLogged, false ));
            // userService.incrementTasksCreated(userLogged); // TODO This has to execute when user accepted the task
            return "redirect:/profile?id="+target.getId();
        } else {
            return "redirect:task-list?usernotfound";
        }
    }

    @GetMapping("/task-approve")
    public String approveTask(Authentication authentication, @RequestParam Long id) {
        User userLogged = getLoggedUser(authentication);
        if (id != null) {
            if (taskService.findByUser(userLogged).contains(taskService.getOne(id))) {
                taskService.approveTask(taskService.findById(id));
                return "redirect:/tasks-pending?approved";
            } else {
                return "redirect:/tasks-pending?notfound";
            }
        } else {
            return "redirect:/tasks-pending?notfound";
        }
    }

    @GetMapping("/task-deny")
    public String denyTask(Authentication authentication, @RequestParam Long id) {
        User userLogged = getLoggedUser(authentication);
        if (id != null) {
            if (taskService.findByUser(userLogged).contains(taskService.getOne(id))) {
                taskService.denyTask(taskService.findById(id));
                return "redirect:/tasks-pending?denied";
            } else {
                return "redirect:/tasks-pending?notfound";
            }
        } else {
            return "redirect:/tasks-pending?notfound";
        }
    }

    @GetMapping("/task-delete")
    public String deleteTask(Model model, Task task, @RequestParam Long id, Authentication authentication) {
        User userLogged = getLoggedUser(authentication);
        // Check if it's user's task
        if (taskService.findByUser(getLoggedUser(authentication)).contains(taskService.getOne(id))) {
            taskService.deleteTaskById(id);
            userService.decrementTasksCreated(userLogged);
            return "redirect:/task-list?deleted";
        } else {
            return "redirect:/task-list?notfound";
        }
    }

    @GetMapping("/task-edit")
    public String editTaskOnScreen(Model model, Task task, @RequestParam Long id, Authentication authentication) {
        User userLogged = getLoggedUser(authentication);
        List<Task> taskList = taskService.findByUser(userLogged);
        if (userLogged != null) {
            model.addAttribute("loggedUser", userLogged);
            model.addAttribute("tasks", taskList);
        }
        // Check if it's user's task
        if (taskService.findByUser(getLoggedUser(authentication)).contains(taskService.getOne(id))) {
            model.addAttribute("task", taskService.getOne(id));
            return "/task-edit";
        } else {
            return "redirect:task-list?notfound";
        }
    }

    @PostMapping("/task-edit")
    public String editTask(Model model, Task task, Authentication authentication) {
        User userLogged = getLoggedUser(authentication);
        List<Task> taskList = taskService.findByUser(userLogged);
        if (userLogged != null) {
            model.addAttribute("loggedUser", userLogged);
            model.addAttribute("tasks", taskList);
        }
        taskService.editById(task.getId(),task.getDescription(),task.getTargetDate(), task.isCompleted());
        return "redirect:task-list?updated";
    }

    @GetMapping("/task-complete")
    public String completeTask(Model model, Task task, @RequestParam Long id, Authentication authentication) {
        taskService.completeTaskById(id);
        User userLogged = getLoggedUser(authentication);
        // Update statistics
        userLogged.setTasksCompleted(userLogged.getTasksCompleted()+1);
        // Update rank
        if (rankService.findByUser(userLogged)==null) {
            rankService.setRank(userLogged, "Newbie", 1);
        } else {
            rankService.checkRank(userLogged);
        }
        // Save user
        userRepository.save(userLogged);
        return "redirect:/task-list";
    }

    @GetMapping("/task-uncomplete")
    public String uncompleteTask(Model model, Task task, @RequestParam Long id, Authentication authentication) {
        taskService.uncompleteTaskById(id);
        User userLogged = getLoggedUser(authentication);
        // Update statistics
        userLogged.setTasksCompleted(userLogged.getTasksCompleted()-1);
        userRepository.save(userLogged);
        return "redirect:/task-list";
    }
}
