// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.cosmos.multiagent.client;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

public class DataLoader {
    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(DataLoader.class);
    public static void main(String[] args) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        logger.info("Triggering data load via PUT /api/chat/data.....");

        String url = "http://localhost:8080/api/chat/data";

        RequestEntity<Void> request = new RequestEntity<>(HttpMethod.PUT, URI.create(url));
        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
        logger.info("Triggered data load via PUT /api/chat/data, response code: " + response.getStatusCode());
    }
}