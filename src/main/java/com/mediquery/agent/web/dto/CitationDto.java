package com.mediquery.agent.web.dto;

/** One grounding citation for an answer. */
public record CitationDto(
        long chunkId,
        String docId,
        String text) {
}
