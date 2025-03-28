package com.cosmos.multiagent.app.tools;

import org.springframework.ai.tool.annotation.Tool;

public class TellJokeTools {

    @Tool(description = "Tell a joke")
    String tellJoke() {
        System.out.println("In TellJokeTools");
        return "Why did the chicken cross the road? To get to the other side!";
    }

}
