package com.demo.ai.app.config;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MySqlChatMemoryConfig {

    @Bean(name = "mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(
            @Value("${mysql.datasource.url}") String url,
            @Value("${mysql.datasource.username}") String username,
            @Value("${mysql.datasource.password}") String password,
            @Value("${mysql.datasource.driver-class-name}") String driver) {

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driver)
                .build();
    }

    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(DataSource mysqlDataSource) {
        return new JdbcTemplate(mysqlDataSource);
    }

    @Bean
    public JdbcChatMemoryRepository jdbcChatMemoryRepository(JdbcTemplate mysqlJdbcTemplate) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(mysqlJdbcTemplate)
                // Notice the lowercase 's' in Mysql
                .dialect(new MysqlChatMemoryRepositoryDialect())
                .build();
    }
}
