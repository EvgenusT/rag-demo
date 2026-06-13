package com.example.ragdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class QueryEnhancer {

    private static final Logger log = LoggerFactory.getLogger(QueryEnhancer.class);
    private final ChatClient chatClient;

    public QueryEnhancer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    /**
     * Техніка 1: Query Rewriting
     * Переформульовує коротке розмовне питання
     * на детальний пошуковий запит
     */
    public String rewrite(String question) {
        String rewritten = chatClient.prompt()
                .system("""
                           Ти — помічник для пошуку по технічній документації IT проєкту.
                                                                                          Документи написані УКРАЇНСЬКОЮ мовою.
                                                                                          Переформулюй питання українською, додай технічні терміни.
                                                                                          Відповідай ТІЛЬКИ покращеним запитом, без пояснень.
                        """)
                .user(question)
                .call()
                .content();

        log.debug("Query rewritten: '{}' → '{}'", question, rewritten);
        return rewritten;
    }

    /**
     * Техніка 2: HyDE (Hypothetical Document Embedding)
     * Генерує гіпотетичну відповідь на питання.
     * Шукаємо не по питанню, а по відповіді —
     * бо відповідь семантично схожа на документи в БД
     */
    public String generateHypotheticalAnswer(String question) {
        String hypothetical = chatClient.prompt()
                .system("""
                        Згенеруй коротку гіпотетичну відповідь технічною мовою.
                                Пиши УКРАЇНСЬКОЮ — як в технічній документації.
                                2-3 речення без вступу.
                        """)
                .user(question)
                .call()
                .content();

        log.debug("HyDE generated for: '{}'", question);
        return hypothetical;
    }

    /**
     * Техніка 3: Multi-Query
     * Генерує 3 варіанти питання —
     * кожен може знайти різні релевантні chunks
     */
    public java.util.List<String> generateMultiQuery(String question) {
        String response = chatClient.prompt()
                .system("""
                        Згенеруй 3 різні варіанти цього питання для пошуку.
                        Кожен варіант з нового рядка, без нумерації.
                        Варіанти мають шукати ту саму інформацію але різними словами.
                        """)
                .user(question)
                .call()
                .content();

        return java.util.Arrays.stream(response.split("\n"))
                .filter(s -> !s.isBlank())
                .limit(3)
                .toList();
    }
}
