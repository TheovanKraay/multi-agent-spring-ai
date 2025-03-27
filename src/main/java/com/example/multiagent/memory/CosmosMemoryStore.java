/*
package com.example.multiai.memory;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.example.multiai.agent.model.ChatMessage;

import java.util.*;

public class CosmosMemoryStore {
    private final CosmosContainer container;

    public CosmosMemoryStore(CosmosContainer container) {
        this.container = container;
    }

    public void saveMemory(String sessionId, List<ChatMessage> messages) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", sessionId);
        doc.put("messages", messages);
        container.upsertItem(doc);
    }

    public List<ChatMessage> loadMemory(String sessionId) {
        try {
            Map doc = container.readItem(sessionId, new PartitionKey(sessionId), Map.class).getItem();
            System.out.println("DEBUG >>> doc = " + doc);
            return (List<ChatMessage>) doc.get("messages");
        } catch (CosmosException e) {
            return new ArrayList<>();
        }
    }
}*/
