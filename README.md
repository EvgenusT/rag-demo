# RAG Demo — Spring AI + pgvector

Мінімальний working приклад RAG системи на Java.

## Що тут є

- **IngestionService** — читає .txt файли, ріже на chunks, зберігає embeddings у pgvector
- **RagService** — приймає питання, знаходить релевантні chunks, передає в LLM
- **RagController** — REST API для всього вище

## Запуск

### 1. Підняти PostgreSQL з pgvector розширенням
```bash
docker-compose up -d
```

### 2. Встановити API ключ
```bash
export OPENAI_API_KEY=sk-...
```

### 3. Запустити додаток
```bash
./mvnw spring-boot:run
```

### 4. Проіндексувати документи (один раз)
```bash
curl -X POST http://localhost:8080/api/ingest
# {"status":"ok","chunksIndexed":12}
```

### 5. Питати!
```bash
# З RAG (знає твої документи)
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Який timeout у Kafka consumer?"}'

# Без RAG (тільки LLM, для порівняння)
curl -X POST http://localhost:8080/api/ask-plain \
  -H "Content-Type: application/json" \
  -d '{"question": "Який timeout у Kafka consumer?"}'
```

## Структура

```
src/main/
├── java/com/example/ragdemo/
│   ├── config/VectorStoreConfig.java    ← налаштування pgvector
│   ├── service/IngestionService.java    ← INDEXING фаза
│   ├── service/RagService.java          ← RETRIEVAL + GENERATION
│   └── controller/RagController.java   ← REST API
└── resources/
    ├── application.yml                  ← конфігурація
    └── docs/                            ← твої документи тут
        ├── team-handbook.txt
        └── architecture-decisions.txt
```

## Що спробувати далі

1. Додай свої документи в `src/main/resources/docs/`
2. Пограй з `topK` і `similarityThreshold` у RagService
3. Порівняй відповіді `/ask` vs `/ask-plain` для специфічних питань
4. Подивись що буде якщо chunk size зробити 100 vs 1000
