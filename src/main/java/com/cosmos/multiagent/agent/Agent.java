// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.cosmos.multiagent.agent;

import java.util.List;

public record Agent(
        String name,
        String systemPrompt,
        List<Object> tools,
        List<String> routableAgents
) {}
