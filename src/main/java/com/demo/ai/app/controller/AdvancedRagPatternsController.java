package com.demo.ai.app.controller;

import com.demo.ai.app.tools.CourseSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/advancedrag")
public class AdvancedRagPatternsController {
    @Autowired
    @Qualifier("openAiEmbeddingModel")
    private EmbeddingModel embeddingModel;

    @Autowired
    @Qualifier("redisVectorStore")
    private VectorStore vectorStore;

    private ChatClient chatClient;

    @Autowired
    private CourseSearchTool courseSearchTool;

    private OpenAiChatModel chatModel;

    public AdvancedRagPatternsController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;

        this.chatClient = ChatClient.create(chatModel);
    }

    //Pattern 1: Agentic RAG
    @GetMapping("/get-course-agentic")
    public String getAnswerAgenticRag(@RequestParam String query) {
        return chatClient.prompt(query)
                .system(
                        """
                                You are ABC Tech AI Course Advisor.
                                
                                If the user asks about:
                                - courses
                                - prices
                                - duration
                                - technologies
                                - what ABC Tech offers
                                
                                use the searchCourses tool.
                                
                                For greetings or general AI questions,
                                answer directly without using the tool.
                                """
                )
                .tools(courseSearchTool)
                .call()
                .content();
    }

    //Pattern 2: Query rewriting
    @GetMapping("/query-rewrite")
    public String queryRewrite(@RequestParam String query) {
        String rewrittenQuery = chatClient.prompt()
                .system("Rewrite the user query as a clear, complete search " +
                        "query for a course catalog. Respond ONLY with the " +
                        "rewritten query, nothing else.")
                .user(query)
                .call()
                .content();

        System.out.println("User prompt original: " + query);
        System.out.println("User prompt rewritten: " + rewrittenQuery);

        List<Document> chunks = vectorStore.similaritySearch(
                SearchRequest.builder().query(rewrittenQuery)
                        .topK(6)
                        .build());

        return answerWithContext(query, chunks);
    }

    private String answerWithContext(String query, List<Document> chunks) {

        if (chunks.isEmpty()) {
            return "No Relevant courses found ";
        }
        String context = chunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        return chatClient.prompt()
                .system(
                        """
                                You are ABC Tech helpful course advisor.
                                Use only the provided course catalog context.
                                Mention course name, price, duration, and level.
                                If the answer is not in the context, politely say so.
                                """
                )
                .user("Context:\n" + context + "\n\nUser Query:\n" + query)
                .call()
                .content();
    }

    //Pattern 3: HyDE (hypothetical document embeddings)
    @GetMapping("/hypothetical-response")
    public String hypotheticalResponse(@RequestParam String query) {
        String hypothetical = chatClient.prompt()
                .system(
                        """
                                Write a short factural answer for this course question.
                                Even if you are not completely sure m write a releastic answer.
                                """
                )
                .user(query)
                .call()
                .content();

        System.out.println("User prompt original: " + query);
        System.out.println("Hypothetical answer: " + hypothetical);

        List<Document> chunks = vectorStore.similaritySearch(
                SearchRequest.builder().query(hypothetical)
                        .topK(6)
                        .build());

        return answerWithContext(query, chunks);
    }

    //Pattern 4: Metadata filtering

    @GetMapping("/filtered-search")
    public String filteredSearch(@RequestParam String query,
                                 @RequestParam(required = false) String level,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) Integer price) {

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op finalOp = null;

        // Build conditions dynamically
        if (category != null && !category.isEmpty()) {
            finalOp = (finalOp == null) ? b.eq("category", category) : b.and(finalOp, b.eq("category", category));
        }
        if (level != null && !level.isEmpty()) {
            finalOp = (finalOp == null) ? b.eq("level", level) : b.and(finalOp, b.eq("level", level));
        }
        if (price != null && price > 0) {
            finalOp = (finalOp == null) ? b.lte("price", price) : b.and(finalOp, b.lte("price", price));
        }

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(6);

        // Only build if an operation was actually created
        if (finalOp != null) {
            System.out.println("DEBUG: Filter expression being applied: " + finalOp.build().toString());
            builder.filterExpression(finalOp.build()); // Convert Op to Filter.Expression here
        } else {
            System.out.println("DEBUG: No filters applied.");
        }

        List<Document> chunks = vectorStore.similaritySearch(builder.build());
        System.out.println("DEBUG: Number of documents found: " + (chunks != null ? chunks.size() : "null"));
        for (Document doc : chunks) {
            System.out.println("Response from vector store: " + doc.getText());
            // --- ADD DEBUG POINT HERE TO SEE METADATA ---
            System.out.println("DEBUG: Document metadata: " + doc.getMetadata());
        }
        return answerWithContext(query, chunks);
    }

    /**
     * @GetMapping("/filtered-search") public String filteredSerach(@RequestParam String query,
     * @RequestParam(required = false) String level,
     * @RequestParam(required = false) String category,
     * @RequestParam(required = false) Integer price) {
     * System.out.println(level + " " + category + " " + price);
     * <p>
     * // empty filter expression
     * StringBuilder filter = new StringBuilder();
     * <p>
     * // if category is provided, add to filter
     * if (category != null && !category.isEmpty()) {
     * if (filter.length() > 0) {
     * filter.append(" && ");
     * }
     * filter.append("category == '")
     * .append(santize(category))
     * .append("'");
     * }
     * //if level is provided, add to filter
     * if (level != null && !level.isEmpty()) {
     * if (filter.length() > 0) {
     * filter.append(" && ");
     * }
     * filter.append("level == '")
     * .append(santize(level)) // AI & DataSceince ==> AI_and_Data_Science
     * .append("'");
     * }
     * // if maxPrice is provided, add to filter
     * if (price != null && price > 0) {
     * if (filter.length() > 0) {
     * filter.append(" && ");
     * }
     * filter.append("price <= ")
     * .append(price);
     * }
     * <p>
     * System.out.println("Filter expression: " + filter.toString());
     * <p>
     * SearchRequest.Builder builder = SearchRequest.builder()
     * .query(query)
     * .topK(6);
     * <p>
     * //        System.out.println();
     * System.out.println(filter.length());
     * if (filter.length() > 0) {
     * builder.filterExpression(filter.toString());
     * }
     * <p>
     * List<Document> chunks = vectorStore.similaritySearch(
     * builder.build());
     * <p>
     * for (Document doc : chunks) {
     * System.out.println("response from vectore store");
     * System.out.println(doc.getText());
     * }
     * <p>
     * return answerWithContext(query, chunks);
     * }
     * <p>
     * private String santize(String value) {
     * if (value == null || value.isEmpty()) return "Unknown";
     * return value
     * .replaceAll("&", "and")
     * .replaceAll("[\\s,/]+", "_")
     * .replaceAll("[^a-zA-Z0-9_-]", "")
     * .trim();
     * }
     **/

    //Pattern 5: LLM re-ranking
    @GetMapping("/rerank")
    public String rerank(@RequestParam String query) {
        // 1. Get candidate documents
        List<Document> maxChunks = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(10)
                .build());

        if (maxChunks.isEmpty()) {
            return "No Courses found based on your request";
        }

        // 2. Format chunks for the LLM
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < maxChunks.size(); i++) {
            String text = maxChunks.get(i).getText();
            // Reassign the trimmed string
            String chunkSnippet = text.length() > 300 ? text.substring(0, 300) : text;
            numbered.append(i + 1).append(". ").append(chunkSnippet).append("\n\n");
        }

        // 3. Ask LLM to rank
        String llmRankingResponse = chatClient.prompt()
                .system("You are a search relevance expert. Given a user query and a list of numbered text chunks, return ONLY the numbers of the top 3 most relevant chunks separated by commas (e.g., 2,5,1). Do not include any other text.")
                .user("Query: " + query + "\n\nChunks:\n" + numbered)
                .call()
                .content();

        System.out.println("LLM Ranking response ==> " + llmRankingResponse);

        // 4. Parse response
        List<Document> rerankedDoc = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+").matcher(llmRankingResponse);

        while (matcher.find() && rerankedDoc.size() < 3) {
            int index = Integer.parseInt(matcher.group()) - 1;
            if (index >= 0 && index < maxChunks.size()) {
                rerankedDoc.add(maxChunks.get(index));
            }
        }

        // 5. Resilience: Fallback if LLM failed to return valid numbers
        if (rerankedDoc.isEmpty()) {
            rerankedDoc = maxChunks.subList(0, Math.min(3, maxChunks.size()));
        }

        return answerWithContext(query, rerankedDoc);
    }

    /**
     * @GetMapping("/rerank") public String rerank(@RequestParam String query) {
     * // 1. get a wider set of candidates
     * List<Document> maxChunks = vectorStore.similaritySearch(SearchRequest.builder()
     * .query(query)
     * .topK(10)
     * .build());
     * <p>
     * if (maxChunks.isEmpty()) {
     * return "No Courses found based your request";
     * }
     * // 2. number + trim each chunk (to ~250 chars) to save tokens
     * StringBuilder numbered = new StringBuilder();
     * for (int i = 0; i < maxChunks.size(); i++) {
     * String chunkSnippet = maxChunks.get(i).getText();
     * if (chunkSnippet.length() > 300) {
     * chunkSnippet.substring(0, 300);
     * }
     * numbered.append(i + 1)
     * .append(", ")
     * .append(chunkSnippet)
     * .append("\n\n");
     * }
     * // 3. ask the LLM to rank and return only the best 3 numbers
     * //ask llm to ranking 9, 4, 6
     * String llmRankingResponse = chatClient
     * .prompt()
     * .system(
     * """
     * You rank text chunks by relevance to a user query.
     * Return ONLY the numbers of the best 3 chunks.
     * Example: 3,7,1
     * """
     * )
     * .user("Query" + query + "Chunks  " + numbered)
     * .call()
     * .content();
     * System.out.println("LLM Ranking response ==> " + llmRankingResponse);
     * <p>
     * // 4. parse the numbers back into documents (regex over the ranking string)
     * List<Document> rerankedDoc = new ArrayList<>();
     * Matcher matcher = Pattern.compile("\\d+")
     * .matcher(llmRankingResponse);
     * <p>
     * while (matcher.find() && rerankedDoc.size() < 3) {
     * <p>
     * int index = Integer.parseInt(matcher.group()) - 1;
     * <p>
     * if (index >= 0 && index < maxChunks.size()) {
     * rerankedDoc.add(maxChunks.get(index));
     * }
     * // 5. resilience: if the LLM gave nothing usable, fall back to the first 3
     * if (rerankedDoc.isEmpty()) {
     * <p>
     * rerankedDoc = maxChunks.subList(
     * 0,
     * Math.min(3, maxChunks.size())
     * );
     * }
     * }
     * System.out.println("Final Re rank chunks");
     * for (int i = 0; i < rerankedDoc.size(); i++) {
     * System.out.println("Rank" + (i + 1));
     * System.out.println(rerankedDoc.get(i).getText());
     * }
     * return answerWithContext(query, rerankedDoc);
     * <p>
     * <p>
     * }
     **/

    //Pattern 6: RetrievalAugmentationAdvisor (modern RAG)
    /**
     * Pipeline Ordering: The RetrievalAugmentationAdvisor manages the execution flow:
     *
     * 1 Transform: QueryTransformer refines the input.
     *
     * 2 Retrieve: DocumentRetriever fetches candidates from Redis.
     *
     * 3 Process: DocumentPostProcessor cleans/reranks the candidates.
     *
     * 4 Augment: QueryAugmenter injects the final context into the LLM prompt.
     */
    @GetMapping("/mordern-rag")
    public String modernRag(@RequestParam String query) {
        System.out.println("User query original: " + query);


        // 1. Query Transformer: Rewrite/expand query for better retrieval accuracy
        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();

        // 2. Document Post-Processor: Filter or rerank retrieved documents
        // Example: Remove documents that are too short to be useful
        DocumentPostProcessor lengthFilter = (q, documents) -> documents.stream()
                .filter(doc -> doc.getText().length() > 50)
                .toList();

        //3  Build the Advisor using the pattern from your image
        var ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(List.of(queryTransformer)) // Added Transformer
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(3)
                        .build())
                .documentPostProcessors(List.of(lengthFilter)) // Added Post-Processor
                .queryAugmenter(
                        ContextualQueryAugmenter.builder()
                                //allow the LLM to answer normaly if no doc is found in vector db
                                .allowEmptyContext(true)
                                .build())
                .build();

        // 4. Build and call the ChatClient
        ChatClient modernchatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are ABC Tech helpful course advisor.
                        Use only the provided course catalog context.
                        Mention course name, price, duration, and level.
                        
                        If the user's question is not about courses
                        (like a greeting or a general request),\s
                        you may answer it normally using your own knowledge.
                        
                        If the question is about courses and the information is not in the provided context,\s
                        politely say you don't have that information.
                        """)
                .defaultAdvisors(ragAdvisor) // Using defaultAdvisors for the client
                .build();


        // 3. Execute the call to get the answer
        String answer = modernchatClient
                .prompt(query)
                .call()
                .content();

        System.out.println("Final Answer: " + answer);
        return answer;
    }


}
