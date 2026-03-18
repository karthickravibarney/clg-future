package com.college.erp.config;

import com.college.erp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NotificationDataInitializer implements CommandLineRunner {

    @Autowired
    private NotificationService notificationService;

    @Override
    public void run(String... args) throws Exception {
        // No automatic notifications scheduled on startup
    }
}
