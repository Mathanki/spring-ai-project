package com.demo.ai.app.config;


import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisClient;

import javax.sql.DataSource;


@Configuration
public class AppConfig {

    private final Environment env;

    // Constructor injection for the Spring Environment abstraction
    public AppConfig(Environment env) {
        this.env = env;
    }


    //simple vector store
    //    @Bean
//    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
//        return SimpleVectorStore.builder(embeddingModel).build();
//    }

    /**
     * //PGVector
     *
     * @Bean
     * @Primary public VectorStore vectorStore(
     * @Value("${app.datasource.postgres.url}") String url,
     * @Value("${app.datasource.postgres.username}") String username,
     * @Value("${app.datasource.postgres.password}") String password,
     * @Value("${app.datasource.postgres.driver-class-name}") String driver,
     * EmbeddingModel embeddingModel) {
     * <p>
     * // 1. Manually build the Postgres connection pool
     * DataSource postgresDataSource = DataSourceBuilder.create()
     * .url(url)
     * .username(username)
     * .password(password)
     * .driverClassName(driver)
     * .build();
     * <p>
     * // 2. Bind it exclusively to a Postgres-only JdbcTemplate
     * JdbcTemplate postgresJdbcTemplate = new JdbcTemplate(postgresDataSource);
     * <p>
     * // 3. Build the PgVectorStore manually using our isolated template
     * return PgVectorStore.builder(postgresJdbcTemplate, embeddingModel)
     * .dimensions(1536)
     * .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
     * .indexType(PgVectorStore.PgIndexType.HNSW)
     * .initializeSchema(true) // Automatically initializes vector schema safely in Postgres
     * .build();
     * }
     **/

    @Value("${spring.ai.vectorstore.redis.index-name}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix}")
    private String prefix;

    // ==========================================
    // 1. REDIS VECTOR STORE CONFIGURATION
    // ==========================================

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        // Dynamically bind to the configuration environment data map populated by Docker Compose
        String host = Binder.get(env).bind("spring.data.redis.host", String.class).orElse("localhost");
        int port = Binder.get(env).bind("spring.data.redis.port", Integer.class).orElse(6379);
        String password = Binder.get(env).bind("spring.data.redis.password", String.class).orElse(null);

        config.setHostName(host);
        config.setPort(port);

        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }

        return new JedisConnectionFactory(config);
    }


    @Bean
    public RedisClient jedisClient(Environment env) {
        // Fallback checks read variables bound by properties file or overridden by Docker Compose Support
        String host = env.getProperty("spring.data.redis.host", "localhost");
        int port = env.getProperty("spring.data.redis.port", Integer.class, 6379);

        return RedisClient.builder()
                .hostAndPort(new HostAndPort(host, port))
                .build();
    }

    @Bean
    public VectorStore redisVectorStore(RedisClient redisClient, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(redisClient, embeddingModel)
                .indexName(indexName)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("level"),
                        RedisVectorStore.MetadataField.tag("category"),
                        RedisVectorStore.MetadataField.numeric("price")
                )
                .prefix(prefix)
                .initializeSchema(true)
                .build();
    }

    // ==========================================
    // 2. POSTGRES (PGVECTOR) CONFIGURATION (FROM DOCKER COMPOSE)
    // ==========================================

    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource() {
        // Force Spring to read the specific keys injected from the container lifecycle
        String url = env.getProperty("spring.ai.vectorstore.pgvector.url", "jdbc:postgresql://localhost:5433/demo_vector_ai");
        String username = env.getProperty("spring.ai.vectorstore.pgvector.username", "postgres");
        String password = env.getProperty("spring.ai.vectorstore.pgvector.password", "welcomeMathanki");

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean(name = "postgresJdbcTemplate")
    public JdbcTemplate postgresJdbcTemplate(DataSource postgresDataSource) {
        return new JdbcTemplate(postgresDataSource);
    }

    @Bean
    public VectorStore pgVectorStore(
            @Qualifier("postgresDataSource") DataSource postgresDataSource,
            EmbeddingModel embeddingModel) {

        // Explicitly build a dedicated JdbcTemplate for Postgres
        JdbcTemplate postgresJdbcTemplate = new JdbcTemplate(postgresDataSource);

        return PgVectorStore.builder(postgresJdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
    }
}
