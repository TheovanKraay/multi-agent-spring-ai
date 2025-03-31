package com.cosmos.multiagent.api;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemResponse;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import com.cosmos.multiagent.agent.memory.CosmosChatSession;
import com.cosmos.multiagent.agent.models.ChatSession;
import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import com.cosmos.multiagent.agent.orchestrator.AgentTransfer;
import com.cosmos.multiagent.api.tools.DateTimeTools;
import com.cosmos.multiagent.api.tools.MathAssistantTools;
import com.cosmos.multiagent.api.tools.ProductSearchTools;
import com.cosmos.multiagent.api.tools.TellJokeTools;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@SpringBootApplication(scanBasePackages = "com.cosmos.multiagent.api")
public class MultiAgent {
    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(MultiAgent.class);
    public static void main(String[] args) {
        SpringApplication.run(MultiAgent.class, args);
    }

    @Bean
    public CommandLineRunner runApp(
            ChatClient chatClient,
            EmbeddingModel embeddingModel,
            VectorStore vectorStore,
            CosmosAsyncClient cosmosAsyncClient,
            ChatModel chatModel
    ) {
        return args -> {
            String CosmosDatabaseName = "MultiAgentDb";
            CosmosChatMemory chatMemory = new CosmosChatMemory(cosmosAsyncClient, CosmosDatabaseName);
            CosmosChatSession chatSession = new CosmosChatSession(cosmosAsyncClient, CosmosDatabaseName);
            String userId = "User1";
            String tenantId = "Tenant1";
            String sessionId = chatSession.createSessionId(userId, tenantId);
            logger.info("Session ID after creation: {}", sessionId);

            //add agent tools
            ArrayList<Object> timeTellerTools = new ArrayList<>();
            timeTellerTools.add(new DateTimeTools());
            AgentTransfer timeTellerAgentTransferTool = new AgentTransfer(chatSession, sessionId, userId, tenantId);
            timeTellerAgentTransferTool.setRoutableAgents(List.of("joketeller", "mathassistant", "productsearch"));
            timeTellerTools.add(timeTellerAgentTransferTool);


            ArrayList<Object> tellJokeTools = new ArrayList<>();
            tellJokeTools.add(new TellJokeTools());
            AgentTransfer tellJokeToolsAgentTransferTool = new AgentTransfer(chatSession, sessionId, userId, tenantId);
            tellJokeToolsAgentTransferTool.setRoutableAgents(List.of("timeteller", "mathassistant", "productsearch"));
            tellJokeTools.add(tellJokeToolsAgentTransferTool);

            ArrayList<Object> mathTools = new ArrayList<>();
            mathTools.add(new MathAssistantTools());
            AgentTransfer mathToolsAgentTransferTool = new AgentTransfer(chatSession, sessionId, userId, tenantId);
            mathToolsAgentTransferTool.setRoutableAgents(List.of("timeteller", "joketeller", "productsearch"));
            mathTools.add(mathToolsAgentTransferTool);

            ArrayList<Object> productSearchTools = new ArrayList<>();
            productSearchTools.add(new ProductSearchTools(vectorStore));
            AgentTransfer productSearchToolsAgentTransferTool = new AgentTransfer(chatSession, sessionId, userId, tenantId);
            productSearchToolsAgentTransferTool.setRoutableAgents(List.of("timeteller", "joketeller", "mathassistant"));
            productSearchTools.add(productSearchToolsAgentTransferTool);

            AgentOrchestrator agentOrchestrator = new AgentOrchestrator(sessionId, userId, tenantId,chatSession,chatMemory, chatModel);

            //register agents
            agentOrchestrator.registerAgent(new Agent("timeteller",
                    "You are a time teller assistant. Call getCurrentDateTime()"+
                            "You can also transfer the user to another agent by calling getRoutableAgents() to " +
                            "determine which agents you can call, then transferAgent() passing the appropriate agent" +
                            "for the question being asked.",
                    timeTellerTools));
            agentOrchestrator.registerAgent(new Agent("joketeller",
                    "You are a funny assistant that can tell the user a joke. Call TellJokeTools()" +
                            "You can also transfer the user to another agent by calling getRoutableAgents() to " +
                            "determine which agents you can call, then transferAgent() passing the appropriate agent" +
                            "for the question being asked.",
                    tellJokeTools));
            agentOrchestrator.registerAgent(new Agent("mathassistant",
                    "You can help the user with sums. Ask user which numbers they want to add together. Call addNumbers()"+
                        "You can also transfer the user to another agent by calling getRoutableAgents() to " +
                            "determine which agents you can call, them transferAgent() passing the appropriate agent" +
                            "for the question being asked.",
                    mathTools));
            agentOrchestrator.registerAgent(new Agent("productsearch",
                    "You can help the user search for products. Ask for what products the user is interested in. Call productSearch() and pass in the user's question as an argument.",
                    productSearchTools));

            //start the chat
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("User: ");
                String input = scanner.nextLine();
                List<String> replyArray = agentOrchestrator.handleUserInput(input);
                for (String reply: replyArray){
                    System.out.println("AI: " + reply);
                }
            }
        };
    }
}
