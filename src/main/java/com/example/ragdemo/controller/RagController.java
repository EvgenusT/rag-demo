package com.example.ragdemo.controller;

import com.example.ragdemo.service.IngestionService;
import com.example.ragdemo.service.QueryEnhancer;
import com.example.ragdemo.service.RagService;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;
    private final IngestionService ingestionService;
    private final QueryEnhancer queryEnhancer;
    private final VectorStore vectorStore;

    public RagController(RagService ragService, IngestionService ingestionService, QueryEnhancer queryEnhancer, VectorStore vectorStore) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
        this.queryEnhancer = queryEnhancer;
        this.vectorStore = vectorStore;
    }

    /**
     * POST /api/ask
     * Body: { "question": "Що таке Spring Boot?" }
     *
     * Відповідає використовуючи RAG (твої документи)
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = ragService.ask(question);
        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer,
                "mode", "RAG"
        ));
    }

    /**
     * POST /api/ask-plain
     * Та ж відповідь але БЕЗ RAG — для порівняння
     */
    @PostMapping("/ask-plain")
    public ResponseEntity<Map<String, String>> askPlain(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = ragService.askWithoutRag(question);
        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer,
                "mode", "plain LLM"
        ));
    }

    /**
     * POST /api/ingest
     * Індексує всі файли з src/main/resources/docs/
     * Виклич це один раз перед тим як питати
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest() throws IOException {
        int count = ingestionService.ingestFromClasspath();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "chunksIndexed", count
        ));
    }

    @PostMapping("/ask-rewrite")
    public ResponseEntity<Map<String, String>> askWithRewrite(
            @RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = ragService.askWithRewriting(question);
        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer,
                "mode", "RAG + Query Rewriting"
        ));
    }

    @PostMapping("/ask-hyde")
    public ResponseEntity<Map<String, String>> askWithHyde(
            @RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = ragService.askWithHyde(question);
        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer,
                "mode", "RAG + HyDE"
        ));
    }

    @PostMapping("/debug-search")
    public ResponseEntity<Object> debugSearch(
            @RequestBody Map<String, String> body) {
        String question = body.get("question");

        var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .similarityThreshold(0.0) // без фільтру — показати всі
                        .build()
        );

        var debug = results.stream().map(doc -> Map.of(
                "content", doc.getText().substring(0, Math.min(100, doc.getText().length())),
                "score", doc.getMetadata().getOrDefault("distance", "n/a"),
                "source", doc.getMetadata().getOrDefault("source", "unknown")
        )).toList();

        return ResponseEntity.ok(debug);
    }

    @GetMapping("/ask-hybrid")
    public String askHybrid( @RequestBody Map<String, String> body) {
        String question = body.get("question");
        return ragService.askWithHybrid(question);
    }
}
