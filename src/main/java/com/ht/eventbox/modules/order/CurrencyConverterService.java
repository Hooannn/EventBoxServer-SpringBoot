package com.ht.eventbox.modules.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CurrencyConverterService {

    @Value("${amdoren.api.key}")
    private String apiKey;

    private static final String EXCHANGE_RATE_API = "https://www.amdoren.com/api/currency.php?api_key=%s&from=USD&to=VND&amount=1";

    public double convertVndToUsd(double vndAmount) throws IOException {
        double exchangeRate = getExchangeRate();
        return BigDecimal.valueOf(vndAmount)
                .divide(BigDecimal.valueOf(exchangeRate), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static class ExchangeRateResponse {
        public int error;
        public String error_message;
        public double amount;
    }

    public double getExchangeRate() throws IOException {
        return fetchRateFromApi();
    }

    /**
     * Gọi ExchangeRate-API để lấy tỷ giá mới
     */
    private double fetchRateFromApi() throws IOException {
        String apiUrl = String.format(EXCHANGE_RATE_API, apiKey);

        RestTemplate restTemplate = new RestTemplate();
        ExchangeRateResponse response = restTemplate.getForObject(apiUrl, ExchangeRateResponse.class);

        if (response == null || response.error != 0) {
            throw new RuntimeException("Không thể lấy tỷ giá từ API");
        }
        return response.amount;
    }
}