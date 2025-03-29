package com.cosmos.multiagent.app;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemResponse;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import com.cosmos.multiagent.agent.model.ChatSession;
import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import com.cosmos.multiagent.agent.orchestrator.AgentTransfer;
import com.cosmos.multiagent.app.tools.DateTimeTools;
import com.cosmos.multiagent.app.tools.MathAssistantTools;
import com.cosmos.multiagent.app.tools.ProductSearchTools;
import com.cosmos.multiagent.app.tools.TellJokeTools;
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

@SpringBootApplication(scanBasePackages = "com.cosmos.multiagent.app")
public class MultiAgentApp {

    public static void main(String[] args) {
        SpringApplication.run(com.cosmos.multiagent.app.MultiAgentApp.class, args);
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
            CosmosAsyncDatabase db = cosmosAsyncClient.getDatabase("MultiAgentDb");
            db.createContainerIfNotExists("ChatMemory", "/conversationId").block();
            db.createContainerIfNotExists("Sessions", "/id").block();
            CosmosAsyncContainer container = db.getContainer("ChatMemory");
            CosmosChatMemory chatMemory = new CosmosChatMemory(container);
            CosmosAsyncContainer sessionContainer = db.getContainer("Sessions");
            String id = UUID.randomUUID().toString();
            ChatSession session = new ChatSession();
            session.setId(id);
            session.setActiveAgent("unknown");
            session.setName("New Session");
            CosmosItemResponse<ChatSession> response  = sessionContainer.createItem(session).block();
            String sessionId = response.getItem().getId();
            System.out.println("Session ID after creation: " + sessionId);
            AgentTransfer agentTransferTool = new AgentTransfer(sessionContainer, sessionId);

            //add agent tools
            ArrayList<Object> timeTellerTools = new ArrayList<>();
            timeTellerTools.add(new DateTimeTools());
            AgentTransfer timeTellerAgentTransferTool = new AgentTransfer(sessionContainer, sessionId);
            timeTellerAgentTransferTool.setRoutableAgents(List.of("joketeller", "mathassistant", "productsearch"));
            timeTellerTools.add(timeTellerAgentTransferTool);


            ArrayList<Object> tellJokeTools = new ArrayList<>();
            tellJokeTools.add(new TellJokeTools());
            AgentTransfer tellJokeToolsAgentTransferTool = new AgentTransfer(sessionContainer, sessionId);
            tellJokeToolsAgentTransferTool.setRoutableAgents(List.of("timeteller", "mathassistant", "productsearch"));
            tellJokeTools.add(tellJokeToolsAgentTransferTool);

            ArrayList<Object> mathTools = new ArrayList<>();
            mathTools.add(new MathAssistantTools());
            AgentTransfer mathToolsAgentTransferTool = new AgentTransfer(sessionContainer, sessionId);
            mathToolsAgentTransferTool.setRoutableAgents(List.of("timeteller", "joketeller", "productsearch"));
            mathTools.add(mathToolsAgentTransferTool);

            ArrayList<Object> productSearchTools = new ArrayList<>();
            productSearchTools.add(new ProductSearchTools(vectorStore));
            AgentTransfer productSearchToolsAgentTransferTool = new AgentTransfer(sessionContainer, sessionId);
            productSearchToolsAgentTransferTool.setRoutableAgents(List.of("timeteller", "joketeller", "mathassistant"));
            productSearchTools.add(productSearchToolsAgentTransferTool);

            AgentOrchestrator agentOrchestrator = new AgentOrchestrator(sessionId,sessionContainer, chatMemory, chatModel);

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
                String reply = agentOrchestrator.handleUserInput(input);
                System.out.println("AI: " + reply);
            }
        };
    }
}
