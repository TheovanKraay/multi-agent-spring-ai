package com.cosmos.multiagent.agent.orchestrator;

import com.azure.json.implementation.jackson.core.JsonProcessingException;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.model.ChatMessage;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
//import com.example.multiai.memory.CosmosMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AgentOrchestrator {
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    //private final CosmosMemoryStore memoryStore;
    private final CosmosChatMemory chatMemory;
    private final String sessionId;
    private String lastRespondingAgent;
    private final ChatModel chatModel;

    @Autowired
    private ChatClient chatClient;

    public AgentOrchestrator(String sessionId, CosmosChatMemory chatMemory, ChatModel chatModel) {
        this.sessionId = sessionId;
        //this.memoryStore = memoryStore;
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.create(chatModel);
    }

    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
    }

    public String handleUserInput(String input) {
        AgentRouting workflow = new AgentRouting(this.chatClient);
        Map<String, String> routes = new HashMap<>();
        for (Agent agent : agents.values()) {
            routes.put(agent.getName(), agent.getSystemPrompt());
        }
        String agentToUse = workflow.route(input, routes);
        //String agentToUse = (lastRespondingAgent != null) ? lastRespondingAgent : agents.keySet().iterator().next();
        System.out.println("DEBUG >>> agentToUse = " + agentToUse);
        Agent agent = agents.get(agentToUse);
        System.out.println("DEBUG >>> agent = " + agent);

        List<Message> history = chatMemory.get(sessionId, 10);


        System.out.println("DEBUG >>> String.valueOf(history) = " + String.valueOf(history));
        System.out.println("DEBUG >>> agent.getSystemPrompt() = " + agent.getSystemPrompt());

        //String response = ChatClient.create(chatModel).prompt(agent.getSystemPrompt()).user(input).messages(messages).tools(agent.getTools().toArray()).call().content();
        //String response = chatModel.call(new Prompt(String.valueOf(history))).getResult().getOutput().toString();

        String response = ChatClient.builder(chatModel)
                .build()
                .prompt(agent.getSystemPrompt())
                .advisors(
                        new MessageChatMemoryAdvisor(chatMemory)
                        //new QuestionAnswerAdvisor(vectorStore)
                )
                .user(input)
                //.messages(messages)
                .tools(agent.getTools().toArray())
                .call()
                .content();


        List<Message> responseMessages = new ArrayList<>();

        responseMessages.add(new ChatMessage("user", input));
        responseMessages.add(new ChatMessage("agent", response));
        chatMemory.add(sessionId, responseMessages);
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