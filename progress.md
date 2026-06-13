# Java Developer → AI Engineer — План розвитку

## 👤 Профіль
- **Роль:** Backend Java розробник
- **Ціль:** Розвиток на стику Java + AI (RAG, LLM, агенти)
- **Стек:** Java, Spring Boot, Maven

---

## ✅ Вже зроблено

### Середовище
- [x] Встановлено Chocolatey (менеджер пакетів Windows)
- [x] Встановлено Java 21 (Temurin)
- [x] Встановлено Maven
- [x] Встановлено Docker Desktop
- [x] Увімкнено SVM (віртуалізація) в BIOS — плата Gigabyte AB350M-DS3H, Ryzen 5 1600
- [x] Docker успішно запускається

### Теорія
- [x] Зрозуміли що таке LLM (токени, transformer, генерація)
- [x] Зрозуміли що таке RAG (Indexing → Retrieval → Generation)
- [x] Зрозуміли різницю RAG vs fine-tuning
- [x] Зрозуміли що таке OpenAI API (програмний доступ до GPT)
- [x] Розібрали підводні камені RAG (chunking, retrieval, evaluation)

### Код
- [x] Створено проєкт rag-demo (Spring AI + pgvector)
- [x] Завантажено архів rag-demo.zip
- [x] Відкрито проєкт в IntelliJ IDEA

---

## 🔄 Поточний крок

**Запуск rag-demo проєкту локально**

Що залишилось зробити:
1. [X] `docker-compose up -d` — підняти PostgreSQL з pgvector
2. [X] Отримати OpenAI API ключ на platform.openai.com
3. [X] Встановити API ключ в PowerShell: `$env:OPENAI_API_KEY="sk-..."`
4. [X] Запустити проєкт: `mvnw spring-boot:run` (або кнопка ▶ в IDEA)
5. [x] Проіндексувати документи: `curl -X POST http://localhost:8080/api/ingest`
6. [x] Задати перше питання: `curl -X POST http://localhost:8080/api/ask`

---

## 📅 План на 3 місяці

### Місяць 1 — Перший working RAG
- [x] Запустити rag-demo локально
- [x] Отримати відповідь від RAG на питання про власні документи
- [ ] Порівняти `/ask` (RAG) vs `/ask-plain` (без RAG)
- [ ] Додати свої .txt документи і проіндексувати
- [ ] Пограти з параметрами: topK, similarityThreshold, розмір chunks

### Місяць 2 — Якість і розуміння
- [ ] Розібрати hybrid search (вектор + keyword BM25)
- [ ] Додати логування питань і відповідей
- [ ] Зрозуміти де система помиляється
- [ ] Базовий evaluation (RAGAS метрики)
- [ ] Встановити Postman для зручного тестування API

### Місяць 3 — Агенти
- [ ] Вивчити LangChain4j
- [ ] Зробити агента з кількома tools (пошук, запит до БД)
- [ ] Опублікувати на GitHub з README
- [ ] Це вже портфоліо для резюме

---

## 🛠 Встановлене ПЗ

| Програма | Версія | Команда встановлення |
|---|---|---|
| Chocolatey | 2.7.2 | — |
| Java | 21 (Temurin) | `choco install temurin21` |
| Maven | 3.9.x | `choco install maven` |
| Docker Desktop | 4.75.0 | `choco install docker-desktop` |
| IntelliJ IDEA | — | — |

---

## 📁 Проєкт rag-demo — структура

```
rag-demo/
├── docker-compose.yml              ← запуск PostgreSQL
├── pom.xml                         ← залежності Spring AI
└── src/main/
    ├── java/com/example/ragdemo/
    │   ├── config/VectorStoreConfig.java    ← налаштування pgvector
    │   ├── service/IngestionService.java    ← INDEXING (документи → вектори)
    │   ├── service/RagService.java          ← RETRIEVAL + GENERATION
    │   └── controller/RagController.java   ← REST API
    └── resources/
        ├── application.yml                  ← конфігурація + API ключ
        └── docs/                            ← твої документи
            ├── team-handbook.txt
            └── architecture-decisions.txt
```

## 🔑 Корисні команди

```powershell
# Docker
docker-compose up -d        # запустити PostgreSQL
docker-compose down         # зупинити
docker ps                   # що запущено

# Maven
mvnw spring-boot:run        # запустити проєкт

# API ключ (PowerShell)
$env:OPENAI_API_KEY="sk-..."

# Тестування
curl -X POST http://localhost:8080/api/ingest
curl -X POST http://localhost:8080/api/ask -H "Content-Type: application/json" -d '{"question": "Який timeout у Kafka consumer?"}'
```

---

## 💡 Як використовувати цей файл

На початку нового чату з Claude — скопіюй і встав вміст цього файлу.
Напиши: "Ось мій прогрес, продовжуємо з кроку X"

Оновлюй файл коли завершуєш кроки — міняй `[ ]` на `[x]`.
