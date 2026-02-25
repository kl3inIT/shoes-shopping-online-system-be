# AI Module Architecture

## Overview

The `com.sba.ssos.ai` package implements an AI-powered shopping assistant using **Spring AI**.
It provides conversational chat with RAG (Retrieval-Augmented Generation), automated quality
checks, and a configurable ingestion pipeline for feeding domain data into a vector store.

The architecture is inspired by [jmix-ai-backend](../../../jmix-ai-backend) and adapted for
a standard Spring Boot REST API application.

---

## Package Structure

```
ai/
├── chat/           Chat pipeline (controller, service, logging, entities)
├── checks/         Automated AI quality evaluation
├── config/         Spring AI bean configuration
├── ingestion/      Data ingestion into VectorStore
├── parameters/     YAML-driven configuration reader
├── rag/            RAG tools, retrieval, post-processing
└── search/         Standalone vector search (no LLM)
```

Total: **38 files** across 7 packages.

---

## 1. chat/ — Chat Pipeline

The core conversational AI pipeline.

| File | Role |
|---|---|
| `Chat.java` | Interface defining the chat contract |
| `ChatService.java` | Implementation — orchestrates memory, RAG tools, LLM call |
| `ChatController.java` | REST endpoint `POST /api/v1/chat` |
| `ChatRequest.java` | Request record (`userId`, `message`) |
| `ChatResponse.java` | Response record (`text`, `logMessages`, `sourceLinks`, tokens, time) |
| `ChatLog.java` | JPA entity — persists each chat interaction (extends `BaseEntity`) |
| `ChatLogRepository.java` | Spring Data repository for `ChatLog` |
| `ChatLogService.java` | Service layer for saving chat logs and errors |

### How a chat request flows

```
Client  →  ChatController  →  ChatService (implements Chat)
                                    │
                ┌───────────────────┼───────────────────┐
                ▼                   ▼                   ▼
        ChatMemory          ToolsManager          QuestionAnswerAdvisor
     (conversation         (RAG tools from         (VectorStore-backed
      history via JDBC)     YAML config)            advisor)
                                    │
                                    ▼
                              ChatClient.prompt()
                                    │
                                    ▼
                               LLM (OpenAI / Gemini)
                                    │
                                    ▼
                            ChatResponse + ChatLog saved
```

### Key design decisions

- **`Chat` interface** — enables mocking in tests and swapping implementations.
- **System prompt loaded from YAML** via `ParametersService`, not hardcoded.
- **`ResponseGeneral<T>`** used for all API responses (consistent with the rest of the app).
- **Conversation ID** = `u:{userId}:t:{threadId}` — supports multi-thread per user.

### API

```
POST /api/v1/chat
Headers: X-Thread-Id (optional)
Body: { "userId": "abc", "message": "What's the return policy?" }

Response: {
  "status": 200,
  "message": "Chat completed",
  "data": {
    "input": "What's the return policy?",
    "output": "Our return policy allows...",
    "sources": ["shop-policies.txt"],
    "logs": ["12:30:01 User prompt: ...", "12:30:03 Found 3 documents..."]
  },
  "timestamp": "2026-02-24T..."
}
```

---

## 2. checks/ — Automated Quality Evaluation

Tests the AI against predefined question-answer pairs and scores the results.
Modeled after jmix-ai-backend's `checks/` package.

| File | Role |
|---|---|
| `CheckDef.java` | JPA entity — a Q&A pair (extends `BaseAuditableEntity`) |
| `CheckRun.java` | JPA entity — one batch execution (extends `BaseEntity`) |
| `CheckResult.java` | JPA entity — result of one check (extends `BaseEntity`) |
| `CheckDefRepository.java` | Repository — `findByActiveTrue()` |
| `CheckRunRepository.java` | Repository for check runs |
| `CheckResultRepository.java` | Repository — `findByCheckRunIdOrderByScoreAsc()` |
| `CheckRunner.java` | Runs all active CheckDefs in parallel, scores via ExternalEvaluator |
| `ExternalEvaluator.java` | Interface — semantic similarity scoring |
| `ExternalEvaluatorImpl.java` | Uses a ChatModel to evaluate (returns score 0-1 as JSON) |
| `CheckController.java` | REST endpoints for triggering and viewing check runs |
| `DefaultChecksInitializer.java` | Seeds 5 default Q&A pairs from `ai/default-check-defs.json` on first boot |

### How it works

```
POST /api/v1/checks/run
        │
        ▼
  CheckRunner.runChecks()
        │
        ├── Load all active CheckDefs from DB
        ├── For each (in parallel, configurable parallelism):
        │       ├── Send question through Chat pipeline (same as a real user)
        │       ├── Get actual answer
        │       └── ExternalEvaluator.evaluateSemantic(reference, actual)
        │               └── ChatModel returns JSON: { score, verdict, rationale, languageMatch }
        │
        ├── Save all CheckResults
        └── Compute average score → save to CheckRun
```

### Scoring rubric (ExternalEvaluator)

- Semantic correctness vs reference: 60%
- Completeness of key points: 30%
- Contradictions / hallucinations penalty: 10%
- Language mismatch: capped at 0.2 max score

### API

```
POST /api/v1/checks/run           → triggers a full check run
GET  /api/v1/checks/runs/{runId}  → view individual check results
```

### Configuration

```yaml
# application.yml
ai:
  checks:
    parallelism: 4   # number of concurrent check threads
```

---

## 3. config/ — Spring AI Configuration

| File | Role |
|---|---|
| `ChatClientConfig.java` | Creates `ChatClient` beans for OpenAI and Gemini |
| `ChatMemoryConfig.java` | Creates `ChatMemory` backed by `JdbcChatMemoryRepository` |
| `VectorStoreConfig.java` | Creates `PgVectorStore` with configurable schema, table, dimensions, distance |

### VectorStore configuration

All values are driven from `application.yml`:

```yaml
application-properties:
  rag-properties:
    schema: public
    table: ai_documents
    dimensions: 1536
    ingest-on-boot: true
    chunk-size: 800
    distance: COSINE    # COSINE | EUCLIDEAN | DOT
```

The config supports multiple distance types and uses Liquibase for schema management
(`initializeSchema: false`), unlike jmix-ai-backend which auto-creates the table.

---

## 4. ingestion/ — Data Ingestion Pipeline

Manages the flow of domain data into the VectorStore for RAG retrieval.

| File | Role |
|---|---|
| `Ingester.java` | Interface — `getType()` + `ingestAll()` |
| `AbstractIngester.java` | Base class with **hash-based change detection** |
| `PolicyIngester.java` | Concrete ingester for `shop-policies.txt` |
| `Chunker.java` | Interface for document splitting strategies |
| `IngesterService.java` | Manages all `Ingester` beans, batch or type-specific execution |
| `IngestionController.java` | REST endpoints for admin-triggered ingestion |

### Hash-based change detection (AbstractIngester)

Instead of deleting and re-embedding all documents on every ingestion run,
`AbstractIngester` computes an MD5 hash of each source's content and compares
it to the hash stored in the vector store metadata.

```
Source changed?  ──── NO  → skip (save embedding API cost)
       │
      YES
       │
       ├── Delete old chunks for this source
       └── Split + embed + store new chunks
```

This is modeled after jmix-ai-backend's `AbstractIngester` which uses Murmur3 hashing.

### Document metadata schema

Every document stored in the VectorStore carries this metadata:

```json
{
  "docType": "policy",
  "source": "shop-policies.txt",
  "sourceHash": "a1b2c3d4e5f6...",
  "size": 1234,
  "ingestedAt": "2026-02-24T10:30:00Z",
  "chunkIndex": 0
}
```

### Adding a new ingester

1. Create `XxxIngester extends AbstractIngester`
2. Implement `getType()`, `loadSources()`, `getSourceLimit()`, `loadDocument()`, `splitToChunks()`
3. Spring auto-discovers it via `@Component`
4. It will appear in `GET /api/v1/ingestion/types` automatically

### API

```
GET  /api/v1/ingestion/types        → ["policy"]
POST /api/v1/ingestion/run          → run ALL ingesters
POST /api/v1/ingestion/run/{type}   → run specific ingester
```

---

## 5. parameters/ — DB-Backed Configuration with YAML Fallback

AI behavior is configured via YAML stored in the database (`ai_parameters` table).
If no active row exists, the system falls back to the classpath default file.
This allows runtime editing of system prompts, tool parameters, and RAG tuning
without redeploying the application.

| File | Role |
|---|---|
| `AiParameters.java` | JPA entity — stores YAML content with `active` flag and `targetType` (extends `BaseAuditableEntity`) |
| `AiParametersRepository.java` | Spring Data repo — `findFirstByActiveTrueAndTargetType()`, `deactivateAllExcept()` |
| `ParametersService.java` | Core service — load from DB first, fallback to classpath; activate, copy, create |
| `ParametersReader.java` | Utility — reads nested values from a `Map` using dot-notation keys |
| `ParametersController.java` | REST endpoints — CRUD, activate, copy, create-from-default |

### How it works

```
ParametersService.loadReader(CHAT)
        │
        ├── Query DB: SELECT * FROM ai_parameters
        │              WHERE active = true AND target_type = 'CHAT'
        │
        ├── Found?  ─── YES → parse YAML content → ParametersReader
        │
        └── NOT found → load classpath:/ai/default-chat-params.yml → ParametersReader
```

### Database table: `ai_parameters`

| Column | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `version` | BIGINT | Optimistic locking |
| `description` | VARCHAR(500) | Human-readable label |
| `target_type` | VARCHAR(20) | `CHAT` or `SEARCH` |
| `active` | BOOLEAN | Only one active per type |
| `content` | TEXT | Full YAML content |
| `created_by` | UUID | Audit |
| `created_at` | TIMESTAMPTZ | Audit |
| `updated_by` | UUID | Audit |
| `updated_at` | TIMESTAMPTZ | Audit |

### API

```
GET    /api/v1/ai-parameters              → list all (optional ?type=CHAT)
GET    /api/v1/ai-parameters/{id}         → get detail (includes YAML content)
POST   /api/v1/ai-parameters              → create new parameter set
POST   /api/v1/ai-parameters/from-default → create from classpath YAML
PUT    /api/v1/ai-parameters/{id}         → update content
POST   /api/v1/ai-parameters/{id}/activate → activate (deactivates others of same type)
POST   /api/v1/ai-parameters/{id}/copy    → duplicate for A/B testing
DELETE /api/v1/ai-parameters/{id}         → delete (only if not active)
```

### Typical workflows

**Initial setup** — no DB rows exist yet:
1. `POST /api/v1/ai-parameters/from-default?type=CHAT`
2. System reads `default-chat-params.yml`, saves to DB, sets as active

**Tuning the system prompt**:
1. `POST /api/v1/ai-parameters/{id}/copy` → creates inactive duplicate
2. `PUT /api/v1/ai-parameters/{newId}` → edit YAML content
3. `POST /api/v1/ai-parameters/{newId}/activate` → swap to new version
4. If the new version performs worse → re-activate the old one

**A/B testing**:
1. Copy existing parameters
2. Modify system prompt or tool thresholds
3. Activate version B
4. Run `POST /api/v1/checks/run` to compare quality scores
5. Pick the winner

### Default YAML format: `resources/ai/default-chat-params.yml`

```yaml
description: Default shoes-shopping AI chat parameters

model:
  temperature: 0.2

tools:
  shop_policies_retriever:
    enabled: true
    description: "Search and return relevant shop policy text..."
    similarityThreshold: 0.0
    topK: 8
    minScore: 0.0
    topReranked: 5
    minRerankedScore: 0.0
    noResultsMessage: "I couldn't find a specific policy..."

postRetrievalProcessor:
  rules: []

systemMessage: |
  You are an AI shopping assistant for an online shoe store...
```

### How it's used in code

```java
// In ChatController — load system prompt
String base = parametersService.getSystemMessage();

// In ToolsManager — check if tool is enabled
ParametersReader reader = parametersService.loadReader();
if (reader.getBoolean("tools.shop_policies_retriever.enabled", true)) {
    tools.add(new PolicyTool(..., reader, ...));
}

// In AbstractRagTool — read tuning parameters
topK = reader.getInt("tools.shop_policies_retriever.topK", 10);
```

---

## 6. rag/ — RAG Tools & Retrieval

Implements the Retrieval-Augmented Generation pipeline.

| File | Role |
|---|---|
| `AbstractRagTool.java` | Base class — similarity search, post-processing, reranking, logging |
| `PolicyTool.java` | Concrete tool for shop policy retrieval |
| `ToolsManager.java` | Factory — creates enabled tools based on YAML config |
| `PostRetrievalProcessor.java` | Stub — filters retrieved docs (no-op currently) |
| `Reranker.java` | Stub — returns null, falls back to similarity scores |
| `Utils.java` | Shared utilities (dedup, formatting, logging helpers) |

### RAG tool execution flow

```
LLM decides to call tool "shop_policies_retriever"
        │
        ▼
  PolicyTool.execute(queryText)
        │
        ├── Build SearchRequest with filter: docType == "policy"
        ├── VectorStore.similaritySearch(request)
        ├── PostRetrievalProcessor.process(query, docs)
        ├── Reranker.rerank(query, docs, topReranked)
        │       └── null → fallback to minScore filtering
        ├── Log results
        └── Return document texts joined by "\n\n"
```

### Tool configuration (from YAML)

Each tool reads its parameters from `tools.<toolName>.*`:

| Parameter | Description | Default |
|---|---|---|
| `enabled` | Whether the tool is active | `true` |
| `description` | Tool description shown to LLM | required |
| `similarityThreshold` | Minimum similarity for VectorStore search | `0.0` |
| `topK` | Max documents to retrieve | `10` |
| `minScore` | Min score after retrieval (when reranker is off) | `0.0` |
| `topReranked` | How many docs to send to reranker | `5` |
| `minRerankedScore` | Min reranker score to keep | `0.0` |
| `noResultsMessage` | Fallback message when no docs found | `"No results found..."` |

### Adding a new RAG tool

1. Create `XxxTool extends AbstractRagTool` with a unique `toolName` and `docType`
2. Add config under `tools.<toolName>` in `default-chat-params.yml`
3. Register in `ToolsManager.getTools()` with `reader.getBoolean("tools.<toolName>.enabled")`

---

## 7. search/ — Standalone Vector Search

Executes RAG tools without involving the LLM. Useful for debugging retrieval quality.

| File | Role |
|---|---|
| `SearchService.java` | Runs all RAG tools against a query, returns raw documents |
| `SearchController.java` | REST endpoint `POST /api/v1/search` |

### API

```
POST /api/v1/search
Body: { "query": "return policy" }

Response: {
  "status": 200,
  "message": "Search completed",
  "data": [
    { "id": "...", "source": "shop-policies.txt", "content": "...", "score": 0.87 }
  ]
}
```

---

## Database Tables

All AI-related tables are created via Liquibase migrations:

| Table | Migration | Entity | Purpose |
|---|---|---|---|
| `ai_documents` | `002-create-vector-table.yaml` | (VectorStore managed) | PgVector embeddings |
| `ai_chat_logs` | `003-create-chat-logs.yaml` | `ChatLog` | Chat interaction logs |
| `ai_check_defs` | `004-create-check-tables.yaml` | `CheckDef` | Q&A test definitions |
| `ai_check_runs` | `004-create-check-tables.yaml` | `CheckRun` | Batch check execution |
| `ai_check_results` | `004-create-check-tables.yaml` | `CheckResult` | Individual check results |
| `ai_parameters` | `006-create-ai-parameters.yaml` | `AiParameters` | YAML config storage (versioned, activatable) |
| `spring_ai_chat_memory` | `001-create-chat-memory.yaml` | (Spring AI managed) | Conversation memory |

### Indexes

| Table | Index | Columns |
|---|---|---|
| `ai_chat_logs` | `idx_ai_chat_logs_conversation` | `conversation_id` |
| `ai_chat_logs` | `idx_ai_chat_logs_created` | `created_at` |
| `ai_check_defs` | `idx_check_def_active` | `active` |
| `ai_check_runs` | `idx_check_run_created` | `created_at` |
| `ai_parameters` | `idx_ai_params_active_type` | `active`, `target_type` |
| `ai_check_results` | `idx_check_result_run` | `check_run_id` |
| `ai_check_results` | `idx_check_result_def` | `check_def_id` |

---

## API Summary

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/chat` | Send a chat message |
| `POST` | `/api/v1/chat/stream` | Send a chat message (SSE stream) |
| `POST` | `/api/v1/search` | Raw vector search (no LLM) |
| `GET` | `/api/v1/ingestion/types` | List available ingester types |
| `POST` | `/api/v1/ingestion/run` | Run all ingesters |
| `POST` | `/api/v1/ingestion/run/{type}` | Run specific ingester |
| `POST` | `/api/v1/checks/run` | Trigger quality check run |
| `GET` | `/api/v1/checks/runs/{runId}` | View check run results |
| `GET` | `/api/v1/ai-parameters` | List parameter sets |
| `GET` | `/api/v1/ai-parameters/{id}` | Get parameter detail |
| `POST` | `/api/v1/ai-parameters` | Create new parameter set |
| `POST` | `/api/v1/ai-parameters/from-default` | Create from classpath YAML |
| `PUT` | `/api/v1/ai-parameters/{id}` | Update content |
| `POST` | `/api/v1/ai-parameters/{id}/activate` | Activate parameter set |
| `POST` | `/api/v1/ai-parameters/{id}/copy` | Copy for A/B testing |
| `DELETE` | `/api/v1/ai-parameters/{id}` | Delete (non-active only) |

---

## Configuration Reference

### application.yml

```yaml
application-properties:
  rag-properties:
    schema: public              # PostgreSQL schema
    table: ai_documents         # Vector store table name
    dimensions: 1536            # Embedding dimensions (must match model)
    ingest-on-boot: true        # Auto-ingest on startup if table is empty
    chunk-size: 800             # Token chunk size for text splitting
    distance: COSINE            # Distance type: COSINE | EUCLIDEAN | DOT

  chat-properties:
    max-request-length: 2000    # Max user message length

ai:
  parameters:
    path: classpath:/ai/default-chat-params.yml
  checks:
    parallelism: 4              # Concurrent threads for check runs

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.2
```

### Resource files

| File | Purpose |
|---|---|
| `resources/ai/default-chat-params.yml` | System prompt + tool config |
| `resources/ai/shop-policies.txt` | Shop policy documents for RAG |
| `resources/ai/default-check-defs.json` | Default Q&A pairs for quality checks |
