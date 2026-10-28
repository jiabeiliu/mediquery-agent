# MediQuery Agent — Devpost Project Story

> Status: final draft 2026-10-12. End-to-end verified on sandbox:
> $1 payment → RAG answer with citations → receipt.

## Inspiration
Medical Q&A agents are powerful but expensive to run — every query burns
GPU/LLM cost whether the answer is useful or not. What if you only paid
for the answers you actually needed, the way you pay per API call?
MediQuery Agent explores pay-per-query as a monetization primitive for
AI agents, built on PayPal's agentic-commerce rails.

## What it does
MediQuery Agent is a pay-per-query AI medical Q&A agent:
1. You type a medical question.
2. You pay a small fixed fee (default $1.00) via PayPal **sandbox**
   (Orders API v2: create → approve → capture).
3. Only after payment capture succeeds, the agent runs a RAG pipeline
   (chunking → embeddings → pgvector retrieval → grounded generation)
   and returns an answer **with citations** — or explicitly abstains
   ("no grounded answer") when retrieval confidence is too low,
   instead of hallucinating.

No payment, no answer (HTTP 402). Every answer is traceable to its
sources; every query is traceable to its payment.

## How we built it
- **RAG engine** (Java 17 / Spring Boot 3.2): ported from our working
  `rag-medical-diagnosis` service — Ollama embeddings (nomic-embed-text),
  pgvector retrieval, llama3.1:8b generation, hit@k/MRR eval harness.
- **PayPal sandbox module**: hand-rolled REST client (no SDK) —
  OAuth2 client-credentials token, Orders API create/capture, token
  caching, graceful failure when credentials are absent.
- **Orchestration**: `POST /api/ask` creates the order;
  `POST /api/ask/{orderId}/capture` captures payment, then runs RAG;
  `GET /api/config` exposes price + sandbox client ID (secret never
  leaves the server).
- **Demo UI**: single-page app — ask → PayPal sandbox button →
  cited answer + payment receipt.

## Challenges we ran into
- Making the payment gate *real* rather than a button: the RAG pipeline
  only executes after a successful capture, enforced server-side.
- Keeping the RAG engine's anti-hallucination behavior (abstention)
  intact through the port.
- Sandbox-only testing: no real money moves; all flows use PayPal's
  sandbox environment and test buyer accounts.

## Accomplishments that we're proud of
- A genuinely payment-gated AI agent — not a demo with a fake paywall.
- Grounded answers with citations, plus explicit abstention instead
  of hallucinations.
- Clean, hackathon-demoable architecture a two-person team can
  extend (teammate: boyfriend, Devpost team formed).

## What we learned
PayPal's Orders API maps cleanly onto agentic pay-per-use flows;
the hard part isn't the payment — it's making the agent worth
paying for (grounding + abstention).

## What's next
- Query credits / bundles (e.g., 10 queries for $8).
- Streaming answers; more document domains.
- Usage analytics per query for the "worth paying for" loop.
