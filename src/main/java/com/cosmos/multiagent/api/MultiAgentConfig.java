package com.cosmos.multiagent.api;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.cosmosdb.CosmosDBVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MultiAgentConfig {

    @Bean
    public CosmosAsyncClient cosmosAsyncClient() {
        return new CosmosClientBuilder()
                .endpoint(System.getenv("AZURE_COSMOSDB_ENDPOINT"))
                .credential(new DefaultAzureCredentialBuilder().build())
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();
    }

    @Bean
    public ChatClient chatClient(AzureOpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }

    @Bean
    public VectorStore vectorStore(
            ObservationRegistry observationRegistry,
            CosmosAsyncClient cosmosAsyncClient,
            EmbeddingModel embeddingModel
    ) {
        return CosmosDBVectorStore.builder(cosmosAsyncClient, embeddingModel)
                .databaseName("MultiAgentDb")
                .containerName("Products")
                .metadataFields(List.of("product_id"))
                .partitionKeyPath("/id")
                .vectorStoreThroughput(1000)
                .vectorDimensions(1536)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .observationRegistry(observationRegistry)
                .build();
    }
}
