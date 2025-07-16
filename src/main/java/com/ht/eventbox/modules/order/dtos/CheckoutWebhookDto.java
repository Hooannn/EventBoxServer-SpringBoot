package com.ht.eventbox.modules.order.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutWebhookDto {
    private String id;

    @JsonProperty("event_type")
    private String eventType;

    private Resource resource;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource {
        private String id;
        private String status;
        private Payer payer;

        @JsonProperty("purchase_units")
        private List<PurchaseUnit> purchaseUnits;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PurchaseUnit {
        @JsonProperty("amount")
        private Amount amount;

        private String description;

        @JsonProperty("custom_id")
        private String customId;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        @JsonProperty("currency_code")
        private String currencyCode;

        private String value;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payer {
        @JsonProperty("email_address")
        private String emailAddress;

        @JsonProperty("payer_id")
        private String payerId;

        private Name name;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Name {
        @JsonProperty("given_name")
        private String givenName;

        @JsonProperty("surname")
        private String surname;
    }
}



