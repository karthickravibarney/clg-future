package com.college.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class ErpApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		setRegisterErrorPageFilter(false);
		return application.sources(ErpApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(ErpApplication.class, args);
	}

}
