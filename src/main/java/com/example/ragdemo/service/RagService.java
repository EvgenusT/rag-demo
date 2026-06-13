package com.example.ragdemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Відповідає за фази RETRIEVAL + GENERATION:
 * Питання → знайти релевантні chunks → передати LLM → відповідь
 */
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QueryEnhancer queryEnhancer;
    private final JdbcTemplate jdbcTemplate;

    private static final String SYSTEM_PROMPT = """
            Ти — корисний асистент. Твоє ім'я - Тод. Відповідай ТІЛЬКИ на основі наданого контексту.
            Якщо відповіді в контексті немає — так і скажи, не вигадуй.
            Відповідай тією мовою, якою задали питання.
            """;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                      QueryEnhancer queryEnhancer, JdbcTemplate jdbcTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.queryEnhancer = queryEnhancer;
        this.jdbcTemplate = jdbcTemplate;
    }

    // Базовий RAG (як був)
    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .system(SYSTEM_PROMPT)
                .advisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(5)
                                .similarityThreshold(0.3)
                                .build())
                        .build())
                .call()
                .content();
    }

    // RAG з Query Rewriting
    public String askWithRewriting(String question) {
        String enhanced = queryEnhancer.rewrite(question);
        return chatClient.prompt()
                .user(enhanced) // ← шукаємо по покращеному питанню
                .system(SYSTEM_PROMPT)
                .advisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(5)
                                .similarityThreshold(0.3)
                                .build())
                        .build())
                .call()
                .content();
    }

    // RAG з HyDE
    public String askWithHyde(String question) {
        String hypothetical = queryEnhancer.generateHypotheticalAnswer(question);

        // Крок 1: шукаємо по гіпотетичній відповіді
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(hypothetical)  // ← гіпотеза для пошуку
                        .topK(5)
                        .similarityThreshold(0.2)
                        .build()
        );

        // Крок 2: будуємо контекст
        String context = docs.stream()
                .map(Document::getText)
                .filter(t -> t != null)
                .collect(Collectors.joining("\n---\n"));

        // Крок 3: LLM відповідає на ОРИГІНАЛЬНЕ питання
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("""
                    Контекст із бази знань:
                    {context}
                    
                    Питання: {question}
                    """)
                        .param("context", context)
                        .param("question", question))  // ← оригінальне питання
                .call()
                .content();
    }

    /**
     * Простий чат БЕЗ RAG — для порівняння
     * Відповідає тільки зі свого навчання
     */
    public String askWithoutRag(String question) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .call()
                .content();
    }

    /**
     * RAG з Hybrid Search (Vector + BM25 через PostgreSQL FTS)
     * Об'єднує результати через Reciprocal Rank Fusion (RRF)
     */
    public String askWithHybrid(String question) {
        // 1. Векторний пошук (семантичний)
        List<Document> vectorResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .similarityThreshold(0.2)
                        .build()
        );

        // 2. Повнотекстовий пошук (BM25-подібний через PostgreSQL)
        List<String> keywordResults = keywordSearch(question, 5);

        // 3. RRF злиття: score = Σ 1/(k + rank)
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        int k = 60; // стандартна константа RRF

        for (int i = 0; i < vectorResults.size(); i++) {
            String content = vectorResults.get(i).getText(); // ✅ getText()
            if (content != null) {
                rrfScores.merge(content, 1.0 / (k + i + 1), Double::sum);
            }
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            rrfScores.merge(keywordResults.get(i), 1.0 / (k + i + 1), Double::sum);
        }

        // 4. Топ-5 документів після злиття
        String context = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining("\n---\n"));

        // 5. Передаємо в LLM з явним контекстом
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("""
                    Контекст із бази знань:
                    {context}
                    
                    Питання: {question}
                    """)
                        .param("context", context)
                        .param("question", question))
                .call()
                .content();
    }

    // ======== ПРИВАТНИЙ ХЕЛПЕР ========

    private List<String> keywordSearch(String query, int topK) {
        // PostgreSQL Full-Text Search з ts_rank
        String sql = """
            SELECT content
            FROM vector_store
            WHERE to_tsvector('english', content) @@ plainto_tsquery('english', ?)
            ORDER BY ts_rank(
                to_tsvector('english', content),
                plainto_tsquery('english', ?)
            ) DESC
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, String.class, query, query, topK);
    }
}