package com.cosmos.multiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cosmos.multiagent")
public class MultiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiAgentApplication.class, args);
    }
}
