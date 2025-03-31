package com.cosmos.multiagent.api;

import com.azure.cosmos.CosmosAsyncClient;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import com.cosmos.multiagent.agent.memory.CosmosChatSession;
import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import com.cosmos.multiagent.agent.orchestrator.AgentTransfer;
import com.cosmos.multiagent.api.tools.DateTimeTools;
import com.cosmos.multiagent.api.tools.MathAssistantTools;
import com.cosmos.multiagent.api.tools.ProductSearchTools;
import com.cosmos.multiagent.api.tools.TellJokeTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class MultiAgentService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private CosmosAsyncClient cosmosAsyncClient;

    @Autowired
    private ChatModel chatModel;

    private static final String COSMOS_DB_NAME = "MultiAgentDb";

    private AgentOrchestrator orchestrator;
    private CosmosChatMemory chatMemory;
    private CosmosChatSession chatSession;

    @PostConstruct
    public void initialize() {
        chatMemory = new CosmosChatMemory(cosmosAsyncClient, COSMOS_DB_NAME);
        chatSession = new CosmosChatSession(cosmosAsyncClient, COSMOS_DB_NAME);

        String dummyUserId = "system";
        String dummyTenantId = "default";
        String dummySessionId = chatSession.createSessionId(dummyUserId, dummyTenantId);

        orchestrator = new AgentOrchestrator(dummySessionId, dummyUserId, dummyTenantId, chatSession, chatMemory, chatModel);

        List<String> allAgents = List.of("timeteller", "joketeller", "mathassistant", "productsearch");

        Agent timeAgent = new Agent("timeteller",
                "You are a time teller assistant. Call getCurrentDateTime()\"+\n" +
                "\"You can also transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "\"determine which agents you can call, then transferAgent() passing the appropriate agent\" +\n" +
                "\"for the question being asked.", List.of(
                new DateTimeTools(),
                agentTransfersAllowed("timeteller", allAgents, dummyUserId, dummyTenantId, dummySessionId)
        ));

        Agent jokeAgent = new Agent("joketeller",
                "You are a funny assistant that can tell the user a joke. Call TellJokeTools()\" +\n" +
                "\"You can also transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "\"determine which agents you can call, then transferAgent() passing the appropriate agent\" +\n" +
                "\"for the question being asked.",
                List.of(new TellJokeTools(),
                agentTransfersAllowed("joketeller", allAgents, dummyUserId, dummyTenantId, dummySessionId)
        ));

        Agent mathAgent = new Agent("mathassistant",
                "You can help the user with sums. Ask user which numbers they want to add together. " +
                        "Call addNumbers()\"+\n" +
                "\"You can also transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "\"determine which agents you can call, them transferAgent() passing the appropriate agent\" +\n" +
                "\"for the question being asked.",
                List.of(new MathAssistantTools(),
                agentTransfersAllowed("mathassistant", allAgents, dummyUserId, dummyTenantId, dummySessionId)
        ));

        Agent productAgent = new Agent("productsearch",
                "You can help the user search for products. Ask for what products the user is " +
                "interested in. Call productSearch() and pass in the user's question as an argument.",
                List.of(new ProductSearchTools(vectorStore),
                agentTransfersAllowed("productsearch", allAgents, dummyUserId, dummyTenantId, dummySessionId)
        ));

        orchestrator.registerAgent(timeAgent);
        orchestrator.registerAgent(jokeAgent);
        orchestrator.registerAgent(mathAgent);
        orchestrator.registerAgent(productAgent);
    }

    private AgentTransfer agentTransfersAllowed(String currentAgent, List<String> allAgents, String userId, String tenantId, String sessionId) {
        AgentTransfer transfer = new AgentTransfer(chatSession, sessionId, userId, tenantId);
        transfer.setRoutableAgents(allAgents.stream().filter(a -> !a.equals(currentAgent)).toList());
        return transfer;
    }

    public AgentOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public CosmosChatSession getChatSession() {
        return chatSession;
    }

    public void dataLoad() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = new ClassPathResource("data/products.json").getInputStream();

        List<Map<String, Object>> products = mapper.readValue(inputStream, new TypeReference<>() {});
        List<Document> documents = new ArrayList<>();

        for (Map<String, Object> product : products) {
            StringBuilder content = new StringBuilder();
            content.append("Product ID: ").append(product.get("product_id")).append("\n");
            content.append("Product Name: ").append(product.get("product_name")).append("\n");
            content.append("Category: ").append(product.get("category")).append("\n");
            content.append("Description: ").append(product.get("product_description")).append("\n");
            content.append("Price: ").append(product.get("price"));

            Document doc = new Document(content.toString());
            doc.getMetadata().put("product_id", product.get("product_id").toString());
            doc.getMetadata().put("product_name", product.get("product_name").toString());
            documents.add(doc);
        }
        vectorStore.add(documents);
    }
}