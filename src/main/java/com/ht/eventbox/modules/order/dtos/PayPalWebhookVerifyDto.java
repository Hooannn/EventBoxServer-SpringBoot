package com.ht.eventbox.modules.order.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class PayPalWebhookVerifyDto {
    @JsonProperty("transmission_id")
    private String transmissionId;

    @JsonProperty("transmission_time")
    private String transmissionTime;

    @JsonProperty("transmission_sig")
    private String transmissionSig;

    @JsonProperty("cert_url")
    private String certUrl;

    @JsonProperty("auth_algo")
    private String authAlgo;

    @JsonProperty("webhook_id")
    private String webhookId;

    @JsonProperty("webhook_event")
    private JsonNode webhookEvent;
}
