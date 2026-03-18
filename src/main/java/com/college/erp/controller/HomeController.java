package com.college.erp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.college.erp.service.NotificationService;

@Controller
public class HomeController {

    private final NotificationService notificationService;

    public HomeController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/home")
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        if (role.equals("ADMIN")) {
            return "redirect:/admin/dashboard";
        } else if (role.equals("STAFF") || role.equals("HOD") || role.equals("PRINCIPAL")) {
            return "redirect:/staff/dashboard";
        } else if (role.equals("STUDENT")) {
            return "redirect:/student/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/notifications")
    public String viewNotifications(@RequestParam(required = false) boolean archived, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        if (archived) {
            model.addAttribute("notifications", notificationService.getArchivedNotificationsForUser(role, username));
            model.addAttribute("viewingArchived", true);
        } else {
            // notifications already added by GlobalControllerAdvice
            model.addAttribute("viewingArchived", false);
        }
        return "view-notifications";
    }
}

