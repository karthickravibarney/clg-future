package com.college.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads/documents");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Cross-platform absolute path fix
        String resourceLocation = uploadPath.replace("\\", "/");
        if (!resourceLocation.startsWith("/")) {
            resourceLocation = "/" + resourceLocation;
        }

        registry.addResourceHandler("/uploads/documents/**")
                .addResourceLocations("file:" + resourceLocation + "/");
    }
}
