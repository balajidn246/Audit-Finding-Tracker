package com.internship.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuditFindingTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditFindingTrackerApplication.class, args);
    }
}
