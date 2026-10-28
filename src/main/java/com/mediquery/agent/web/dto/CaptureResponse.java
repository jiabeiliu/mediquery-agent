package com.mediquery.agent.web.dto;

import java.util.List;

/** Returned by {@code POST /api/ask/{orderId}/capture} on a COMPLETED capture. */
public record CaptureResponse(
        String answer,
        List<CitationDto> citations,
        String paymentId,
        String amount,
        String currency) {
}
