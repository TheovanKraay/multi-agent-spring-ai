package com.cosmos.multiagent.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cosmos.multiagent.data")
public class DataLoad {
    public static void main(String[] args) {
        SpringApplication.run(DataLoad.class, args);
    }
}
