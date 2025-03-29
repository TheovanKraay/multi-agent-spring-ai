package com.cosmos.multiagent.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatSession {
    @JsonProperty("id")
    private String id;
    private String name;
    private String activeAgent;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getActiveAgent() {
        return activeAgent;
    }
    public void setActiveAgent(String activeAgent) {
        this.activeAgent = activeAgent;
    }

    public String toString() {
        return "Session: " + id + ", " + name + ", " + activeAgent;
    }
}
