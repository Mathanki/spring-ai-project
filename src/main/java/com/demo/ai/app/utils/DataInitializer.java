package com.demo.ai.app.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer {

    private final VectorStore vectorStore;

    // Use @Qualifier to target your custom vectorStore bean precisely
    public DataInitializer(@Qualifier("redisVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     @PostConstruct public void initData()
     {
     TextReader textReader = new TextReader(new ClassPathResource("Courses.txt"));
     TokenTextSplitter splitter = TokenTextSplitter
     .builder()
     .withChunkSize(500)
     .build();
     List<Document> docs = splitter.split(textReader.get());
     vectorStore.add(docs);
     }
     **/
    //aviod deuplicate embeding each time

    /**
     * @PostConstruct public void initData()
     * {
     * List<Document> existingDocs = vectorStore.similaritySearch(
     * SearchRequest.builder().query("ABCTech").topK(1).build());
     * if(existingDocs != null && !existingDocs.isEmpty())
     * {
     * System.out.println("Data already exists in vector store . skip initialize");
     * return;
     * }
     * System.out.println("Data not found in vector store. Initializing data...");
     * TextReader textReader = new TextReader(new ClassPathResource("Courses.txt"));
     * TokenTextSplitter splitter = TokenTextSplitter
     * .builder()
     * .withChunkSize(500)
     * .build();
     * List<Document> docs = splitter.split(textReader.get());
     * vectorStore.add(docs);
     * }
     **/

    @PostConstruct
    public void initData() {
        // 1. Skip initialization if data already exists to prevent duplication
        List<Document> existingDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query("ABCTech").topK(1).build());
        if (existingDocs != null && !existingDocs.isEmpty()) {
            System.out.println("Data already exists in vector store. Skipping initialization.");
            return;
        }

        System.out.println("Initializing data from Courses.txt...");
        try {
            List<String> lines = Files.readAllLines(
                    new ClassPathResource("Courses.txt").getFile().toPath()
            );

            List<Document> documents = new ArrayList<>();
            Map<String, Object> currentMetadata = new HashMap<>();
            StringBuilder contentBuilder = new StringBuilder();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    // When an empty line is hit, save the completed document
                    if (contentBuilder.length() > 0) {
                        documents.add(new Document(contentBuilder.toString(), new HashMap<>(currentMetadata)));
                        contentBuilder.setLength(0);
                        currentMetadata.clear();
                    }
                } else if (line.startsWith("Title:") || line.startsWith("Description:")) {
                    contentBuilder.append(line).append(" ");
                } else if (line.startsWith("Level:")) {
                    currentMetadata.put("level", line.replace("Level:", "").trim());
                } else if (line.startsWith("Category:")) {
                    currentMetadata.put("category", line.replace("Category:", "").trim());
                } else if (line.startsWith("Price:")) {
                    // Parse price as Integer for numeric filtering
                    String priceStr = line.replace("Price:", "").replace("$", "").trim();
                    currentMetadata.put("price", Integer.parseInt(priceStr));
                }
            }

            // Add the final document if the file didn't end with an empty line
            if (contentBuilder.length() > 0) {
                documents.add(new Document(contentBuilder.toString(), currentMetadata));
            }

            vectorStore.add(documents);
            System.out.println("Successfully ingested " + documents.size() + " courses.");

        } catch (IOException e) {
            System.err.println("Error reading Courses.txt: " + e.getMessage());
        }
    }
}
