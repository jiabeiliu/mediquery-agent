package com.mediquery.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MediQuery product settings.
 */
@ConfigurationProperties(prefix = "mediquery")
public class MediQueryProperties {

    /**
     * Price charged per answered question, as a decimal string, e.g. "1.00".
     */
    private String pricePerQuery = "1.00";

    /**
     * ISO currency code, e.g. "USD".
     */
    private String currency = "USD";

    /**
     * Where the payer returns after approving on paypal.com (redirect flow).
     */
    private String returnUrl = "http://localhost:8080/";

    /**
     * Where the payer returns after cancelling on paypal.com (redirect flow).
     */
    private String cancelUrl = "http://localhost:8080/";

    public String getPricePerQuery() {
        return pricePerQuery;
    }

    public void setPricePerQuery(String pricePerQuery) {
        this.pricePerQuery = pricePerQuery;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}
