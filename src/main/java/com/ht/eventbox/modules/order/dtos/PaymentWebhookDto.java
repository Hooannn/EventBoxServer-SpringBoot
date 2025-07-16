package com.ht.eventbox.modules.order.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentWebhookDto {
    private String id;

    @JsonProperty("event_type")
    private String eventType;

    private Resource resource;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource {
        private String id;
        private String status;

        @JsonProperty("final_capture")
        private boolean finalCapture;

        @JsonProperty("supplementary_data")
        private SupplementaryData supplementaryData;

        @JsonProperty("seller_receivable_breakdown")
        private SellerReceivableBreakdown sellerReceivableBreakdown;

        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SellerReceivableBreakdown {
            @JsonProperty("gross_amount")
            private Money grossAmount;
            @JsonProperty("paypal_fee")
            private Money paypalFee;
            @JsonProperty("net_amount")
            private Money netAmount;

            @Getter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Money {
                @JsonProperty("currency_code")
                private String currencyCode;
                private String value;
            }
        }
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SupplementaryData {
        @JsonProperty("related_ids")
        private RelatedIds relatedIds;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelatedIds {
        @JsonProperty("order_id")
        private String orderId;
    }
}