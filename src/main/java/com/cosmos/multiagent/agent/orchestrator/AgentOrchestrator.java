package com.cosmos.multiagent.agent.orchestrator;

import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.model.ChatMessage;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AgentOrchestrator {
    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(AgentOrchestrator.class);
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final CosmosChatMemory chatMemory;
    private final String sessionId;
    private final ChatModel chatModel;

    @Autowired
    private ChatClient chatClient;

    public AgentOrchestrator(String sessionId, CosmosChatMemory chatMemory, ChatModel chatModel) {
        this.sessionId = sessionId;
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.create(chatModel);
    }

    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
    }

    public String handleUserInput(String input) {
        AgentRouting agentRouting = new AgentRouting(this.chatClient);
        Map<String, String> routes = new HashMap<>();
        for (Agent agent : agents.values()) {
            routes.put(agent.getName(), agent.getSystemPrompt());
        }
        String agentToUse = agentRouting.route(input, routes);
        logger.info("Agent to use: {}", agentToUse);
        Agent agent = agents.get(agentToUse);
        String response = ChatClient.builder(chatModel)
                .build()
                .prompt(agent.getSystemPrompt())
                .advisors(
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .user(input)
                .tools(agent.getTools().toArray())
                .call()
                .content();
        List<Message> responseMessages = new ArrayList<>();
        responseMessages.add(new ChatMessage("user", input));
        responseMessages.add(new ChatMessage("agent", response));
        chatMemory.add(sessionId, responseMessages);
        return response;
    }
}