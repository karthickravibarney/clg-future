package com.college.erp.controller;

import com.college.erp.model.Staff;
import com.college.erp.model.Student;
import com.college.erp.repository.StaffRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @ModelAttribute
    public void addAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            String fullRole = auth.getAuthorities().iterator().next().getAuthority();
            String role = fullRole.replace("ROLE_", "");
            
            model.addAttribute("username", username);
            model.addAttribute("role", role);
            model.addAttribute("userId", username);

            // Fetch extra details based on role
            if ("ADMIN".equals(role)) {
                model.addAttribute("userFullName", "Administrator");
            } else if ("STUDENT".equals(role)) {
                Optional<Student> student = studentRepository.findByRollNumber(username);
                student.ifPresent(s -> {
                    model.addAttribute("userFullName", s.getFullName());
                    model.addAttribute("profilePhotoPath", s.getProfilePhotoPath());
                });
            } else {
                Optional<Staff> staff = staffRepository.findByEmployeeId(username);
                staff.ifPresent(s -> {
                    model.addAttribute("userFullName", s.getFullName());
                    model.addAttribute("profilePhotoPath", s.getProfilePhotoPath());
                });
            }

            model.addAttribute("notifications", notificationService.getNotificationsForUser(role, username));
            model.addAttribute("unreadCount", notificationService.getUnreadCount(role, username));
        }
    }
}

