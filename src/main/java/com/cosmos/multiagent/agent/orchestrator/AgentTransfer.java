package com.cosmos.multiagent.agent.orchestrator;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.PartitionKey;
import com.cosmos.multiagent.agent.model.ChatSession;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.List;

public class AgentTransfer {

    private CosmosAsyncContainer sessionContainer;
    private String sessionId;

    @Tool(description = "Get routable agents")
    public List<String> getRoutableAgents() {
        return routableAgents;
    }

    public Object setRoutableAgents(List<String> routableAgents) {
        this.routableAgents = routableAgents;
        return null;
    }

    private List<String > routableAgents = new ArrayList<>();
    public AgentTransfer(CosmosAsyncContainer sessionContainer, String sessionId) {
        this.sessionContainer = sessionContainer;
        this.sessionId = sessionId;
    }

    @Tool(description = "Tell a joke")
    String transferAgent(String agentName) {
        try {
            // Define patch operations (e.g., update "name" and add "status")
            CosmosPatchOperations patchOps = CosmosPatchOperations.create()
                    .replace("/activeAgent", agentName);

            // Perform patch
            CosmosItemResponse<ChatSession> response = sessionContainer.patchItem(
                    sessionId,
                    new PartitionKey(sessionId),
                    patchOps,
                    ChatSession.class
            ).block();

            ChatSession updatedItem = response.getItem();
            System.out.println("Patched item: " + updatedItem.getId());
        } catch (CosmosException e) {
            System.err.println("Patch failed: " + e.getMessage());
     }
        return "Agent transferred to " + agentName;}

}
