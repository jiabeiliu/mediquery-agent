# MediQuery Agent — Demo Video Script (2–3 min)

Recording: Mac QuickTime — Cmd+Shift+5 → record selected portion (browser window).
Mic on, speak the lines below while doing the actions. One continuous take is fine.

---

## Shot 1 — Hook (0:00–0:25), show the landing page

> "AI medical Q&A is powerful — but every query burns GPU cost, and worse,
> models hallucinate. What if you only paid for answers you actually need,
> and every answer came with receipts — literally?
> This is MediQuery Agent: pay-per-query medical Q&A, gated by PayPal."

## Shot 2 — Live demo: paid query (0:25–1:30)

Action: type "What are the common symptoms of type 2 diabetes?" → click Ask $1.00.

> "I type my question and hit Ask. One dollar, via PayPal sandbox —
> no real money moves. The PayPal checkout opens, I approve with the
> sandbox buyer account…"

Action: complete the sandbox payment, wait for the answer.

> "Payment captured. Only now does the RAG pipeline run — retrieve from
> pgvector, generate with a local LLM. And here's the answer, with
> citations: every claim traces to a source chunk. Plus the payment
> receipt, right below."

Action: scroll through Sources and Receipt.

## Shot 3 — Abstention (1:30–2:00)

Action: ask "What are the symptoms of asthma?" (not in the knowledge base),
pay $1, show the result.

> "And when the knowledge base doesn't have the answer? The agent abstains
> instead of guessing. No hallucinated citations — that's the whole point:
> worth paying for means grounded or nothing."

## Shot 4 — Architecture + close (2:00–2:30)

Action: show a simple architecture (draw on screen or show README diagram).

> "Under the hood: Spring Boot, PayPal Orders API for the paywall,
> pgvector retrieval plus local LLM for grounded answers. No payment,
> no answer — enforced server-side.
> Built for the PayPal AI Hackathon. Thanks for watching."

---

## Checklist before recording
- [ ] App running with PayPal sandbox credentials set
- [ ] Seed data ingested (python3 scripts/seed.py)
- [ ] Logged into PayPal sandbox buyer account in the browser
- [ ] Close unrelated tabs; hide bookmarks bar (Cmd+Shift+B)
- [ ] Do a dry run once before recording
