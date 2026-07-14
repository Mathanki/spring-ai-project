package com.demo.ai.app.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NewsTool {

    RestTemplate restTemplate = new RestTemplate();

    @Tool(description = "Get latest news")
    public String getLatestNews(String topic){
        String apiKey = "d48d1c8881204a08a7079b7f1c8fe80c";
        String apiUrl = "https://newsapi.org/v2/everything?q=" + topic + "&apiKey=" + apiKey;
        String result = restTemplate.getForObject(apiUrl, String.class);
        return result;
    }
}
