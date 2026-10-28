package com.mediquery.agent.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Minimal PayPal REST client built on Spring {@link RestClient} — no PayPal
 * SDK dependency. Talks to the real PayPal sandbox (or live) endpoints:
 *
 * <ul>
 *   <li>{@code POST /v1/oauth2/token} — client-credentials grant, Basic auth
 *       ({@code client-id:client-secret}). Token cached until expiry.</li>
 *   <li>{@code POST /v2/checkout/orders} — creates an order with
 *       {@code intent=CAPTURE}; returns the order id and the payer-approval link.</li>
 *   <li>{@code POST /v2/checkout/orders/{id}/capture} — captures an approved
 *       order; returns capture id, status and amount.</li>
 * </ul>
 */
@Service
public class PayPalClient {

    private final PayPalProperties props;
    private final RestClient restClient;

    // Token cache. Guarded by the intrinsic lock.
    private String cachedToken;
    private Instant tokenExpiresAt;

    public PayPalClient(PayPalProperties props, RestClient.Builder builder) {
        this.props = props;
        this.restClient = builder
                .baseUrl(props.getBaseUrl())
                .build();
    }

    private void requireCredentials() {
        if (props.getClientId() == null || props.getClientId().isBlank()
                || props.getClientId().startsWith("TODO")
                || props.getClientSecret() == null || props.getClientSecret().isBlank()
                || props.getClientSecret().startsWith("TODO")) {
            throw new PayPalException(
                    "PayPal credentials are not configured. Set PAYPAL_CLIENT_ID and "
                            + "PAYPAL_CLIENT_SECRET (see application.yml).");
        }
    }

    /**
     * OAuth2 client-credentials token, cached until ~60s before expiry.
     */
    public synchronized String getAccessToken() {
        requireCredentials();
        if (cachedToken != null && tokenExpiresAt != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        String basic = Base64.getEncoder().encodeToString(
                (props.getClientId() + ":" + props.getClientSecret())
                        .getBytes(StandardCharsets.UTF_8));
        try {
            JsonNode root = restClient.post()
                    .uri("/v1/oauth2/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(JsonNode.class);
            cachedToken = root.path("access_token").asText(null);
            long expiresIn = root.path("expires_in").asLong(3600);
            if (cachedToken == null) {
                throw new PayPalException("PayPal token response had no access_token: " + root);
            }
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
            return cachedToken;
        } catch (HttpStatusCodeException e) {
            throw new PayPalException("PayPal token request failed (HTTP "
                    + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(),
                    e.getStatusCode().value());
        } catch (PayPalException e) {
            throw e;
        } catch (Exception e) {
            throw new PayPalException("PayPal token request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a CAPTURE-intent order. Returns the order id and approval link.
     */
    public OrderInfo createOrder(String amount, String currency,
                                 String returnUrl, String cancelUrl) {
        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "amount", Map.of(
                                "currency_code", currency,
                                "value", amount))),
                "application_context", Map.of(
                        "brand_name", "MediQuery Agent",
                        "return_url", returnUrl,
                        "cancel_url", cancelUrl,
                        "user_action", "PAY_NOW"));
        try {
            JsonNode root = post("/v2/checkout/orders", body);
            String id = root.path("id").asText(null);
            String approvalUrl = null;
            for (JsonNode link : root.path("links")) {
                if ("approve".equals(link.path("rel").asText())) {
                    approvalUrl = link.path("href").asText(null);
                }
            }
            if (id == null) {
                throw new PayPalException("PayPal order response had no id: " + root);
            }
            return new OrderInfo(id, root.path("status").asText(""), approvalUrl);
        } catch (HttpStatusCodeException e) {
            throw new PayPalException("PayPal create-order failed (HTTP "
                    + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(),
                    e.getStatusCode().value());
        } catch (PayPalException e) {
            throw e;
        } catch (Exception e) {
            throw new PayPalException("PayPal create-order failed: " + e.getMessage(), e);
        }
    }

    /**
     * Captures an approved order. Returns capture id, status and amount.
     */
    public CaptureInfo captureOrder(String orderId) {
        try {
            JsonNode root = post("/v2/checkout/orders/" + orderId + "/capture", Map.of());
            String status = root.path("status").asText("");
            // purchase_units[0].payments.captures[0]
            JsonNode capture = root.path("purchase_units").path(0)
                    .path("payments").path("captures").path(0);
            String captureId = capture.path("id").asText(null);
            String captureStatus = capture.path("status").asText(status);
            String amount = capture.path("amount").path("value").asText(null);
            String currency = capture.path("amount").path("currency_code").asText(null);
            return new CaptureInfo(captureId, captureStatus, amount, currency);
        } catch (HttpStatusCodeException e) {
            throw new PayPalException("PayPal capture failed (HTTP "
                    + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(),
                    e.getStatusCode().value());
        } catch (PayPalException e) {
            throw e;
        } catch (Exception e) {
            throw new PayPalException("PayPal capture failed: " + e.getMessage(), e);
        }
    }

    private JsonNode post(String uri, Map<String, Object> body) {
        return restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    public record OrderInfo(String orderId, String status, String approvalUrl) {
    }

    public record CaptureInfo(String captureId, String status, String amount, String currency) {
    }
}
