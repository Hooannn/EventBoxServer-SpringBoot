package com.ht.eventbox.modules.ticket;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.messaging.Notification;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.enums.TicketItemTraceEvent;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.auth.AuthService;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.order.OrderRepository;
import com.ht.eventbox.modules.order.TicketItemRepository;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.ticket.dtos.FeedbackTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.GiveawayTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
import com.ht.eventbox.modules.user.UserService;
import com.ht.eventbox.utils.Helper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TicketService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TicketService.class);

    @Value("${application.security.jwt.qrcode-secret-key}")
    private String qrcodeSecretKey;

    public interface OrganizationView {
        Long getId();
    }

    public interface EventView {
        Long getId();
        String getTitle();
        String getDescription();
        String getAddress();

        @JsonProperty("place_name")
        String getPlaceName();

        OrganizationView getOrganization();

        Set<Asset> getAssets();
    }

    public interface EventShowView {
        Long getId();

        String getTitle();

        EventView getEvent();

        @JsonProperty("start_time")
        java.time.LocalDateTime getStartTime();

        @JsonProperty("end_time")
        java.time.LocalDateTime getEndTime();
    }

    public interface TicketView {
        Long getId();

        String getName();

        String getDescription();

        @JsonProperty("event_show")
        EventShowView getEventShow();
    }

    public interface UserView {
        Long getId();

        @JsonProperty("first_name")
        String getFirstName();

        @JsonProperty("last_name")
        String getLastName();

        @JsonProperty("assets")
        Set<Asset> getAssets();
    }

    public interface OrderView {
        Long getId();

        OrderStatus getStatus();

        List<Payment> getPayments();

        UserView getUser();

        @JsonProperty("place_total")
        Double getPlaceTotal();

        @JsonProperty("created_at")
        java.time.LocalDateTime getCreatedAt();

        @JsonProperty("updated_at")
        java.time.LocalDateTime getUpdatedAt();
    }

    public interface TicketItemDetails {
        Long getId();

        @JsonProperty("place_total")
        Double getPlaceTotal();

        TicketView getTicket();

        String getFeedback();

        OrderView getOrder();

        List<TicketItemTrace> getTraces();

        @JsonProperty("feedback_at")
        java.time.LocalDateTime getFeedbackAt();

        @JsonProperty("created_at")
        java.time.LocalDateTime getCreatedAt();

        @JsonProperty("updated_at")
        java.time.LocalDateTime getUpdatedAt();
    }

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final AuthService authService;
    private final TicketItemRepository ticketItemRepository;
    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;
    private final TicketItemTraceRepository ticketItemTraceRepository;
    private final SocketIOServer socketIOServer;
    private final MailService mailService;
    private final PushNotificationService pushNotificationService;

    public List<TicketItemDetails> getTicketItemsByUserIdAndOrderStatusIs(Long userId, OrderStatus status) {
        return ticketItemRepository.findAllByOrderUserIdAndOrderStatusIsOrderByIdAsc(
                userId,
                status,
                TicketItemDetails.class
        );
    }

    public TicketItemDetails getTicketItemById(Long ticketItemId) {
        return ticketItemRepository.findById(
                ticketItemId,
                TicketItemDetails.class
        ).orElseThrow(() -> new HttpException(
                Constant.ErrorCode.TICKET_ITEM_NOT_FOUND,
                HttpStatus.BAD_REQUEST
        ));
    }

    public String getTicketItemQrCode(Long userId, Long ticketItemId) {
        var ticketItem = ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(ticketItemId, userId, OrderStatus.FULFILLED, TicketItemDetails.class)
                .orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.TICKET_ITEM_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        if (ticketItem.getTicket().getEventShow().getStartTime().isAfter(LocalDateTime.now())) {
            throw new HttpException(
                    Constant.ErrorCode.SHOW_NOT_STARTED,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (ticketItem.getTicket().getEventShow().getEndTime().isBefore(LocalDateTime.now())) {
            throw new HttpException(
                    Constant.ErrorCode.SHOW_ENDED,
                    HttpStatus.BAD_REQUEST
            );
        }

        // sử dụng jwt sinh token với sub là ticketItemId, thời hạn sử dụng 5 phút
        return jwtService.generateQrCode(ticketItem.getId());
    }


    public TicketItemDetails validateTicketItem(Long userId, ValidateTicketItemDto validateTicketItemDto) {
        String sub;

        try {
            // kiểm tra token có hợp lệ không (đúng secret và còn hạn sử dụng)
            boolean isTokenValid = jwtService.isTokenValid(validateTicketItemDto.getToken(), qrcodeSecretKey);
            if (!isTokenValid) {
                throw new HttpException(
                        Constant.ErrorCode.TICKET_ITEM_INVALID,
                        HttpStatus.BAD_REQUEST
                );
            }

            // extract lấy ticketItemId
            sub = jwtService.extractSub(validateTicketItemDto.getToken(), qrcodeSecretKey);
        } catch (Exception e) {
            throw new HttpException(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (sub == null || sub.isEmpty()) {
            throw new HttpException(
                    Constant.ErrorCode.TICKET_ITEM_INVALID,
                    HttpStatus.BAD_REQUEST
            );
        }

        long ticketItemId = Long.parseLong(sub);

        // Kiểm tra ticketItemId đúng với userId và orderStatus là FULFILLED và đúng với eventShowId
        var ticketItem = ticketItemRepository.findByIdAndOrderStatusIsAndTicketEventShowId(ticketItemId, OrderStatus.FULFILLED, validateTicketItemDto.getEventShowId(), TicketItemDetails.class)
                .orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.TICKET_ITEM_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        // Kiểm tra xem chương trình đã bắt đầu chưa và đã kết thúc chưa
        if (ticketItem.getTicket().getEventShow().getStartTime().isAfter(LocalDateTime.now())) {
            throw new HttpException(
                    Constant.ErrorCode.SHOW_NOT_STARTED,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (ticketItem.getTicket().getEventShow().getEndTime().isBefore(LocalDateTime.now())) {
            throw new HttpException(
                    Constant.ErrorCode.SHOW_ENDED,
                    HttpStatus.BAD_REQUEST
            );
        }

        // Chỉ thành viên của tổ chức mới có thể xác thực vé
        long orgId = ticketItem.getTicket().getEventShow().getEvent().getOrganization().getId();

        boolean isMember = organizationRepository.existsByIdAndUserOrganizationsUserId(
                orgId,
                userId
        );

        if (!isMember) {
            throw new HttpException(
                    Constant.ErrorCode.USER_NOT_IN_ORGANIZATION,
                    HttpStatus.BAD_REQUEST
            );
        }

        return ticketItem;
    }

    public boolean createTicketItemTrace(Long userId, ValidateTicketItemDto createTicketItemTraceDto) {
        // xác thực lại mã qr
        var ticketItem = validateTicketItem(
                userId,
                createTicketItemTraceDto
        );

        var trace = new TicketItemTrace();
        trace.setTicketItem(TicketItem.builder().id(ticketItem.getId()).build());
        trace.setIssuer(User.builder().id(userId).build());

        // nếu vé chưa có trace nào thì mặc định là CHECKED_IN
        if (ticketItem.getTraces().isEmpty()) {
            trace.setEvent(TicketItemTraceEvent.CHECKED_IN);
        } else {
            // nếu vé đã có trace thì kiểm tra trace cuối cùng
            var lastTrace = ticketItem.getTraces().get(ticketItem.getTraces().size() - 1);
            if (lastTrace.getEvent() == TicketItemTraceEvent.CHECKED_IN) {
                trace.setEvent(TicketItemTraceEvent.WENT_OUT);
            } else {
                trace.setEvent(TicketItemTraceEvent.CHECKED_IN);
            }
        }

        ticketItemTraceRepository.save(trace);

        // gửi sự kiện đến client qua socketio để cập nhật trạng thái vé
        CompletableFuture.runAsync(() -> {
            socketIOServer.getNamespace("/ticket")
                    .getRoomOperations(ticketItem.getId().toString())
                    .sendEvent("traces_updated", Map.of(
                            "ticket_item_id", ticketItem.getId()
                    ));
        });

        // gửi sự kiện đến client qua socketio để cập nhật dashboard
        CompletableFuture.runAsync(() -> {
            socketIOServer.getNamespace("/event")
                    .getRoomOperations(ticketItem.getTicket().getEventShow().getEvent().getId().toString())
                    .sendEvent("traces_updated", Map.of(
                            "ticket_item_id", ticketItem.getId()
                    ));
        });

        return true;
    }

    public List<TicketItem> getTicketItemByShowId(Long userId, Long showId) {
        Event event = eventRepository.findByShowsId(showId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        var members = event.getOrganization().getUserOrganizations();

        if (members.stream().noneMatch(m -> m.getUser().getId().equals(userId) && List.of(OrganizationRole.MANAGER, OrganizationRole.OWNER).contains(m.getRole()))) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        return ticketItemRepository.findAllByTicketEventShowId(showId, TicketItem.class);
    }

    public boolean createTicketItemFeedback(Long userId, Long ticketItemId, FeedbackTicketItemDto feedbackTicketItemDto) {
        // kiểm tra ticketItemId đúng với userId và orderStatus là FULFILLED
        var ticketItem = ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(ticketItemId, userId, OrderStatus.FULFILLED, TicketItem.class)
                .orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.TICKET_ITEM_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        if (ticketItem.getTraces().isEmpty()) {
            throw new HttpException(
                    Constant.ErrorCode.TICKET_ITEM_NOT_USED,
                    HttpStatus.BAD_REQUEST
            );
        }

        var eventShow = ticketItem.getTicket().getEventShow();

        // Kiểm tra xem chương trình đã kết thúc chưa
        var isEnded = eventShow.getEndTime().isBefore(LocalDateTime.now());

        if (!isEnded) {
            throw new HttpException(
                    Constant.ErrorCode.SHOW_NOT_ENDED,
                    HttpStatus.BAD_REQUEST
            );
        }

        // Kiểm tra xem người dùng đã gửi phản hồi cho vé này chưa
        if (ticketItem.getFeedback() != null) {
            throw new HttpException(
                    Constant.ErrorCode.NOT_ALLOWED_OPERATION,
                    HttpStatus.BAD_REQUEST
            );
        }

        ticketItem.setFeedback(feedbackTicketItemDto.getFeedback());
        ticketItem.setFeedbackAt(LocalDateTime.now());
        ticketItemRepository.save(ticketItem);

        return true;
    }

    public void remindUpcomingEvents() {
        var startOfDay = LocalDateTime.of(
                LocalDate.now(),
                LocalTime.MIN
        );
        var endOfDay = LocalDateTime.of(
                LocalDate.now(),
                LocalTime.MAX
        );
        var ticketItems = ticketItemRepository.findAllByOrderStatusIsAndRemindedIsFalseAndTicketEventShowStartTimeBetween(
                OrderStatus.FULFILLED,
                startOfDay,
                endOfDay
        );

        ticketItems.forEach(
                ticketItem -> {
                    var order = ticketItem.getOrder();
                    var user = order.getUser();
                    var eventShow = ticketItem.getTicket().getEventShow();
                    var event = eventShow.getEvent();

                    try {
                        mailService.sendReminderEmail(
                                user.getEmail(),
                                event,
                                eventShow
                        );
                    } catch (MessagingException e) {
                        logger.error("Error when send reminder email for userId {}: {}", user.getId(), e.getMessage());
                    }

                    var notification = Notification.builder()
                                    .setTitle("Nhắc nhở: Sự kiện sắp diễn ra - " + event.getTitle())
                                    .setBody("Chương trình \"" + eventShow.getTitle() + "\" sẽ diễn ra vào " + Helper.formatDateToString(eventShow.getStartTime()) + ". Đừng quên tham gia nhé!")
                                    .setImage(event.getAssets().stream()
                                            .filter(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO)
                                            .findFirst()
                                            .map(Asset::getSecureUrl)
                                            .orElse(null))
                                    .build();

                    try {
                        pushNotificationService.push(
                                ticketItem.getOrder().getUser().getId(),
                                notification,
                                Map.of(
                                        "type", "upcoming_event",
                                        "event_id", event.getId().toString(),
                                        "event_show_id", eventShow.getId().toString()
                                )
                        );
                    } catch (Exception e) {
                        logger.error("Error when push notification for userId {}: {}", user.getId(), e.getMessage());
                    }

                    ticketItem.setReminded(true);
                }
        );

        ticketItemRepository.saveAll(ticketItems);
    }

    public boolean giveawayTicketItem(Long userId, Long ticketItemId, GiveawayTicketItemDto giveawayTicketItemDto) {
        // kiểm tra ticketItemId đúng với userId và orderStatus là FULFILLED
        var ticketItem = ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(ticketItemId, userId, OrderStatus.FULFILLED, TicketItem.class)
                .orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.TICKET_ITEM_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        // kiểm tra xem người dùng có đang tặng vé cho chính mình không
        if (ticketItem.getOrder().getUser().getEmail().equals(giveawayTicketItemDto.getRecipientEmail())) {
            throw new HttpException(
                    Constant.ErrorCode.NOT_ALLOWED_OPERATION,
                    HttpStatus.BAD_REQUEST
            );
        }

        // kiểm tra xem vé đã được sử dụng chưa (có trace chưa)
        if (!ticketItem.getTraces().isEmpty()) {
            throw new HttpException(
                    Constant.ErrorCode.TICKET_ITEM_ALREADY_USED,
                    HttpStatus.BAD_REQUEST
            );
        }

        // kiểm tra xem chương trình đã kết thúc chưa
        var isEnded = ticketItem.getTicket().getEventShow().getEndTime().isBefore(LocalDateTime.now());
        if (isEnded) {
            throw new HttpException(
                    Constant.ErrorCode.SHOW_ENDED,
                    HttpStatus.BAD_REQUEST
            );
        }

        // kiểm tra xem recipientEmail có tồn tại trong hệ thống không
        var recipient = userService.getByEmail(giveawayTicketItemDto.getRecipientEmail());

        var isPasswordMatch = authService.isPasswordMatch(
                userId, giveawayTicketItemDto.getPassword()
        );

        if (!isPasswordMatch) {
            throw new HttpException(
                    Constant.ErrorCode.NOT_ALLOWED_OPERATION,
                    HttpStatus.BAD_REQUEST
            );
        }

        String fromEmail = ticketItem.getOrder().getUser().getEmail();
        var order = ticketItem.getOrder();

        if (order.getItems().size() == 1) {
            // cập nhật lại userId của order
            order.setUser(recipient);
        } else {
            // tạo order mới cho người nhận
            var newOrder = new Order();
            newOrder.setUser(recipient);
            newOrder.setPlaceTotal(ticketItem.getPlaceTotal());
            newOrder.setStatus(OrderStatus.FULFILLED);
            newOrder.setFulfilledAt(LocalDateTime.now());
            newOrder.setExpiredAt(LocalDateTime.now());
            var savedOrder = orderRepository.save(newOrder);

            // cập nhật lại ticketItem sang order mới
            ticketItem.setOrder(savedOrder);
        }
        ticketItemRepository.save(ticketItem);

        // gửi email thông báo tặng vé thành công
        CompletableFuture.runAsync(() -> {
            try {
                mailService.sendGiveawayNotificationEmail(
                        recipient.getEmail(),
                        ticketItem.getTicket().getEventShow().getEvent(),
                        ticketItem.getTicket().getEventShow(),
                        fromEmail
                );
            } catch (MessagingException e) {
                logger.error("Error when send giveaway notification email to userId {}: {}", recipient.getId(), e.getMessage());
            }
        });

        return true;
    }

    public List<TicketItemDetails> getLatestTicketItemFeedbackByOrganizationId(Long organizationId) {
        // kiểm tra tổ chức có tồn tại không
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.ORGANIZATION_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        return ticketItemRepository.findTop20ByTicketEventShowEventOrganizationIdAndFeedbackIsNotNullOrderByFeedbackAtDesc(
                organization.getId(),
                TicketItemDetails.class
        );
    }

}
