package com.demo.ai.app.controller;

import com.demo.ai.app.tools.CourseSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    @Qualifier("openAiEmbeddingModel")
    private EmbeddingModel embeddingModel;

    @Autowired
    @Qualifier("redisVectorStore")
    private VectorStore vectorStore;

    private ChatClient chatClient;

    @Autowired
    private CourseSearchTool courseSearchTool;

    public RagController(OpenAiChatModel chatModel) {

        this.chatClient = ChatClient.create(chatModel);
    }


    @PostMapping("/embeddings")
    public float[] embeddings(@RequestParam String text) {
        return embeddingModel.embed(text);
    }

    // --> Spring boot app / get-course --> advisors --> pg vector table
    // --> cosine similarity search --> top 4 chunks
    // --> prompt template --> chat model --> answer
    @GetMapping("/get-course")
    public String getAnswerWithRag(@RequestParam String query) {
        return chatClient
                .prompt(query)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .call()
                .content();
    }

    @GetMapping("/search-without-llm")
    public List<Document> searchWithOutLLM(@RequestParam String query, @RequestParam(defaultValue = "4") int topK) {
        return vectorStore.similaritySearch(SearchRequest
                .builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.7)
                .build());
    }

    @GetMapping("/get-course-controlled")
    public String getAnswerWithRagAndLLM(@RequestParam String query) {
        return chatClient
                .prompt(query)
                .system(
                        """
                                     You are ABC Tech course advisor.
                                     Answer using only the context provided from the course catalog.
                                      If the answer is not in the context, say
                                      "I don't have that course information right now."
                                       Always mention course name, price, duration, and level when available.
                                """
                )
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .query(query)
                                .topK(3)
                                .similarityThreshold(0.7)
                                .build())
                        .build())
                .call()
                .content();
    }


}
