package com.cosmos.multiagent.api;

import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class MultiAgentController {

    @Autowired
    private MultiAgentService multiAgentService;

    @PostMapping
    public List<Message> handleUserInput(@RequestParam String input) {
        AgentOrchestrator orchestrator = multiAgentService.getOrchestrator();
        return orchestrator.handleUserInput(input);
    }

    @GetMapping("/data")
    public void dataLoad() throws IOException {
        multiAgentService.dataLoad();
    }

}