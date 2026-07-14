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

import java.util.List;

@Component
public class DataInitializer {

    private final VectorStore vectorStore;
    // Use @Qualifier to target your custom vectorStore bean precisely
    public DataInitializer(@Qualifier("redisVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
    @PostConstruct
    public void initData()
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
    @PostConstruct
    public void initData()
    {
        List<Document> existingDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query("ABCTech").topK(1).build());
        if(existingDocs != null && !existingDocs.isEmpty())
        {
            System.out.println("Data already exists in vector store . skip initialize");
            return;
        }
        System.out.println("Data not found in vector store. Initializing data...");
        TextReader textReader = new TextReader(new ClassPathResource("Courses.txt"));
        TokenTextSplitter splitter = TokenTextSplitter
                .builder()
                .withChunkSize(500)
                .build();
        List<Document> docs = splitter.split(textReader.get());
        vectorStore.add(docs);
    }
}
