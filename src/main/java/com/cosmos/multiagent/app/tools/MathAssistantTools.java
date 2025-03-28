package com.cosmos.multiagent.app.tools;

import org.springframework.ai.tool.annotation.Tool;

public class MathAssistantTools {

    @Tool(description = "Add two numbers together")
    String addNumbers(int a, int b) {
        System.out.println("In MathAssistantTools");
        return "The sum of " + a + " and " + b + " is " + (a + b) + ".";
    }

}
