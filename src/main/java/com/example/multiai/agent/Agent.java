package com.example.multiai.agent;

import java.util.List;

public class Agent {
    private final String name;
    private final String systemPrompt;
    private final List<Object> tools;
    private final List<String> routableAgents;

    public Agent(String name, String systemPrompt, List<Object> tools, List<String> routableAgents) {
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.tools = tools;
        this.routableAgents = routableAgents;
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

    public List<String> getRoutableAgents() {
        return routableAgents;
    }
}