package com.mediquery.agent.web.dto;

/**
 * Public demo config. The client ID is public by design (it is embedded in
 * the PayPal JS SDK script tag); the client secret is never exposed.
 */
public record ConfigResponse(
        String pricePerQuery,
        String currency,
        String paypalClientId,
        String mode) {
}
