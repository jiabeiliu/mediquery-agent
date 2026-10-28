package com.mediquery.agent.web.dto;

/** Returned by {@code POST /api/ask}: pay this order, then capture it. */
public record AskOrderResponse(
        String orderId,
        String approvalUrl,
        String amount,
        String currency) {
}
