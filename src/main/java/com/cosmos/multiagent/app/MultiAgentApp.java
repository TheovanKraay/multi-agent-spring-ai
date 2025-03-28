package com.cosmos.multiagent.app;

import com.azure.cosmos.*;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import com.cosmos.multiagent.app.tools.DateTimeTools;
import com.cosmos.multiagent.app.tools.MathAssistantTools;
import com.cosmos.multiagent.app.tools.ProductSearchTools;
import com.cosmos.multiagent.app.tools.TellJokeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

            Document document1 = new Document(UUID.randomUUID().toString(), "A hat is a stylish and functional accessory designed to shield the head from the elements while adding a touch of personality to any outfit. Crafted from materials such as wool, cotton, straw, or synthetic blends, hats come in a variety of shapes and designs, from wide-brimmed sun hats to snug beanies and classic fedoras. They offer versatile use, providing protection from sun, rain, or cold while serving as a fashionable statement piece. Whether for outdoor adventures, formal occasions, or casual outings, a hat combines practicality and style, making it a timeless wardrobe essential", Map.of("key1", "value1"));
            Document document2 = new Document(UUID.randomUUID().toString(), "Wool socks are premium, cozy footwear accessories designed to provide exceptional warmth, comfort, and moisture-wicking properties. Made from natural wool fibers, they are ideal for keeping feet insulated in cold weather while remaining breathable in warmer conditions. These socks are soft, durable, and naturally odor-resistant, making them perfect for everyday wear, outdoor adventures, or lounging at home. With their ability to regulate temperature and cushion feet, wool socks offer unparalleled comfort, making them an essential addition to any wardrobe, whether for hiking, working, or simply relaxing.", Map.of("key2", "value2"));
            Document document3 = new Document(UUID.randomUUID().toString(), "Shoes are versatile footwear designed to protect and comfort the feet while enabling effortless movement and style. They come in a wide range of designs, materials, and functions, catering to various activities, from formal occasions to rugged outdoor adventures. Crafted from durable materials such as leather, canvas, or synthetic blends, shoes provide support, cushioning, and stability through features like rubber soles, padded insoles, and secure fastenings. Available in diverse styles such as sneakers, boots, sandals, and dress shoes, they blend functionality with aesthetic appeal, making them a staple for every wardrobe", Map.of("key3", "value3"));
            vectorStore.add(List.of(document1, document2, document3));
            CosmosChatMemory chatMemory = new CosmosChatMemory(container);

            ArrayList<Object> timeTellerTools = new ArrayList<>();
            timeTellerTools.add(new DateTimeTools());

            ArrayList<Object> tellJokeTools = new ArrayList<>();
            tellJokeTools.add(new TellJokeTools());

            ArrayList<Object> mathTools = new ArrayList<>();
            mathTools.add(new MathAssistantTools());

            ArrayList<Object> productSearchTools = new ArrayList<>();
            productSearchTools.add(new ProductSearchTools(vectorStore));

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
            agentOrchestrator.registerAgent(new Agent("productsearch",
                    "You can help the user search for products. Ask for what products the user is interested in. Call productSearch() and pass in the user's question as an argument.",
                    productSearchTools, List.of()));

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
