package com.ht.eventbox.modules.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.event.EventShowRepository;
import com.ht.eventbox.modules.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailJobHandler implements RedisBackgroundJobHandler {
    private static final TypeReference<Map<String, String>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final MailService mailService;
    private final EventRepository eventRepository;
    private final EventShowRepository eventShowRepository;
    private final ObjectMapper objectMapper;

    @Override
    public BackgroundJobType supports() {
        return BackgroundJobType.SEND_MAIL;
    }

    @Override
    public void handle(BackgroundJobEnvelope envelope) throws Exception {
        Map<String, String> payload = objectMapper.readValue(envelope.payload(), PAYLOAD_TYPE);
        MailKind mailKind = MailKind.valueOf(required(payload, "mailKind"));

        switch (mailKind) {
            case REGISTRATION, VERIFY_RESEND ->
                    mailService.sendRegistrationEmail(required(payload, "recipient"), required(payload, "name"), required(payload, "otp"));
            case FORGOT_PASSWORD ->
                    mailService.sendForgotPasswordEmail(required(payload, "recipient"), required(payload, "otp"));
            case MEMBER_ADDED ->
                    mailService.sendMemberAddedEmail(required(payload, "recipient"), required(payload, "name"), required(payload, "orgName"));
            case MEMBER_REMOVED ->
                    mailService.sendMemberRemovedEmail(required(payload, "recipient"), required(payload, "name"), required(payload, "orgName"));
            case ORDER_REFUNDED ->
                    mailService.sendOrderRefundedMail(
                            required(payload, "recipient"),
                            required(payload, "name"),
                            required(payload, "orderId"),
                            required(payload, "amount"),
                            required(payload, "timestamp"));
            case ORDER_PAID ->
                    mailService.sendOrderPaidMail(
                            required(payload, "recipient"),
                            required(payload, "name"),
                            required(payload, "orderId"),
                            required(payload, "amount"),
                            required(payload, "timestamp"));
            case REMINDER -> {
                Event event = eventRepository.findById(Long.valueOf(required(payload, "eventId")))
                        .orElseThrow(() -> new IllegalStateException("Event not found"));
                EventShow eventShow = eventShowRepository.findById(Long.valueOf(required(payload, "eventShowId")))
                        .orElseThrow(() -> new IllegalStateException("Event show not found"));
                mailService.sendReminderEmail(required(payload, "recipient"), event, eventShow);
            }
            case GIVEAWAY_NOTIFICATION -> {
                Event event = eventRepository.findById(Long.valueOf(required(payload, "eventId")))
                        .orElseThrow(() -> new IllegalStateException("Event not found"));
                EventShow eventShow = eventShowRepository.findById(Long.valueOf(required(payload, "eventShowId")))
                        .orElseThrow(() -> new IllegalStateException("Event show not found"));
                mailService.sendGiveawayNotificationEmail(
                        required(payload, "recipient"),
                        event,
                        eventShow,
                        required(payload, "from"));
            }
        }
    }

    private String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing payload field: " + key);
        }
        return value;
    }
}
