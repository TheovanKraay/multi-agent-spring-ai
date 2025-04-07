// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.cosmos.multiagent.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class MultiAgentCliTester {

    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(MultiAgentCliTester.class);

    private static final String BASE_URL = "http://localhost:8080/api/chat";
    private static final String TENANT_ID = "default";
    private static final String USER_ID = "cli-user";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ObjectMapper objectMapper = new ObjectMapper();
        String sessionId = null;

        try {
            // Step 1: Create session ONCE at startup
            String sessionUrl = String.format("%s/tenant/%s/user/%s/session", BASE_URL, TENANT_ID, USER_ID);
            HttpURLConnection sessionConn = (HttpURLConnection) new URL(sessionUrl).openConnection();
            sessionConn.setRequestMethod("POST");
            sessionConn.setDoOutput(true);
            sessionConn.connect();

            BufferedReader sessionReader = new BufferedReader(new InputStreamReader(sessionConn.getInputStream()));
            sessionId = sessionReader.readLine().replace("\"", "").trim();
            sessionReader.close();

            logger.info("Session created with ID: " + sessionId);

        } catch (Exception e) {
            logger.error("Failed to create session.", e);
            e.printStackTrace();
            return;
        }

        System.out.println("Enter your message (type 'exit' to quit):");

        while (true) {
            try {
                System.out.print("user: ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }

                // Step 2: Use the same session for all completions
                String encodedInput = URLEncoder.encode(input, "UTF-8");
                String completionUrl = String.format(
                        "%s/tenant/%s/user/%s/session/%s/completion?input=%s",
                        BASE_URL, TENANT_ID, USER_ID, sessionId, encodedInput
                );

                HttpURLConnection completionConn = (HttpURLConnection) new URL(completionUrl).openConnection();
                completionConn.setRequestMethod("POST");
                completionConn.setDoOutput(true);
                completionConn.connect();

                String responseJson;
                try (BufferedReader responseReader = new BufferedReader(new InputStreamReader(completionConn.getInputStream()))) {
                    responseJson = responseReader.lines().collect(Collectors.joining());
                }

                List<Map<String, Object>> messages = objectMapper.readValue(responseJson, new TypeReference<>() {});

                boolean foundResponse = false;
                for (Map<String, Object> message : messages) {
                    String role = String.valueOf(message.get("role"));
                    if (!"user".equalsIgnoreCase(role)) {
                        String text = String.valueOf(message.get("text"));
                        System.out.println(role + " agent: " + text);
                        foundResponse = true;
                    }
                }

                if (!foundResponse) {
                    System.out.println("(no agent responses)");
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("An error occurred while communicating with the API.");
            }
        }
    }
}
