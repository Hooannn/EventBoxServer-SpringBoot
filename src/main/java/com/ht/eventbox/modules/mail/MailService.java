package com.ht.eventbox.modules.mail;

import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.utils.Helper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }

    public void sendResetPasswordVerificationMail(String to, String subject, String signature) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();
        context.setVariable("code", signature);

        String htmlContent = templateEngine.process("reset-password-verification", context);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    public void sendRegistrationEmail(String to, String name, String otp) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();

        context.setVariable("name", name);
        context.setVariable("otp", otp);

        String htmlContent = templateEngine.process(Constant.MailTemplate.VERIFY_EMAIL, context);

        helper.setTo(to);
        helper.setSubject(Constant.MailSubject.VERIFY_EMAIL);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    public void sendForgotPasswordEmail(String to, String otp) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();

        context.setVariable("otp", otp);

        String htmlContent = templateEngine.process(Constant.MailTemplate.FORGOT_PASSWORD, context);

        helper.setTo(to);
        helper.setSubject(Constant.MailSubject.FORGOT_PASSWORD);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    public void sendMemberAddedEmail(String to, String name, String orgName) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();

        context.setVariable("name", name);
        context.setVariable("orgName", orgName);

        String htmlContent = templateEngine.process(Constant.MailTemplate.MEMBER_ADDED, context);

        helper.setTo(to);
        helper.setSubject(Constant.MailSubject.MEMBER_ADDED);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    public void sendMemberRemovedEmail(String to, String name, String orgName) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();

        context.setVariable("name", name);
        context.setVariable("orgName", orgName);

        String htmlContent = templateEngine.process(Constant.MailTemplate.MEMBER_REMOVED, context);

        helper.setTo(to);
        helper.setSubject(Constant.MailSubject.MEMBER_REMOVED);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    public void sendOrderPaidMail(String to, String name, String invoiceId, String total, String paidDate) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("invoiceId", invoiceId);
        context.setVariable("total", total);
        context.setVariable("paidDate", paidDate);

        String htmlContent = templateEngine.process(Constant.MailTemplate.ORDER_PAID, context);

        helper.setTo(to);
        helper.setSubject(Constant.MailSubject.ORDER_PAID);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    public void sendReminderEmail(String to, Event event, EventShow eventShow) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();

        var eventImageUrl = event.getAssets().stream()
                .filter(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO)
                .findFirst()
                .map(Asset::getSecureUrl)
                .orElse(null);

        context.setVariable("eventName", event.getTitle());
        context.setVariable("eventImageUrl", eventImageUrl);
        context.setVariable("eventShowName", eventShow.getTitle());
        context.setVariable("eventShowStartTime", Helper.formatDateToString(eventShow.getStartTime()));

        String htmlContent = templateEngine.process(Constant.MailTemplate.UPCOMING_EVENT, context);

        helper.setTo(to);
        helper.setSubject(Constant.MailSubject.UPCOMING_EVENT + " - " + event.getTitle());
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    public void sendGiveawayNotificationEmail(String to, Event event, EventShow eventShow, String from) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context = new Context();

        var eventImageUrl = event.getAssets().stream()
                .filter(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO)
                .findFirst()
                .map(Asset::getSecureUrl)
                .orElse(null);

        context.setVariable("senderName", from);
        context.setVariable("eventName", event.getTitle());
        context.setVariable("eventImageUrl", eventImageUrl);
        context.setVariable("eventShowName", eventShow.getTitle());
        context.setVariable("eventShowStartTime", Helper.formatDateToString(eventShow.getStartTime()));

        String htmlContent = templateEngine.process(Constant.MailTemplate.GIVEAWAY_TICKET, context);

        helper.setTo(to);
        helper.setSubject(Constant.MailSubject.GIVEAWAY_TICKET + " - " + event.getTitle());
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }
}
