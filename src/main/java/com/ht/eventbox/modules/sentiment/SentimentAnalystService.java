package com.ht.eventbox.modules.sentiment;

import com.ht.eventbox.enums.FeedbackSentimentType;
import com.ht.eventbox.modules.order.TicketItemRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class SentimentAnalystService {
    @Value("${sentiment.service.api.base.url}")
    private String apiBaseUrl;

    private final TicketItemRepository ticketItemRepository;

    @Builder
    @Data
    public static class PredictDto {
        private String text;
    }

    @Builder
    @Data
    public static class PredictResponseDto {
        private String text;
        private FeedbackSentimentType sentiment;
    }

    public FeedbackSentimentType predict(String feedback) {
        RestTemplate restTemplate = new RestTemplate();

        String url = apiBaseUrl + "/api/v1/sentiment/prediction";

        PredictDto request = PredictDto.builder()
                .text(feedback)
                .build();

        PredictResponseDto response = restTemplate.postForObject(url, request, PredictResponseDto.class);

        return response != null ? response.getSentiment() : null;
    }

    @Async
    @Transactional
    public void updateFeedbackSentiment(Long ticketItemId) {
        var ticketItem = ticketItemRepository.findById(ticketItemId).orElse(null);

        if (ticketItem == null) {
            return;
        }

        var sentiment = predict(ticketItem.getFeedback());
        if (sentiment == null) {
            return;
        }

        ticketItem.setFeedbackType(sentiment);
        ticketItemRepository.save(ticketItem);
    }
}
