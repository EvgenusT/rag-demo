package com.example.ragdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Відповідає за фазу INDEXING:
 * Документи → chunks → embeddings → vector store
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;

    // Splitter ріже текст на chunks
    // defaultChunkSize=500 токенів, minChunkSizeChars=50, overlap між chunks
    private final TokenTextSplitter splitter =
            new TokenTextSplitter(500, 100, 50, 10000, true);

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Завантажує всі .txt файли з classpath:/docs/
     * і індексує їх у vector store.
     */
    public int ingestFromClasspath() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        // Збираємо .txt і .pdf разом
        List<Resource> allResources = new ArrayList<>();
        allResources.addAll(List.of(resolver.getResources("classpath:/docs/*.txt")));
        allResources.addAll(List.of(resolver.getResources("classpath:/docs/*.pdf")));

        List<Document> allDocs = new ArrayList<>();

        for (Resource resource : allResources) {
            log.info("Ingesting: {}", resource.getFilename());

            // Видаляємо старі chunks одразу в циклі
            vectorStore.delete(
                    new Filter.Expression(
                            Filter.ExpressionType.EQ,
                            new Filter.Key("source"),
                            new Filter.Value(resource.getFilename())
                    )
            );
//
            // Вибираємо reader залежно від типу файлу
            List<Document> docs = readResource(resource);
            List<Document> chunks = splitter.apply(docs);

            log.info("  {} → {} chunks", resource.getFilename(), chunks.size());
            allDocs.addAll(chunks);
        }

        // VectorStore автоматично:
        // 1. Викликає EmbeddingModel для кожного chunk
        // 2. Зберігає вектор + текст у Postgres
        vectorStore.add(allDocs);
        log.info("Total indexed: {} chunks", allDocs.size());

        return allDocs.size();
    }

    private List<Document> readResource(Resource resource) {
        String filename = resource.getFilename();

        assert filename != null;
        if (filename.endsWith(".pdf")) {
            var docs = new PagePdfDocumentReader(resource).read();
            docs.forEach(doc -> {
                // Прибираємо зайві пробіли
                assert doc.getText() != null;
                String cleaned = doc.getText()
                        .replaceAll(" {2,}", " ")        // кілька пробілів → один
                        .replaceAll("\\n{3,}", "\n\n")   // кілька порожніх рядків → два
                        .trim();
                doc.getMetadata().put("source", filename);
            });
            return docs;
        }

        return new TextReader(resource).get();
    }
}
