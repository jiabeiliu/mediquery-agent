package com.mediquery.agent.paypal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PayPal connection settings. Credentials come from environment variables —
 * never hardcode them.
 */
@ConfigurationProperties(prefix = "paypal")
public class PayPalProperties {

    /**
     * Sandbox base URL, e.g. https://api-m.sandbox.paypal.com.
     */
    private String baseUrl = "https://api-m.sandbox.paypal.com";

    /**
     * REST App client ID (public; also exposed to the demo UI via /api/config).
     */
    private String clientId;

    /**
     * REST App client secret. NEVER exposed via any API.
     */
    private String clientSecret;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
