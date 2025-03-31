package com.cosmos.multiagent.api.tools;

import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

public class MathAssistantTools {
    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(MathAssistantTools.class);
    @Tool(description = "Add two numbers together")
    String addNumbers(int a, int b) {
        logger.info("Called addNumbers() tool");
        return "The sum of " + a + " and " + b + " is " + (a + b) + ".";
    }

}
