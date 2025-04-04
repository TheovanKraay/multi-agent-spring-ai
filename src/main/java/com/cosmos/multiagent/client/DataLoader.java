package com.cosmos.multiagent.client;

import org.springframework.web.client.RestTemplate;

public class DataLoader {
    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("Triggering data load via GET /api/chat/data.....");
        String url = "http://localhost:8080/api/chat/data";

        restTemplate.getForEntity(url, Void.class);

        System.out.println("Triggered data load via GET /api/chat/data");
    }
}
