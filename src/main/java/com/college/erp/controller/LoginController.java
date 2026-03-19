package com.college.erp.controller;

import com.college.erp.security.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.college.erp.service.NotificationService;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final NotificationService notificationService;

    @Value("${erp.jwt.cookie-name}")
    private String jwtCookieName;

    public LoginController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
            NotificationService notificationService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.notificationService = notificationService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password, HttpServletResponse response,
            Model model) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateTokenFromUsername(username);

            Cookie cookie = new Cookie(jwtCookieName, jwt);
            cookie.setHttpOnly(true);
            cookie.setSecure(true); // Always secure for production (Render HTTPS)
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            cookie.setAttribute("SameSite", "None"); // Required for cross-site cookie handling in some cases

            response.addCookie(cookie);

            // Redundant Header for absolute certainty in modern browsers
            response.setHeader("Set-Cookie",
                    jwtCookieName + "=" + jwt +
                            "; Path=/; HttpOnly; Secure; SameSite=None; Max-Age=" + (24 * 60 * 60));

            return "redirect:/home";
        } catch (AuthenticationException e) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        Cookie cookie = new Cookie(jwtCookieName, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login?logout";
    }

    @PostMapping("/notifications/mark-read/{id}")
    public String markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/clear/{id}")
    public String clearNotification(@PathVariable Long id) {
        notificationService.clearNotification(id);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable("id") Long id) {
        notificationService.deleteNotification(id);
        return "redirect:/notifications?archived=true";
    }

    @PostMapping("/notifications/clear-all")
    public String clearAll(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String userId = auth.getName();
            String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            notificationService.clearAllNotifications(role, userId);
        }
        return "redirect:/notifications";
    }

    @GetMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/home";
        }
        return "redirect:/login";
    }
}
