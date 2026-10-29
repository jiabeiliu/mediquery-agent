# Run the functional MediQuery Agent demo locally

This is the working demo entry point for the PayPal AI Hackathon. **There is no hosted application or hosting fee.** Judges can build and interact with the actual Java application using the steps below. This is not a static mockup. All payments use PayPal sandbox; no real money is charged. The application and local AI inference require no paid API subscription.

- Source: https://github.com/jiabeiliu/mediquery-agent
- Demonstration video: https://www.youtube.com/watch?v=ZNIy8Wo2Ko8
- Expected flow: question → sandbox order → buyer approval → server-side capture → retrieval and answer → sources and receipt.

## 1. Install the prerequisites

Use a desktop computer with Java 17+, Maven, Python 3, PostgreSQL with the pgvector extension, and Ollama. Internet access is needed for dependencies, model downloads, and PayPal sandbox. The two models need several GB of disk space; 16 GB of RAM is a practical target. CPU generation may be slow. The submitted application has been demonstrated on macOS; the commands below use a POSIX shell.

Official installation sources:

- Java: https://adoptium.net/
- Maven: https://maven.apache.org/install.html
- PostgreSQL: https://www.postgresql.org/download/
- pgvector: https://github.com/pgvector/pgvector#installation
- Ollama: https://ollama.com/download
- Python: https://www.python.org/downloads/

Install pgvector for the PostgreSQL server version you actually run. If PostgreSQL is already installed, you do not need a second server. Ensure that PostgreSQL and Ollama are running.

```bash
git clone https://github.com/jiabeiliu/mediquery-agent.git
cd mediquery-agent
ollama pull nomic-embed-text
ollama pull llama3.1:8b
```

If Ollama is not running, start its desktop application or run `ollama serve` in another terminal. Verify the models with `ollama list`.

## 2. Create a dedicated local demonstration database

In a fresh installation, connect using your local PostgreSQL administrator account. These commands create only the dedicated demo role and database; skip creation if they already exist. The password shown here is for a local demonstration database only.

```bash
psql postgres -c "CREATE ROLE mediquery LOGIN PASSWORD 'mediquery';"
psql postgres -c "CREATE DATABASE mediquery OWNER mediquery;"
psql mediquery -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

If your administrator account requires explicit connection options, add `-U YOUR_ADMIN_USER -h localhost` to the commands. Creating the extension may require administrator privileges. The application creates its own `document_chunk` table on startup; the `mediquery` role must own the database/schema or have table-creation privileges in `public`.

```bash
export DB_URL='jdbc:postgresql://localhost:5432/mediquery'
export DB_USER='mediquery'
export DB_PASSWORD='mediquery'
```

## 3. Configure free PayPal sandbox access

1. Sign in at https://developer.paypal.com/dashboard/applications/sandbox and create or select a **sandbox REST application**.
2. Use its sandbox Client ID and Secret as the application environment variables below. The secret stays on the local server; never commit it or place it in Devpost's public fields.
3. At https://developer.paypal.com/dashboard/accounts, obtain a **Personal sandbox buyer account**. Use this buyer account, separate from the seller/business account, to approve checkout. Do not use a real card or live PayPal credentials.

In the same terminal that will start the application:

```bash
export PAYPAL_CLIENT_ID='YOUR_SANDBOX_APP_CLIENT_ID'
read -r -s -p 'Sandbox app secret: ' PAYPAL_CLIENT_SECRET
printf '\n'
export PAYPAL_CLIENT_SECRET
export PAYPAL_BASE_URL='https://api-m.sandbox.paypal.com'
export RETURN_URL='http://localhost:8080/'
export CANCEL_URL='http://localhost:8080/'
export LLM_PROVIDER='ollama'
export LLM_BASE_URL='http://localhost:11434/v1'
export LLM_EMBEDDING_MODEL='nomic-embed-text'
export LLM_CHAT_MODEL='llama3.1:8b'
```

The secret-entry command above uses **Bash**; if your terminal uses another shell, run `bash` before these exports. No OpenAI key is needed. Each evaluator uses their own free sandbox application and sandbox buyer account; no developer secrets or buyer passwords are published in this repository.

## 4. Build and start

```bash
mvn -B package
java -jar target/mediquery-agent-0.1.0.jar
```

Leave that terminal running. If Maven reports a Java-version mismatch, point `JAVA_HOME` to a Java 17+ JDK and check `mvn -version`. If port 8080 is occupied by an existing copy of this demo, use that copy or stop it before starting another.

In a **second terminal**, from the repository directory:

```bash
curl --fail http://localhost:8080/api/config
python3 scripts/seed.py
```

Run `seed.py` **once for a fresh demo database**; it inserts three repository-provided sample texts, covering type 2 diabetes, hypertension, and cold versus flu. Running it repeatedly inserts duplicates. These are small demonstration texts, not a comprehensive or clinically validated medical corpus.

Open http://localhost:8080/ in a browser. The public configuration should report `mode: sandbox` and price `1.00` USD. It must not contain a client secret. If you use a different database or Ollama host, adjust the environment variables before startup.

## 5. Test the actual workflow

### A. Successful paid question

1. Enter **What are the common symptoms of type 2 diabetes?**
2. Click **Ask — $1.00**, then use the PayPal checkout button.
3. Approve using the **Personal sandbox buyer account**. No real money moves.
4. Wait for local model generation. The backend runs RAG only after a completed capture.
5. Inspect the answer, its chunk references, the retrieved source cards, and the capture receipt showing `1.00 USD`.

### B. Question outside the demonstration knowledge base

1. Enter **What are the symptoms of asthma?** and complete a separate sandbox checkout.
2. The demonstration texts do not cover asthma. The prompt instructs the model to say the context does not contain the answer; the submitted recording shows this abstention.
3. Retrieved source cards can still appear because the application returns nearest neighbors, even when they do not answer the question. These cards are retrieval evidence, not proof that an unsupported answer is correct. Model outputs can vary; abstention and citation correctness are not formally guaranteed by the implementation.

### C. Cancel and server-side payment gate

Create another order, then cancel checkout. No new answer should be generated for that order. A previous answer may remain visible; it is not a response to the cancelled question.

An unknown order cannot retrieve an answer. This negative check creates no payment:

```bash
curl -i -X POST http://localhost:8080/api/ask/not-a-real-order/capture
```

Expected: HTTP `402` with an unknown/already-captured order error. The actual endpoints are `POST /api/ask` and `POST /api/ask/{orderId}/capture`; this repository does not expose `/api/query`.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Database connection fails | PostgreSQL service, DB_URL port/database, role password and privileges. |
| `extension vector is not available` | Install pgvector for the active PostgreSQL server version, then create the extension as an administrator. |
| Ollama connection refused | Start Ollama and confirm `curl http://localhost:11434/api/tags` works. |
| Model not found / embedding mismatch | Pull the two specified models; use 768-dimensional nomic embeddings with the unchanged vector(768) schema. |
| Checkout fails | Check sandbox app credentials, internet access, popup settings, and that buyer and seller sandbox accounts differ. |
| Answer generation takes time | The first local model load can be slow; keep the server running and inspect its terminal. |
| No useful answer for the diabetes example | Confirm seed ingestion succeeded and that the database is the same one used by the application. |
| Duplicate source cards | The seed script may have been run multiple times. Use a new dedicated demo database for a clean run. |

## Validation and limits

The source at commit `addc76662480954510c554994a8080bc33c5fb85` was successfully packaged on macOS on October 29, 2026. The packaged JAR was started separately against the existing local demonstration database: the homepage and sandbox configuration responded successfully, and an unknown-order capture returned HTTP 402. This separate startup used placeholder PayPal credentials and did not create or capture payments. The repository currently has no automated test suite. The submitted video demonstrates the sandbox payment and RAG UI; build success alone does not certify all payment or model behavior.

Pending orders are stored in memory and are lost on restart. The ingestion endpoint has no authentication; keep this local demonstration bound to a trusted environment. The application has no persistent payer/query-history feature, no automatic refund for an abstained answer, and no clinical-diagnosis guarantee. This is an educational prototype, not medical advice.
