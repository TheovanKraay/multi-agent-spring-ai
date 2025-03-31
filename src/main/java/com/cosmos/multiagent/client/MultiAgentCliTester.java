package com.cosmos.multiagent.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class MultiAgentCliTester {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ObjectMapper objectMapper = new ObjectMapper();
        String apiUrl = "http://localhost:8080/api/chat?input=";

        System.out.println("Enter your message (type 'exit' to quit):");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input)) {
                break;
            }

            try {
                String encodedInput = URLEncoder.encode(input, "UTF-8");
                URL url = new URL(apiUrl + encodedInput);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String jsonResponse = reader.lines().collect(Collectors.joining());
                    reader.close();

                    List<String> responses = objectMapper.readValue(jsonResponse, new TypeReference<List<String>>() {});

                    for (String response : responses) {
                        System.out.println("AI: " + response);
                    }
                } else {
                    System.out.println("Request failed with HTTP code: " + responseCode);
                }

            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        scanner.close();
        System.out.println("CLI tester exited.");
    }
}
