package com.mediquery.agent.web;

import com.mediquery.agent.config.MediQueryProperties;
import com.mediquery.agent.paypal.PayPalClient;
import com.mediquery.agent.paypal.PayPalException;
import com.mediquery.agent.paypal.PayPalProperties;
import com.mediquery.agent.rag.service.ChatService;
import com.mediquery.agent.rag.service.RagAnswer;
import com.mediquery.agent.web.dto.AskOrderResponse;
import com.mediquery.agent.web.dto.AskRequest;
import com.mediquery.agent.web.dto.CaptureResponse;
import com.mediquery.agent.web.dto.CitationDto;
import com.mediquery.agent.web.dto.ConfigResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Pay-per-query orchestration:
 *
 * <ol>
 *   <li>{@code POST /api/ask} — creates a PayPal order for one query;
 *       the payer approves it (PayPal JS SDK or approval link).</li>
 *   <li>{@code POST /api/ask/{orderId}/capture} — captures the payment; on a
 *       {@code COMPLETED} capture the RAG pipeline answers the stored
 *       question. Failed captures return HTTP 402.</li>
 * </ol>
 *
 * <p>Questions are held in memory keyed by PayPal order id between the two
 * calls. This is demo-grade by design (no DB table, no TTL) — see README.
 */
@RestController
@RequestMapping("/api")
public class AskController {

    private static final int TOP_K = 5;

    private final PayPalClient payPalClient;
    private final ChatService chatService;
    private final MediQueryProperties mediQueryProps;
    private final PayPalProperties payPalProps;

    private final Map<String, String> pendingQuestions = new ConcurrentHashMap<>();

    public AskController(PayPalClient payPalClient,
                         ChatService chatService,
                         MediQueryProperties mediQueryProps,
                         PayPalProperties payPalProps) {
        this.payPalClient = payPalClient;
        this.chatService = chatService;
        this.mediQueryProps = mediQueryProps;
        this.payPalProps = payPalProps;
    }

    /** Public demo config. The client ID is public; the secret is never exposed. */
    @GetMapping("/config")
    public ConfigResponse config() {
        return new ConfigResponse(
                mediQueryProps.getPricePerQuery(),
                mediQueryProps.getCurrency(),
                payPalProps.getClientId(),
                "sandbox");
    }

    /**
     * Start a paid question: creates a PayPal order for
     * {@code mediquery.price-per-query} and remembers the question.
     */
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@Valid @RequestBody AskRequest request) {
        try {
            PayPalClient.OrderInfo order = payPalClient.createOrder(
                    mediQueryProps.getPricePerQuery(),
                    mediQueryProps.getCurrency(),
                    mediQueryProps.getReturnUrl(),
                    mediQueryProps.getCancelUrl());
            pendingQuestions.put(order.orderId(), request.question().strip());
            return ResponseEntity.ok(new AskOrderResponse(
                    order.orderId(), order.approvalUrl(),
                    mediQueryProps.getPricePerQuery(), mediQueryProps.getCurrency()));
        } catch (PayPalException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Could not create PayPal order: " + e.getMessage()));
        }
    }

    /**
     * Capture the approved payment, then answer the stored question with RAG.
     * Non-completed captures (or unknown orders) return HTTP 402.
     */
    @PostMapping("/ask/{orderId}/capture")
    public ResponseEntity<?> capture(@PathVariable String orderId) {
        String question = pendingQuestions.get(orderId);
        if (question == null) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("error", "Unknown or already-captured order. Ask again to create a new order."));
        }
        PayPalClient.CaptureInfo capture;
        try {
            capture = payPalClient.captureOrder(orderId);
        } catch (PayPalException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "PayPal capture failed: " + e.getMessage()));
        }
        if (!"COMPLETED".equalsIgnoreCase(capture.status())) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("error", "Payment not completed (status: " + capture.status() + ")."));
        }
        // Payment is good: consume the order so it cannot be double-captured.
        pendingQuestions.remove(orderId);

        RagAnswer rag = chatService.answer(question, TOP_K);
        return ResponseEntity.ok(new CaptureResponse(
                rag.answer(),
                rag.sources().stream()
                        .map(s -> new CitationDto(s.chunkId(), s.docId(), s.content()))
                        .collect(Collectors.toList()),
                capture.captureId(), capture.amount(), capture.currency()));
    }
}
