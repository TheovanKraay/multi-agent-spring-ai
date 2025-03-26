package com.example.multiai.manager;

import com.azure.json.implementation.jackson.core.JsonProcessingException;
import com.example.multiai.agent.Agent;
import com.example.multiai.memory.CosmosMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AgentManager {
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final CosmosMemoryStore memoryStore;
    private final String sessionId;
    private String lastRespondingAgent;
    private final ChatModel chatModel;

    @Autowired
    private ChatClient chatClient;

    public AgentManager(String sessionId, CosmosMemoryStore memoryStore, ChatModel chatModel) {
        this.sessionId = sessionId;
        this.memoryStore = memoryStore;
        this.chatModel = chatModel;
    }

    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
    }

    public String handleUserInput(String input) {
        RoutingWorkflow workflow = new RoutingWorkflow(this.chatModel);
        Map<String, String> routes = new HashMap<>();
        for (Agent agent : agents.values()) {
            routes.put(agent.getName(), agent.getSystemPrompt());
        }
        String agentToUse = workflow.route(input, routes);
        //String agentToUse = (lastRespondingAgent != null) ? lastRespondingAgent : agents.keySet().iterator().next();
        System.out.println("DEBUG >>> agentToUse = " + agentToUse);
        Agent agent = agents.get(agentToUse);
        System.out.println("DEBUG >>> agent = " + agent);

        List<com.example.multiai.manager.ChatMessage> history = memoryStore.loadMemory(sessionId);


        System.out.println("DEBUG >>> String.valueOf(history) = " + String.valueOf(history));
        System.out.println("DEBUG >>> agent.getSystemPrompt() = " + agent.getSystemPrompt());
        for (com.example.multiai.manager.ChatMessage message : history) {
            System.out.println("DEBUG >>> message in history = " + message.getText());
        }

        List<Message> messages = new ArrayList<>();
        for (com.example.multiai.manager.ChatMessage message : history) {
            messages.add(new ChatMessage(message.getRole(), message.getText()));
        }
        String response = ChatClient.create(chatModel).prompt(agent.getSystemPrompt()).user(input).messages(messages).tools(agent.getTools().toArray()).call().content();
        //String response = chatModel.call(new Prompt(String.valueOf(history))).getResult().getOutput().toString();
        history.add(new ChatMessage("user", input));
        history.add(new ChatMessage("agent", response));
        memoryStore.saveMemory(sessionId, history);

        lastRespondingAgent = agent.getName();
        return response;
    }

    public List<Message> getMessages(String text) throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return StreamSupport.stream(objectMapper.readTree(text).get("messages").spliterator(), false)
                .map(node -> new ChatMessage(node.get("role").asText(), node.get("content").asText()))
                .collect(Collectors.toList());
    }

}