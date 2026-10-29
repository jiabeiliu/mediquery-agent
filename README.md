# MediQuery Agent

**Pay-per-query AI medical Q&A agent** — grounded answers with citations, powered by RAG.
You only pay for the answers you need.

Built for the **PayPal AI Hackathon** (deadline 2026-11-12).

## Functional demo for judges — free local setup

**[Open the complete demo setup and testing guide](docs/JUDGES.md)** · **[Watch the 1:43 demonstration video](https://www.youtube.com/watch?v=ZNIy8Wo2Ko8)**

The functional demo runs locally with Java, PostgreSQL/pgvector, and Ollama. There is no hosted application and no paid AI API is required. The guide covers database creation, model downloads, free PayPal sandbox configuration, seed data, and step-by-step success, abstention, and payment-gate checks. Evaluators use their own free sandbox app and buyer account; no secrets are published in the repository. All demonstration payments are sandbox transactions, with no real money charged.

## How it works

1. You type a medical question and hit **Ask**.
2. The backend creates a **PayPal sandbox order** for the price-per-query ($1.00).
3. You approve the payment with the PayPal button (sandbox — no real money).
4. The backend **captures** the payment, then runs the RAG pipeline:
   embed → pgvector retrieval → local LLM generates a grounded answer **with citations**.
5. If the knowledge base has nothing relevant, the model **abstains** instead of
   hallucinating ("no grounded answer" is intentional anti-hallucination behavior).

## Architecture

```
                    ┌─────────────────────────────┐
                    │        Demo UI (index.html)  │
                    │  textarea → PayPal JS SDK   │
                    │  → answer + citations       │
                    └──────────────┬──────────────┘
                                   │  REST
                    ┌──────────────▼──────────────┐
                    │   Spring Boot 3.2 (Java 17)  │
                    │  ┌────────────────────────┐ │
                    │  │ AskController          │ │
                    │  │  POST /api/ask         │ │──▶ PayPalClient ──▶ api-m.sandbox.paypal.com
                    │  │  POST /api/ask/{id}/   │ │    (orders v2, no SDK)
                    │  │        capture ──┐     │ │
                    │  └──────────────────┼─────┘ │
                    │                     ▼       │
                    │  ┌─────────────────────────────────┐
                    │  │ RAG engine (ported 1:1 from     │
                    │  │ rag-medical-diagnosis)          │
                    │  │ chunk → embed → pgvector → LLM  │
                    │  └────────┬───────────────┬────────┘
                    └───────────┼───────────────┼─────────
                 ┌──────────────▼──┐      ┌──────▼────────┐
                 │ PostgreSQL +    │      │ Ollama        │
                 │ pgvector        │      │ nomic-embed-  │
                 │ (document_chunk)│      │ text +        │
                 └─────────────────┘      │ llama3.1:8b   │
                                          └───────────────┘
```

## API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/config` | Public demo config: `{pricePerQuery, currency, paypalClientId, mode}`. The secret is never exposed. |
| `POST` | `/api/ask` | Body `{question}` → creates a PayPal order → `{orderId, approvalUrl, amount, currency}` |
| `POST` | `/api/ask/{orderId}/capture` | Captures payment; on `COMPLETED` runs RAG → `{answer, citations: [{chunkId, docId, text}], paymentId, amount, currency}`. Failed capture → HTTP 402 `{error}`. |
| `POST` | `/api/ingest` | Body `{docId, text}` → `{docId, chunks}`. For loading demo documents. |

## Run locally

### 1. Prerequisites

- Java 17, Maven
- PostgreSQL with the `pgvector` extension
- Ollama with `nomic-embed-text` and `llama3.1:8b` pulled

Follow [the judge setup guide](docs/JUDGES.md#2-create-a-dedicated-local-demonstration-database) to install pgvector for your active PostgreSQL version, create the database with the correct owner, and pull both Ollama models.

### 2. PayPal sandbox app (required for payments)

1. Go to https://developer.paypal.com/dashboard/applications/sandbox and log in.
2. Create a REST App (or use the default one) and copy its **Client ID** and **Secret**.
3. In the sandbox, create a **buyer test account** (for approving payments) at
   https://developer.paypal.com/dashboard/accounts.

```bash
export PAYPAL_CLIENT_ID="your-sandbox-client-id"
export PAYPAL_CLIENT_SECRET="your-sandbox-secret"
# optional overrides:
# export DB_URL=jdbc:postgresql://localhost:5432/mediquery
# export DB_USER=mediquery DB_PASSWORD=mediquery
# export PRICE_PER_QUERY=1.00 CURRENCY=USD
```

### 3. Build & run

```bash
cd ~/workspace/mediquery-agent
mvn -q compile          # or: mvn package
mvn spring-boot:run
# open http://localhost:8080/
```

### 4. Load demo documents

```bash
# Run once against a fresh demo database, while the app is running.
python3 scripts/seed.py
```

Then ask questions in the UI, approve with the sandbox buyer account, and get
a cited answer.

## Status notes (honest)

- The **RAG engine is ported 1:1** from the working `rag-medical-diagnosis`
  project (same chunking, embeddings, retrieval, prompts, abstention behavior).
  That project's full pipeline was verified end-to-end on macOS.
- The **PayPal module wires the real sandbox REST endpoints**
  (`/v1/oauth2/token`, `/v2/checkout/orders`, `/…/capture`) with no SDK.
  The PayPal sandbox payment flow has been verified with sandbox credentials.
- Pending questions are held **in memory** keyed by PayPal order id (demo-grade;
  a restart loses them, no TTL). Fine for the hackathon demo, not for production.
- The demo UI is a single static file; the PayPal JS SDK is loaded from
  paypal.com with the sandbox client ID from `/api/config`.
- Medical answers are generated from whatever you ingest; this is a demo, not
  medical advice (the UI says so).
- Abstention and correct citations are model behaviors to evaluate, not formal guarantees. Retrieved source cards can appear even when they do not answer the question.
- The app does not implement persistent payer/query history or automatic refunds for abstained answers. Pending orders are in memory.
