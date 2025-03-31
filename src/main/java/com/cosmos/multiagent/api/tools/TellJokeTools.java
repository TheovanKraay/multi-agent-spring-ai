package com.cosmos.multiagent.api.tools;

import com.cosmos.multiagent.agent.orchestrator.AgentOrchestrator;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

public class TellJokeTools {
    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(TellJokeTools.class);
    @Tool(description = "Tell a joke")
    String tellJoke() {
        logger.info("Called tellJoke() tool");
        return "Why did the chicken cross the road? To get to the other side!";
    }

}
