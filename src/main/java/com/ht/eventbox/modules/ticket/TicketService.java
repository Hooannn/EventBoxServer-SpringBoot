package com.ht.eventbox.modules.ticket;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.enums.TicketItemTraceEvent;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.order.TicketItemRepository;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final EventRepository eventRepository;
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

    public interface OrderView {
        Long getId();

        OrderStatus getStatus();

        List<Payment> getPayments();

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

        OrderView getOrder();

        List<TicketItemTrace> getTraces();

        @JsonProperty("created_at")
        java.time.LocalDateTime getCreatedAt();

        @JsonProperty("updated_at")
        java.time.LocalDateTime getUpdatedAt();
    }

    private final TicketItemRepository ticketItemRepository;
    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;
    private final TicketItemTraceRepository ticketItemTraceRepository;
    private final SocketIOServer socketIOServer;

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
}
