package com.cosmos.multiagent.agent.orchestrator;

import com.cosmos.multiagent.agent.memory.CosmosChatSession;
import org.springframework.ai.tool.annotation.Tool;
import java.util.ArrayList;
import java.util.List;

public class AgentTransfer {

    private CosmosChatSession chatSession;

    @Tool(description = "Get routable agents")
    public List<String> getRoutableAgents() {
        return routableAgents;
    }

    public Object setRoutableAgents(List<String> routableAgents) {
        this.routableAgents = routableAgents;
        return null;
    }

    private List<String > routableAgents = new ArrayList<>();
    public AgentTransfer(CosmosChatSession chatSession) {
        this.chatSession = chatSession;
    }

    @Tool(description = "Transfer agent to another agent")
    String transferAgent(String agentName, String sessionId, String userId, String tenantId) {
        chatSession.patchActiveAgent(sessionId, userId, tenantId, agentName);
        return "Agent transferred to " + agentName;}

}
