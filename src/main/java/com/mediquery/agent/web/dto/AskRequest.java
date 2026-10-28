package com.mediquery.agent.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/ask}. */
public record AskRequest(
        @NotBlank String question) {
}
