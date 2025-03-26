package com.example.multiai.cli;

import com.azure.cosmos.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.example.multiai.agent.Agent;
import com.example.multiai.manager.AgentManager;
import com.example.multiai.memory.CosmosMemoryStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@SpringBootApplication(scanBasePackages = "com.example.multiai")
public class ChatCliApp {
    @Autowired
    private ChatClient chatClient;
    public static void main(String[] args) {
        System.out.println("DEBUG >>> AZURE_OPENAI_APIKEY = " + System.getenv("AZURE_OPENAI_APIKEY"));
        System.out.println("DEBUG >>> AZURE_OPENAI_ENDPOINT = " + System.getenv("AZURE_OPENAI_ENDPOINT"));

        ConfigurableApplicationContext context = SpringApplication.run(ChatCliApp.class, args);

        CosmosClient cosmosClient = new CosmosClientBuilder()
                .endpoint(System.getenv("AZURE_COSMOSDB_ENDPOINT"))
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        CosmosDatabase db = cosmosClient.getDatabase("MultiAgentDb");
        CosmosContainer container = db.getContainer("chatMemory");

        CosmosMemoryStore memoryStore = new CosmosMemoryStore(container);
        ChatModel chatModel = context.getBean(ChatModel.class);

        ArrayList<Object> timeTellerTools = new ArrayList<>();
        timeTellerTools.add(new DateTimeTools());

        ArrayList<Object> tellJokeTools = new ArrayList<Object>();
        tellJokeTools.add(new TellJokeTools());

        AgentManager manager = new AgentManager(UUID.randomUUID().toString(), memoryStore, chatModel);
        Agent agent1 = new Agent("timeteller", "You are a time teller assistant. Call getCurrentDateTime()", timeTellerTools, List.of());
        Agent agent2 = new Agent("joketeller", "You are a funny assistant that can tell the user a joke. Call TellJokeTools()", tellJokeTools, List.of());
        manager.registerAgent(agent1);
        manager.registerAgent(agent2);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("User: ");
            String input = scanner.nextLine();
            String reply = manager.handleUserInput(input);
            System.out.println("AI: " + reply);
        }


    }

    static class DateTimeTools {

        @Tool(description = "Get the current date and time in the user's timezone")
        String getCurrentDateTime() {
            System.out.println("In DateTimeTools");
            return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
        }

    }

    static class TellJokeTools {

        @Tool(description = "Tell a joke")
        String tellJoke() {
            System.out.println("In TellJokeTools");
            return "Why did the chicken cross the road? To get to the other side!";
        }

    }

}
