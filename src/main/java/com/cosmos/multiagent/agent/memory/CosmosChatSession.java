// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.cosmos.multiagent.agent.memory;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKeyBuilder;
import com.azure.cosmos.models.PartitionKeyDefinition;
import com.azure.cosmos.models.PartitionKeyDefinitionVersion;
import com.azure.cosmos.models.PartitionKind;
import com.azure.cosmos.models.ThroughputProperties;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.cosmos.multiagent.agent.models.ChatSession;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CosmosChatSession {
    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(CosmosChatSession.class);

    private final CosmosAsyncContainer container;

    public CosmosAsyncContainer getContainer() {
        return this.container;
    }

    public CosmosChatSession(CosmosAsyncClient cosmosAsyncClient, String databaseName) {
        String containerName = "ChatSession";
        CosmosAsyncDatabase db = cosmosAsyncClient.getDatabase(databaseName);
        // List of partition keys, in hierarchical order. You can have up to three levels of keys.
        List<String> subpartitionKeyPaths = new ArrayList<String>();
        subpartitionKeyPaths.add("/tenantId");
        subpartitionKeyPaths.add("/userId");
        subpartitionKeyPaths.add("/sessionId");

        //Create a partition key definition object with Kind ("MultiHash") and Version V2
        PartitionKeyDefinition subpartitionKeyDefinition = new PartitionKeyDefinition();
        subpartitionKeyDefinition.setPaths(subpartitionKeyPaths);
        subpartitionKeyDefinition.setKind(PartitionKind.MULTI_HASH);
        subpartitionKeyDefinition.setVersion(PartitionKeyDefinitionVersion.V2);

        // Create a container properties object
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, subpartitionKeyDefinition);

        // Create a throughput properties object
        ThroughputProperties throughputProperties = ThroughputProperties.createManualThroughput(400);
        db.createContainerIfNotExists(containerProperties, throughputProperties).block();
        this.container = db.getContainer(containerName);
    }

    public String createSessionId(String userId, String tenantId) {

        String id = UUID.randomUUID().toString();
        ChatSession session = new ChatSession();
        session.setId(id);
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setSessionId(id);
        session.setActiveAgent("unknown");
        session.setName("New Session");
        CosmosItemResponse<ChatSession> response  = container.createItem(session).block();
        return response.getItem().getId();
    }
    public void patchActiveAgent(String sessionId, String userId, String tenantId, String agentName) {
        try {
            // Define patch operations (e.g., update "name" and add "status")
            CosmosPatchOperations patchOps = CosmosPatchOperations.create()
                    .replace("/activeAgent", agentName);

            // Perform patch
            CosmosItemResponse<ChatSession> response = container.patchItem(
                    sessionId,
                    new PartitionKeyBuilder().add(tenantId).add(userId).add(sessionId).build(),
                    patchOps,
                    ChatSession.class
            ).block();

            ChatSession updatedItem = response.getItem();
            logger.info("Patched item: {}", updatedItem.getId());
        } catch (CosmosException e) {
            logger.error("Patch failed: {}", e.getMessage());
        }
    }
    public String getActiveAgent(String sessionId, String userId, String tenantId) {
        return container.readItem(sessionId, new PartitionKeyBuilder().add(tenantId).add(userId).add(sessionId).build(), ChatSession.class).block().getItem().getActiveAgent();
    }

    public List<ChatSession> getSessions(String userId, String tenantId) {
        List<ChatSession> sessions = new ArrayList<>();
        CosmosPagedFlux<ChatSession> sessionResults = this.container.queryItems("SELECT * FROM c WHERE c.userId = '" + userId + "' AND c.tenantId = '" + tenantId + "'", new CosmosQueryRequestOptions(), ChatSession.class);

        sessionResults.byPage().flatMap(page -> {
            for (ChatSession session : page.getResults())
            {
                sessions.add(session);
            }
            return Flux.empty();
        }).blockLast();
        return sessions;

    }

    public void deleteSession(String sessionId, String userId, String tenantId) {
        container.deleteItem(sessionId, new PartitionKeyBuilder().add(tenantId).add(userId).add(sessionId).build()).block();
    }

}
