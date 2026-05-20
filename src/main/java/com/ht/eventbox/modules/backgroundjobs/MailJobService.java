package com.ht.eventbox.modules.backgroundjobs;

import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.order.OrderRepository;
import com.ht.eventbox.modules.order.TicketItemRepository;
import com.ht.eventbox.utils.Helper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class MailJobService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MailJobService.class);

    private final JobScheduler jobScheduler;
    private final MailService mailService;
    private final OrderRepository orderRepository;
    private final TicketItemRepository ticketItemRepository;

    public void enqueueRegistrationEmail(String to, String name, String otp) {
        enqueueAfterCommit(() -> sendRegistrationEmail(to, name, otp));
    }

    public void enqueueResendVerifyEmail(String to, String name, String otp) {
        enqueueAfterCommit(() -> sendRegistrationEmail(to, name, otp));
    }

    public void enqueueForgotPasswordEmail(String to, String otp) {
        enqueueAfterCommit(() -> sendForgotPasswordEmail(to, otp));
    }

    public void enqueueMemberAddedEmail(String to, String name, String orgName) {
        enqueueAfterCommit(() -> sendMemberAddedEmail(to, name, orgName));
    }

    public void enqueueMemberRemovedEmail(String to, String name, String orgName) {
        enqueueAfterCommit(() -> sendMemberRemovedEmail(to, name, orgName));
    }

    public void enqueueOrderPaidEmail(long orderId) {
        enqueueAfterCommit(() -> sendOrderPaidEmail(orderId));
    }

    public void enqueueOrderRefundedEmail(long orderId) {
        enqueueAfterCommit(() -> sendOrderRefundedEmail(orderId));
    }

    public void enqueueGiveawayNotificationEmail(long ticketItemId, String fromEmail) {
        enqueueAfterCommit(() -> sendGiveawayNotificationEmail(ticketItemId, fromEmail));
    }

    void sendRegistrationEmail(String to, String name, String otp) {
        try {
            mailService.sendRegistrationEmail(to, name, otp);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send registration email to " + to, e);
        }
    }

    void sendForgotPasswordEmail(String to, String otp) {
        try {
            mailService.sendForgotPasswordEmail(to, otp);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send forgot password email to " + to, e);
        }
    }

    void sendMemberAddedEmail(String to, String name, String orgName) {
        try {
            mailService.sendMemberAddedEmail(to, name, orgName);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send member added email to " + to, e);
        }
    }

    void sendMemberRemovedEmail(String to, String name, String orgName) {
        try {
            mailService.sendMemberRemovedEmail(to, name, orgName);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send member removed email to " + to, e);
        }
    }

    void sendOrderPaidEmail(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        try {
            mailService.sendOrderPaidMail(
                    order.getUser().getEmail(),
                    order.getUser().getFullName(),
                    order.getId().toString(),
                    Helper.formatCurrencyToString(order.getPlaceTotal()),
                    Helper.formatDateToString(order.getFulfilledAt()));
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send paid email for order " + orderId, e);
        }
    }

    void sendOrderRefundedEmail(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        try {
            mailService.sendOrderRefundedMail(
                    order.getUser().getEmail(),
                    order.getUser().getFullName(),
                    order.getId().toString(),
                    Helper.formatCurrencyToString(order.getPlaceTotal()),
                    Helper.formatDateToString(order.getUpdatedAt()));
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send refunded email for order " + orderId, e);
        }
    }

    void sendGiveawayNotificationEmail(long ticketItemId, String fromEmail) {
        TicketItem ticketItem = ticketItemRepository.findById(ticketItemId)
                .orElseThrow(() -> new IllegalStateException("Ticket item not found: " + ticketItemId));

        try {
            mailService.sendGiveawayNotificationEmail(
                    ticketItem.getOrder().getUser().getEmail(),
                    ticketItem.getTicket().getEventShow().getEvent(),
                    ticketItem.getTicket().getEventShow(),
                    fromEmail);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send giveaway email for ticket item " + ticketItemId, e);
        }
    }

    private void enqueueAfterCommit(JobLambda job) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    jobScheduler.enqueue(job);
                }
            });
            return;
        }

        jobScheduler.enqueue(job);
    }
}
