package com.demo.ai.app.controller;

import com.demo.ai.app.tools.DateTimeTool;
import com.demo.ai.app.tools.NewsTool;
import com.demo.ai.app.tools.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class DemoController {

    private OpenAiChatModel openAiChatModel;

    private ChatClient chatClient;

    private ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

    @Autowired
    private DateTimeTool dateTimeTool;

    @Autowired
    private NewsTool newsTool;

    @Autowired
    private WeatherTool weatherTool;

    String conversationId = UUID.randomUUID().toString();


//    public DemoController(OpenAiChatModel openAiChatModel){
//        this.openAiChatModel = openAiChatModel;
//    }

//    public DemoController(OpenAiChatModel openAiChatModel) {
//        this.chatClient = ChatClient.create(openAiChatModel);
//    }

    public DemoController(ChatClient.Builder builder) {
        //this.chatClient = builder.build();
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .build())
                .defaultAdvisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .build();

    }


    @GetMapping("/info/{prompt}")
    public String getInfo(@PathVariable String prompt) {
        // String response = openAiChatModel.call(prompt);

//        String response = chatClient
//                .prompt(prompt)
//                .call()
//                .content();

        ChatResponse chatResponse = chatClient
                .prompt(prompt)
                .tools(dateTimeTool, newsTool, weatherTool)
                .call()
                .chatResponse();

        System.out.println(chatResponse.getMetadata().getModel());

        return chatResponse.getResult().getOutput().getText();
    }

    @GetMapping("assistant/planMyDay")
    public String planMyDay(@RequestParam String prompt) {

        return chatClient.prompt(prompt)
                .system(
                        """
                                You are a smart personal assistant
                                You have access to tools for date and time, weather, and latest news.
                                Use whichever tools you need to answer the user clearly and completely.
                                Always give a helpful, well-organized response.
                                """
                )
                .tools(dateTimeTool, newsTool, weatherTool)
                .call()
                .content();
    }

    @GetMapping("/recommend/movie")
    public String recommend(@RequestParam String type, @RequestParam String year, @RequestParam String lang) {

        String templete = """
                       I want to watch a {type} movie tonight with good rating,
                looking for movies around this year {year}.
                The language im looking for is {lang}.
                Suggest one specific movie and tell me the cast and length of the movie.
                
                response format should be:
                1. Movie Name
                2. basic plot
                3. cast
                4. length
                5. IMDB rating
                """;
        PromptTemplate promptTemplate = new PromptTemplate(templete);
        Prompt prompt = promptTemplate.create(Map.of("type", type,
                "year", year,
                "lang", lang));
        String response = chatClient.prompt(prompt).call().content();
        return response;
    }
}
