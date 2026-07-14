package com.demo.ai.app.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CourseSearchTool {

    @Autowired
    @Qualifier("redisVectorStore")
    private VectorStore vectorStore;

    @Tool(description = "Search ABC Tech course catalog for courses matching a topic, technology, or skill. " +
            "Use this whenever the user asks about courses, prices, durations, or what ABC Tech offers.")
    public String searchCourse(@ToolParam(description = "The topic to search like Java, AI, Devops") String topic) {
        System.out.println("CourseSearchTool called for topic " + topic);
        List<Document> result = vectorStore.similaritySearch(SearchRequest.builder()
                .query(topic)
                .topK(6)
                .similarityThreshold(0.7)
                .build());

        return result.stream().map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

}
