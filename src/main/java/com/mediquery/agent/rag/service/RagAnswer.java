package com.mediquery.agent.rag.service;

import java.util.List;

/**
 * The result of a RAG answer: the generated text plus the retrieved
 * chunks that ground it. When retrieval finds nothing relevant, the
 * generation prompt instructs the model to say so explicitly instead
 * of hallucinating (abstention).
 */
public record RagAnswer(
        String answer,
        List<ScoredChunk> sources) {
}
