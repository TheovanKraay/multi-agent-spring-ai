package com.cosmos.multiagent.api.tools;

import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;

public class DateTimeTools {
    private static final org.slf4j.Logger
    logger = LoggerFactory.getLogger(DateTimeTools.class);
    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        logger.info("Called getCurrentDateTime() tool");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}
