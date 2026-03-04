//package com.projectfaust.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//@Configuration
//public class AiConfig {
//
//    @Value("${spring.ai.openai.api-key}")
//    private String apiKey;
//
//    @Bean
//    public OpenAiApi openAiApi() {
//        // Používáme builder, abychom se vyhnuli tomu 8-parametrovému konstruktoru
//        return OpenAiApi.builder()
//                .apiKey(apiKey)
//                .build();
//    }
//
//    @Bean
//    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
//        return new OpenAiEmbeddingModel(openAiApi);
//    }
//
//    @Bean
//    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
//        // Snapshot verze vyžaduje povinné závislosti přímo v metodě builder(...)
//        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
//                // Tady už můžeš řetězit další nastavení, pokud je potřeba
//                // .tableName("vector_store")
//                // .dimensions(1536)
//                .build();
//    }
//}