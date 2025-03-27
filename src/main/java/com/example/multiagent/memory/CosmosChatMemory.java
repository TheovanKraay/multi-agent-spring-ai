package com.example.multiagent.memory;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.PartitionKey;
import com.example.multiagent.agent.model.ChatMessage;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CosmosChatMemory implements ChatMemory {
    private final CosmosContainer container;

    public CosmosChatMemory(CosmosContainer container) {
        this.container = container;
    }
    @Override
    public void add(String conversationId, List<Message> messages) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", conversationId);
        doc.put("messages", messages);
        container.upsertItem(doc);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        try {
            Map<String, Object> doc = container.readItem(
                    conversationId,
                    new PartitionKey(conversationId),
                    Map.class
            ).getItem();

            System.out.println("DEBUG >>> doc = " + doc);

            List<Map<String, Object>> rawMessages = (List<Map<String, Object>>) doc.get("messages");

            List<Message> messages = rawMessages.stream()
                    .map(m -> new ChatMessage(
                            (String) m.get("role"),
                            (String) m.get("text")
                    ))
                    .collect(Collectors.toList());

            return messages;
        } catch (CosmosException e) {
            return new ArrayList<>();
        }
    }


    @Override
    public void clear(String conversationId) {

    }
}
