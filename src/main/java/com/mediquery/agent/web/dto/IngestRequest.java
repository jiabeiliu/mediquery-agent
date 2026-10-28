package com.mediquery.agent.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/ingest}. */
public record IngestRequest(
        @NotBlank String docId,
        @NotBlank String text) {
}
