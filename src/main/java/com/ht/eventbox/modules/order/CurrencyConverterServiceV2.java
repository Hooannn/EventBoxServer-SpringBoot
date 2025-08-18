package com.ht.eventbox.modules.order;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CurrencyConverterServiceV2 {
    private static final String EXCHANGE_RATE_API = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json";
    private static final String SGD_EXCHANGE_RATE_API = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/sgd.json";

    public double convertVndToUsd(double vndAmount) throws IOException {
        double exchangeRate = getExchangeRate();
        return BigDecimal.valueOf(vndAmount)
                .divide(BigDecimal.valueOf(exchangeRate), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public double convertVndToSgd(double vndAmount) throws IOException {
        double exchangeRate = getSgdExchangeRate();
        return BigDecimal.valueOf(vndAmount)
                .divide(BigDecimal.valueOf(exchangeRate), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static class ExchangeRateResponse {
        public String date;
        public Map<String, Double> usd;
    }

    private static class SgdExchangeRateResponse {
        public String date;
        public Map<String, Double> sgd;
    }

    public double getExchangeRate() throws IOException {
        return fetchRateFromApi();
    }
    /**
     * Gọi ExchangeRate-API để lấy tỷ giá mới
     */
    private double fetchRateFromApi() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        ExchangeRateResponse response = restTemplate.getForObject(EXCHANGE_RATE_API, ExchangeRateResponse.class);

        if (response == null || response.usd == null || response.usd.get("vnd") == null) {
            throw new RuntimeException("Không thể lấy tỷ giá từ API");
        }
        return response.usd.get("vnd");
    }

    public double getSgdExchangeRate() throws IOException {
        return fetchSgdRateFromApi();
    }

    /**
     * Gọi ExchangeRate-API để lấy tỷ giá mới
     */
    private double fetchSgdRateFromApi() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        SgdExchangeRateResponse response = restTemplate.getForObject(SGD_EXCHANGE_RATE_API, SgdExchangeRateResponse.class);

        if (response == null || response.sgd == null || response.sgd.get("vnd") == null) {
            throw new RuntimeException("Không thể lấy tỷ giá từ API");
        }
        return response.sgd.get("vnd");
    }
}