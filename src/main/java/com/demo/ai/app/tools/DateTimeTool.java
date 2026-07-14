package com.demo.ai.app.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class DateTimeTool {

    @Tool(description = "Get current date and time for user's time zone")
    public String getZoneCurrentDateTime(String timeZone) {
        System.out.println("DateTimeTool called for timeZone " + timeZone);
        //return LocalDateTime.now().toString();
        return ZonedDateTime.now(ZoneId.of(timeZone)).toString();
    }

    @Tool(description = "Get current date and time")
    public String getCurrentDateTime() {
        System.out.println("Default time zone");
        //return LocalDateTime.now().toString();
        return ZonedDateTime.now(ZoneId.systemDefault()).toString();
    }
}
