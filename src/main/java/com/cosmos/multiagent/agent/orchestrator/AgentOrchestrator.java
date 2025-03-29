package com.cosmos.multiagent.agent.orchestrator;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.model.ChatMessage;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import com.cosmos.multiagent.agent.model.ChatSession;
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
    private final CosmosAsyncContainer sessionContainer;
    private final ChatModel chatModel;

    @Autowired
    private ChatClient chatClient;
    AgentTransfer agentTransfer;
    AgentRouting agentRouting;

    public AgentOrchestrator(String sessionId, CosmosAsyncContainer sessionContainer, CosmosChatMemory chatMemory, ChatModel chatModel) {
        this.sessionId = sessionId;
        this.sessionContainer = sessionContainer;
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.create(chatModel);
        this.agentTransfer = new AgentTransfer(sessionContainer, sessionId);
        this.agentRouting = new AgentRouting(this.chatClient);
    }

    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
    }

    public String handleUserInput(String input) {
        logger.info("session id: {}", sessionId);
        CosmosItemResponse<ChatSession> sessionCosmosItemResponse = sessionContainer.readItem(sessionId, new PartitionKey(sessionId), ChatSession.class).block();
        String activeAgent = sessionCosmosItemResponse.getItem().getActiveAgent().toString();
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
        String checkActiveAgent = sessionContainer.readItem(sessionId, new PartitionKey(sessionId), ChatSession.class).block().getItem().getActiveAgent();
        if (!checkActiveAgent.equals(activeAgent)) {
            //recursive call to handle the new agent
            logger.info("Agent changed during processing. New agent: {}", checkActiveAgent);
            logger.info("Doing recursive call to handle the new agent...");
            return handleUserInput(input);
        }
        List<Message> responseMessages = new ArrayList<>();
        responseMessages.add(new ChatMessage("user", input));
        responseMessages.add(new ChatMessage("agent", response));
        chatMemory.add(sessionId, responseMessages);
        return response;
    }
}