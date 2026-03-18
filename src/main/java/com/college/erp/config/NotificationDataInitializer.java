package com.college.erp.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NotificationDataInitializer implements CommandLineRunner {


    @Override
    public void run(String... args) throws Exception {
        // No automatic notifications scheduled on startup
    }
}
