package com.cosmos.multiagent.agent.memory;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.cosmos.multiagent.agent.models.ChatMessage;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import java.util.UUID;

public class CosmosChatMemory implements ChatMemory {
    private final CosmosAsyncContainer container;

    public CosmosChatMemory(CosmosAsyncClient cosmosAsyncClient, String databaseName) {
        CosmosAsyncDatabase db = cosmosAsyncClient.getDatabase(databaseName);
        db.createContainerIfNotExists("ChatMemory", "/conversationId").block();
        this.container = db.getContainer("ChatMemory");
    }

    public void createChatMemoryContainer(CosmosAsyncDatabase db) {
        db.createContainerIfNotExists("ChatMemory", "/conversationId").block();
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Mono<CosmosItemResponse<Object>>> tasks = new ArrayList<>();

        for (Message message : messages) {
            if (!(message instanceof ChatMessage)) {
                continue; // skip unknown message types
            }
            ChatMessage chatMessage = (ChatMessage) message;
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", UUID.randomUUID().toString());
            doc.put("conversationId", conversationId);
            doc.put("role", chatMessage.getRole());
            doc.put("text", chatMessage.getText());

            tasks.add(container.createItem(doc, new PartitionKey(conversationId), new CosmosItemRequestOptions()));
        }

        // Block until all items are created
        Mono.when(tasks).block();
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String query = "SELECT * FROM c WHERE c.conversationId = @conversationId ORDER BY c._ts ASC";

        SqlParameter param = new SqlParameter("@conversationId", conversationId);
        SqlQuerySpec querySpec = new SqlQuerySpec(query, Arrays.asList(param));

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions()
                .setPartitionKey(new PartitionKey(conversationId));

        CosmosPagedFlux<Map<String, Object>> results = container.queryItems(
                querySpec,
                options,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        List<Map<String, Object>> messageDocs = results.byPage()
                .flatMapIterable(FeedResponse::getResults)
                .collectList()
                .block();

        if (messageDocs == null) return Collections.emptyList();

        if (lastN > 0 && messageDocs.size() > lastN) {
            messageDocs = messageDocs.subList(messageDocs.size() - lastN, messageDocs.size());
        }

        return messageDocs.stream()
                .map(m -> new ChatMessage((String) m.get("role"), (String) m.get("text")))
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        String query = "SELECT c.id FROM c WHERE c.conversationId = @conversationId";
        SqlParameter param = new SqlParameter("@conversationId", conversationId);
        SqlQuerySpec querySpec = new SqlQuerySpec(query, Arrays.asList(param));

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions()
                .setPartitionKey(new PartitionKey(conversationId));

        CosmosPagedFlux<Map<String, Object>> results = container.queryItems(
                querySpec,
                options,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        List<Map<String, Object>> items = results.byPage()
                .flatMapIterable(FeedResponse::getResults)
                .collectList()
                .block();

        if (items == null || items.isEmpty()) return;

        List<CosmosItemOperation> operations = items.stream()
                .map(item -> CosmosBulkOperations.getDeleteItemOperation(
                        (String) item.get("id"),
                        new PartitionKey(conversationId)))
                .collect(Collectors.toList());

        container.executeBulkOperations(Flux.fromIterable(operations)).then().block();
    }
}
