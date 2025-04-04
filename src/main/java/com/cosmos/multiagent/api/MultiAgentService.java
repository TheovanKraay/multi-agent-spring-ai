package com.cosmos.multiagent.api;

import com.azure.cosmos.CosmosAsyncClient;
import com.cosmos.multiagent.agent.Agent;
import com.cosmos.multiagent.agent.memory.CosmosChatMemory;
import com.cosmos.multiagent.agent.memory.CosmosChatSession;
import com.cosmos.multiagent.agent.models.ChatSession;
import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import com.cosmos.multiagent.agent.orchestrator.AgentTransfer;
import com.cosmos.multiagent.repository.UsersRepository;
import com.cosmos.multiagent.api.tools.DateTimeTools;
import com.cosmos.multiagent.api.tools.MathAssistantTools;
import com.cosmos.multiagent.api.tools.NotifyCustomer;
import com.cosmos.multiagent.api.tools.ProductSearchTools;
import com.cosmos.multiagent.api.tools.TellJokeTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.Message;
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

    @Autowired
    private UsersRepository usersRepository;

    private static final String COSMOS_DB_NAME = "MultiAgentDb";

    private AgentOrchestrator orchestrator;
    private CosmosChatMemory chatMemory;
    private CosmosChatSession chatSession;



    @PostConstruct
    public void initialize() {
        chatMemory = new CosmosChatMemory(cosmosAsyncClient, COSMOS_DB_NAME);
        chatSession = new CosmosChatSession(cosmosAsyncClient, COSMOS_DB_NAME);
        orchestrator = new AgentOrchestrator(chatSession, chatMemory, chatModel);

        List<String> allAgents = List.of("timeteller", "joketeller", "mathassistant", "productsearch", "Refunds");

        Agent timeAgent = new Agent("timeteller",
                "You are a time teller assistant. Call getCurrentDateTime()\"+\n" +
                "\"You can also transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "\"determine which agents you can call, then transferAgent() passing the appropriate agent\" +\n" +
                "\"for the question being asked, plus the tenantId, userId, and sessionId.",
                List.of(
                new DateTimeTools()),
                agentTransfersAllowed("timeteller", allAgents)
        );

        Agent jokeAgent = new Agent("joketeller",
                "You are a funny assistant that can tell the user a joke. Call TellJokeTools()\" +\n" +
                "\"You can also transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "\"determine which agents you can call, then transferAgent() passing the appropriate agent\" +\n" +
                "\"for the question being asked.",
                List.of(new TellJokeTools()),
                agentTransfersAllowed("joketeller", allAgents)
        );

        Agent mathAgent = new Agent("mathassistant",
                "You can help the user with sums. Ask user which numbers they want to add together. Call addNumbers()\"+\n" +
                "\"You can also transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "\"determine which agents you can call, then call transferAgent() passing the appropriate agent\" +\n" +
                "\"for the question being asked.",
                List.of(new MathAssistantTools()),
                agentTransfersAllowed("mathassistant", allAgents)
        );

        Agent productAgent = new Agent("productsearch",
                "You can help the user search for products. Ask for what products the user is " +
                "interested in. Call productSearch() and pass in the user's question as an argument.\"+\n" +
                "\"You can also transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "\"determine which agents you can call, then call transferAgent() passing the appropriate agent\" +\n" +
                "\"for the question being asked",
                List.of(new ProductSearchTools(vectorStore)),
                agentTransfersAllowed("productsearch", allAgents)
        );

        Agent refundsAgent = new Agent("Refunds",
        "You are a refund agent.\n" +
                "    For now all you can do is arrange a refund or transfer the user to another agent by calling getRoutableAgents() to \" +\n" +
                "    determine which agents you can call, then call transferAgent() passing the appropriate agent\" +\n" +
                "    for the question being asked" +
                "    If the user asks for a refund, you must ask what their preferred method of notification is and user id in one message.\n" +
                "    Then call the notifyCustomer() method passing userId and method values to it. " +
                "    You must return the response from notifyCustomer() to the user \n" ,
                List.of(new NotifyCustomer(usersRepository)),
                agentTransfersAllowed("Refunds", allAgents)
        );

        orchestrator.registerAgent(timeAgent);
        orchestrator.registerAgent(jokeAgent);
        orchestrator.registerAgent(mathAgent);
        orchestrator.registerAgent(productAgent);
        orchestrator.registerAgent(refundsAgent);
    }
    /**
     * This method returns a list of agents that the current agent can transfer to.
     *
     * @param currentAgent The name of the current agent.
     * @param allAgents    A list of all available agents.
     * @return A list of agent names that the current agent can transfer to.
     */
    private List<String> agentTransfersAllowed(String currentAgent, List<String> allAgents) {
        return allAgents.stream().filter(a -> !a.equals(currentAgent)).toList();
    }

    public AgentOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public CosmosChatSession getChatSession() {
        return chatSession;
    }

    public CosmosChatMemory getChatMemory() {
        return chatMemory;
    }

    public List<ChatSession> getChatSessions(String userId, String tenantId) {
        return chatSession.getSessions(userId, tenantId);
    }

    public List<Message> getChatSession(String sessionId, int lastN) {
        return chatMemory.get(sessionId, lastN);
    }

    public String getChatSessionId(String userId, String tenantId) {
        return chatSession.createSessionId(userId, tenantId);
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