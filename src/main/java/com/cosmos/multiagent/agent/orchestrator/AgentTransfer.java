package com.cosmos.multiagent.agent.orchestrator;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.PartitionKey;
import com.cosmos.multiagent.agent.memory.CosmosChatSession;
import com.cosmos.multiagent.agent.models.ChatSession;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.List;

public class AgentTransfer {

    private CosmosChatSession chatSession;
    private String sessionId;
    private String userId;
    private String tenantId;

    @Tool(description = "Get routable agents")
    public List<String> getRoutableAgents() {
        return routableAgents;
    }

    public Object setRoutableAgents(List<String> routableAgents) {
        this.routableAgents = routableAgents;
        return null;
    }

    private List<String > routableAgents = new ArrayList<>();
    public AgentTransfer(CosmosChatSession chatSession, String sessionId, String userId, String tenantId) {
        this.chatSession = chatSession;
        this.sessionId = sessionId;
        this.userId = userId;
        this.tenantId = tenantId;
    }

    @Tool(description = "Tell a joke")
    String transferAgent(String agentName) {
        chatSession.patchActiveAgent(this.sessionId, this.userId, this.tenantId,  agentName);
/*        try {
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
     }*/
        return "Agent transferred to " + agentName;}

}
