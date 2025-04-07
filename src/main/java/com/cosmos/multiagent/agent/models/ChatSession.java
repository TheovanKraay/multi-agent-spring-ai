// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.cosmos.multiagent.agent.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatSession {
    @JsonProperty("id")
    private String id;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    private String sessionId;
    private String userId;
    private String tenantId;
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
