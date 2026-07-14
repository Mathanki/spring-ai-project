package com.demo.ai.app.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WeatherTool {

    RestTemplate restTemplate = new RestTemplate();

    @Tool(description = "Get current weather info")
    private String getCurrentWeather(
            @ToolParam(description = "City name for eg NewYork, NewJersy") String location) {
        try {
            System.out.println("Weather Tool called for location " + location);
            String url = "https://wttr.in/" + location + "?format=3";
            return restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

}
