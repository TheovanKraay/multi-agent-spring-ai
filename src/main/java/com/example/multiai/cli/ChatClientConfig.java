package com.example.multiai.cli;

import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    private final AzureOpenAiChatModel chatModel;

    public ChatClientConfig(AzureOpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(chatModel).build();
    }
}
