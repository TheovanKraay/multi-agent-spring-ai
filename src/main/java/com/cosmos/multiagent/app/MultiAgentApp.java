package com.cosmos.multiagent.app;

import com.azure.cosmos.*;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import com.cosmos.multiagent.app.tools.DateTimeTools;
import com.cosmos.multiagent.app.tools.MathAssistantTools;
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

@SpringBootApplication(scanBasePackages = "com.cosmos.multiagent")
public class MultiAgentApp {

    public static void main(String[] args) {
        SpringApplication.run(MultiAgentApp.class, args);
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
            CosmosAsyncContainer container = db.getContainer("ChatMemory");

            CosmosChatMemory chatMemory = new CosmosChatMemory(container);

            ArrayList<Object> timeTellerTools = new ArrayList<>();
            timeTellerTools.add(new DateTimeTools());

            ArrayList<Object> tellJokeTools = new ArrayList<>();
            tellJokeTools.add(new TellJokeTools());

            ArrayList<Object> mathTools = new ArrayList<>();
            mathTools.add(new MathAssistantTools());

            AgentOrchestrator agentOrchestrator = new AgentOrchestrator(UUID.randomUUID().toString(), chatMemory, chatModel);

            agentOrchestrator.registerAgent(new Agent("timeteller",
                    "You are a time teller assistant. Call getCurrentDateTime()",
                    timeTellerTools, List.of()));
            agentOrchestrator.registerAgent(new Agent("joketeller",
                    "You are a funny assistant that can tell the user a joke. Call TellJokeTools()",
                    tellJokeTools, List.of()));
            agentOrchestrator.registerAgent(new Agent("mathassistant",
                    "You can help the user with sums. Ask user which numbers they want to add together. Call addNumbers()",
                    mathTools, List.of()));

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
