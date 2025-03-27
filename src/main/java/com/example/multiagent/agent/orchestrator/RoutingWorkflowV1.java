/*
package com.example.multiai.agent.manager;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

public class RoutingWorkflowV1 {

    private final ChatModel chatModel;

    public RoutingWorkflowV1(ChatModel ChatModel) {
        System.out.println("[RoutingWorkflow] Initialized with ChatClient.");
        this.chatModel = ChatModel;
    }

    public String route(String input, Map<String, String> routes) {
        System.out.println("[RoutingWorkflow] Routing input: " + input);
        String route = determineRoute(input, routes);
        System.out.println("[RoutingWorkflow] Determined route: " + route);
        return route;
    }

    private String determineRoute(String input, Map<String, String> routes) {
        System.out.println("[RoutingWorkflow] Determining route via LLM...");

        StringBuilder options = new StringBuilder();
        for (String key : routes.keySet()) {
            options.append("- ").append(key).append("\n");
        }

        String routingPrompt = """
                You are a routing assistant that determines which specialist should handle a customer input.
                Available categories:
                %s

                Based on the following input, respond with just the best matching category label from the list above:

                "%s"
                """.formatted(options, input);

        System.out.println("[RoutingWorkflow] Routing prompt:\n" + routingPrompt);

        String decision;
        try {
            decision = ChatClient.create(this.chatModel).prompt()
                    .system("You are a helpful assistant that only responds with one of the available category labels.")
                    .user(routingPrompt)
                    .call()
                    .content()
                    .trim()
                    .toLowerCase();

            System.out.println("[RoutingWorkflow] LLM decision: " + decision);

            if (!routes.containsKey(decision)) {
                System.out.println("[RoutingWorkflow] Decision not in route list, defaulting to 'general'");
                return "general";
            }

            return decision;
        } catch (Exception e) {
            System.err.println("[RoutingWorkflow] Exception during routing:");
            e.printStackTrace();
            return "general";
        }
    }
}
*/
