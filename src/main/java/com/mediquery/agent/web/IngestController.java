package com.mediquery.agent.web;

import com.mediquery.agent.rag.service.IngestService;
import com.mediquery.agent.web.dto.IngestRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Document ingestion for demo setup: chunk -&gt; embed -&gt; store.
 * (Ported from the rag-medical-diagnosis {@code POST /api/ingest}.)
 */
@RestController
@RequestMapping("/api")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    /** Ingest a document: chunk it, embed the chunks, store them with vectors. */
    @PostMapping("/ingest")
    public Map<String, Object> ingest(@Valid @RequestBody IngestRequest request) {
        int chunks = ingestService.ingest(request.docId(), request.text());
        return Map.of("docId", request.docId(), "chunks", chunks);
    }
}
