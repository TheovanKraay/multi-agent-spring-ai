package com.cosmos.multiagent.agent.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.memory.CosmosChatSession;
import com.cosmos.multiagent.agent.models.ChatMessage;
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
    private final String userId;
    private final String tenantId;
    private final CosmosChatSession chatSession;
    private final ChatModel chatModel;

    @Autowired
    private ChatClient chatClient;
    AgentTransfer agentTransfer;
    AgentRouting agentRouting;

    public AgentOrchestrator(String sessionId, String userId, String tenantId, CosmosChatSession chatSession, CosmosChatMemory chatMemory, ChatModel chatModel) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.chatSession = chatSession;
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.create(chatModel);
        this.agentTransfer = new AgentTransfer(chatSession, sessionId, userId, tenantId);
        this.agentRouting = new AgentRouting(this.chatClient);
    }

    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
    }

    public List<Message> handleUserInput(String input) {
        List<Message> responseMessages = new ArrayList<>();
        logger.info("session id: {}", sessionId);
        String activeAgent = chatSession.getActiveAgent(sessionId, userId, tenantId);
        logger.info("Active agent: {}", activeAgent);
        if (activeAgent.equals("unknown")) {
            Map<String, String> routes = new HashMap<>();
            for (Agent agent : agents.values()) {
                routes.put(agent.getName(), agent.getSystemPrompt());
            }
            activeAgent = agentRouting.route(input, routes);
            this.agentTransfer.transferAgent(activeAgent);
        }
        logger.info("Agent to use: {}", activeAgent);
        Agent agent = agents.get(activeAgent);
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
        String checkActiveAgent = chatSession.getActiveAgent(sessionId, userId, tenantId);
        responseMessages.add(new ChatMessage("user", input));
        responseMessages.add(new ChatMessage(activeAgent, response));
        if (!checkActiveAgent.equals(activeAgent)) {
            logger.info("Agent transfer during processing. New agent: {}", checkActiveAgent);
            //recursive call to handle the new agent
            responseMessages.addAll(handleUserInput(input));
        }
        chatMemory.add(sessionId, responseMessages);
        return responseMessages;
    }


}