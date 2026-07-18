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
        List<Document> existingDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query("ABCTech").topK(1).build());

        if (existingDocs != null && !existingDocs.isEmpty()) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(new ClassPathResource("Courses.txt").getFile().toPath());
            List<Document> documents = new ArrayList<>();
            Map<String, Object> currentMetadata = new HashMap<>();
            StringBuilder contentBuilder = new StringBuilder();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    if (contentBuilder.length() > 0) {
                        documents.add(new Document(contentBuilder.toString(), new HashMap<>(currentMetadata)));
                        contentBuilder.setLength(0);
                        currentMetadata.clear();
                    }
                } else if (line.startsWith("Title:") || line.startsWith("Description:")) {
                    contentBuilder.append(line).append(" ");
                } else if (line.startsWith("Level:")) {
                    String val = line.replace("Level:", "").trim();
                    currentMetadata.put("level", val);
                    contentBuilder.append("Level: ").append(val).append(" "); // Now included in text
                } else if (line.startsWith("Category:")) {
                    String val = line.replace("Category:", "").trim();
                    currentMetadata.put("category", val);
                    contentBuilder.append("Category: ").append(val).append(" "); // Now included in text
                } else if (line.startsWith("Duration:")) {
                    String val = line.replace("Duration:", "").trim();
                    currentMetadata.put("duration", val);
                    contentBuilder.append("Duration: ").append(val).append(" "); // ADDED THIS
                } else if (line.startsWith("Price:")) {
                    String p = line.replace("Price:", "").replace("$", "").trim();
                    currentMetadata.put("price", Integer.parseInt(p));
                    contentBuilder.append("Price: $").append(p).append(" "); // Now included in text
                }
            }
            if (contentBuilder.length() > 0) documents.add(new Document(contentBuilder.toString(), currentMetadata));
            vectorStore.add(documents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
