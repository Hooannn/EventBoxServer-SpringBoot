package com.ht.eventbox.modules.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.Notification;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushNotificationJobHandler implements RedisBackgroundJobHandler {
    private static final TypeReference<Map<String, String>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final PushNotificationService pushNotificationService;
    private final ObjectMapper objectMapper;

    @Override
    public BackgroundJobType supports() {
        return BackgroundJobType.SEND_PUSH_NOTIFICATION;
    }

    @Override
    public void handle(BackgroundJobEnvelope envelope) throws Exception {
        Map<String, String> payload = objectMapper.readValue(envelope.payload(), PAYLOAD_TYPE);
        List<Long> userIds = parseUserIds(required(payload, "userIds"));
        String title = required(payload, "title");
        String body = required(payload, "body");
        String image = payload.get("image");

        Map<String, String> data = new LinkedHashMap<>(payload);
        data.remove("userIds");
        data.remove("title");
        data.remove("body");
        data.remove("image");

        var notificationBuilder = Notification.builder()
                .setTitle(title)
                .setBody(body);
        if (image != null && !image.isBlank()) {
            notificationBuilder.setImage(image);
        }

        pushNotificationService.push(userIds, notificationBuilder.build(), data);
    }

    private List<Long> parseUserIds(String rawUserIds) {
        return List.of(rawUserIds.split(","))
                .stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing payload field: " + key);
        }
        return value;
    }
}
