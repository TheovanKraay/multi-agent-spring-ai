package com.cosmos.multiagent.agent;

import java.util.List;

public class Agent {
    private final String name;
    private final String systemPrompt;
    private final List<Object> tools;

    public Agent(String name, String systemPrompt, List<Object> tools) {
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.tools = tools;
    }

    public String getName() {
        return name;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public List<Object> getTools() {
        return tools;
    }
}