package com.example.ragdemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Відповідає за фази RETRIEVAL + GENERATION:
 * Питання → знайти релевантні chunks → передати LLM → відповідь
 */
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QueryEnhancer queryEnhancer;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, QueryEnhancer queryEnhancer) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Ти — корисний асистент. Відповідай ТІЛЬКИ на основі наданого контексту.
                        Якщо відповіді в контексті немає — так і скажи, не вигадуй.
                        Відповідай тією мовою, якою задали питання.
                        """)
//                .defaultSystem("""
//                        Ти — веселий асистент-гуморист. Відповідай на основі наданого контексту,
//                        але додавай жарти, саркастичні коментарі та емодзі.
//                        Уявляй себе як IT-розробник який вже 10 годин дивиться в код і трохи збожеволів.
//                        Якщо відповіді в контексті немає — скажи про це з гумором.
//                        Відповідай тією мовою, якою задали питання.
//                        """)
                .build();
        this.queryEnhancer = queryEnhancer;
    }

    // Базовий RAG (як був)
    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
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
        // Шукаємо по гіпотетичній відповіді, але питаємо оригінальне питання
        return chatClient.prompt()
                .user(hypothetical)
                .advisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(5)
                                .similarityThreshold(0.2) // нижче бо HyDE вже схожа на документи
                                .build())
                        .build())
                .call()
                .content();
    }

    /**
     * Простий чат БЕЗ RAG — для порівняння
     * Відповідає тільки зі свого навчання
     */
    public String askWithoutRag(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}