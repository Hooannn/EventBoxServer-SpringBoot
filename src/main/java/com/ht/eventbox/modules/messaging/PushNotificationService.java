package com.ht.eventbox.modules.messaging;

import com.google.firebase.messaging.*;
import com.ht.eventbox.entities.FCMToken;
import com.ht.eventbox.modules.user.FCMTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PushNotificationService {
    private final FCMTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    public BatchResponse push(List<Long> userIds, Notification notification, Map<String, String> messageData) throws Exception {
        List<FCMToken> fcmTokens = fcmTokenRepository.findAllByUserIdIn(userIds);
        if (fcmTokens.isEmpty()) throw new Exception("Token not found");
        MulticastMessage messages = buildMessages(fcmTokens, notification, messageData);
        return firebaseMessaging.sendEachForMulticast(messages);
    }


    public BatchResponse push(List<Long> userIds, Notification notification, Map<String, String> messageData, boolean dryRun) throws Exception {
        List<FCMToken> fcmTokens = fcmTokenRepository.findAllByUserIdIn(userIds);
        if (fcmTokens.isEmpty()) throw new Exception("Token not found");
        MulticastMessage messages = buildMessages(fcmTokens, notification, messageData);
        return firebaseMessaging.sendEachForMulticast(messages, dryRun);
    }


    public BatchResponse push(Long userId, Notification notification, Map<String, String> messageData) throws Exception {
        FCMToken fcmToken = fcmTokenRepository.findByUserId(userId).orElseThrow(() -> new Exception("Token not found"));
        MulticastMessage messages = buildMessages(fcmToken, notification, messageData);
        return firebaseMessaging.sendEachForMulticast(messages);
    }

    public BatchResponse push(Long userId, Notification notification, Map<String, String> messageData, boolean dryRun) throws Exception {
        FCMToken fcmToken = fcmTokenRepository.findByUserId(userId).orElseThrow(() -> new Exception("Token not found"));
        MulticastMessage messages = buildMessages(fcmToken, notification, messageData);
        return firebaseMessaging.sendEachForMulticast(messages, dryRun);
    }


    private MulticastMessage buildMessages(FCMToken fcmToken, Notification notification, Map<String, String> messageData) {
        List<String> messageTokens = extractTokens(fcmToken);

        return MulticastMessage.builder()
                .putAllData(messageData)
                .setNotification(notification)
                .addAllTokens(messageTokens)
                .build();
    }


    private MulticastMessage buildMessages(List<FCMToken> fcmTokens, Notification notification, Map<String, String> messageData) {
        List<String> messageTokens = extractTokens(fcmTokens);
        return MulticastMessage.builder()
                .putAllData(messageData)
                .setNotification(notification)
                .addAllTokens(messageTokens)
                .build();
    }


    private List<String> extractTokens(FCMToken fcmToken) {
        return Stream.of(fcmToken.getIos(), fcmToken.getAndroid(), fcmToken.getWebPush())
                .filter(Objects::nonNull)
                .filter(token -> !token.isEmpty())
                .toList();
    }


    private List<String> extractTokens(List<FCMToken> fcmTokens) {
        return fcmTokens
                .stream()
                .flatMap(fcmToken -> extractTokens(fcmToken).stream())
                .toList();
    }
}
