package com.ht.eventbox.modules.ticket;

import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.modules.ticket.dtos.FeedbackTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.GiveawayTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
        private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TicketController.class);

        private final TicketService ticketService;

        /*
         * API dùng để lấy tất cả các vé của người dùng hiện tại với trạng thái đã thanh
         * toán (FULFILLED), dùng cho mobile app giao diện người dùng (vé của tôi)
         */
        @GetMapping("/items/me")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:orders')")
        public ResponseEntity<Response<List<TicketService.TicketItemDetails>>> getMyTicketItems(
                        Principal principal) {
                var res = ticketService.getTicketItemsByUserIdAndOrderStatusIn(Long.valueOf(principal.getName()),
                                Arrays.asList(OrderStatus.FULFILLED, OrderStatus.APPROVED));

                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để lấy thông tin chi tiết của một vé theo ID, dùng cho mobile app
         * giao diện người dùng (vé của tôi)
         */
        @GetMapping("/items/{ticketItemId}")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:orders')")
        public ResponseEntity<Response<TicketService.TicketItemDetails>> getTicketItemById(
                        Principal principal,
                        @PathVariable Long ticketItemId) {
                var res = ticketService.getTicketItemById(ticketItemId);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để sinh mã QR cho vé theo ID, dùng cho mobile app giao diện người
         * dùng (vé của tôi)
         */
        @GetMapping("/items/{ticketItemId}/qrcode")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:orders')")
        public ResponseEntity<Response<String>> getTicketItemQrCode(
                        Principal principal,
                        @PathVariable String ticketItemId) {
                var res = ticketService.getTicketItemQrCode(Long.valueOf(principal.getName()), Long.valueOf(ticketItemId));
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để xác thực vé, dùng cho mobile app giao diện ban tổ chức (quét vé)
         */
        @PostMapping("/validate")
        public ResponseEntity<Response<TicketService.TicketItemDetails>> validateTicketItem(
                        Principal principal,
                        @Valid @RequestBody ValidateTicketItemDto validateTicketItemDto) {
                var res = ticketService.validateTicketItem(Long.valueOf(principal.getName()), validateTicketItemDto);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để tạo một trace cho vé (check-in hoặc ra ngoài tuỳ vào trace cuối
         * cùng), dùng cho mobile app giao diện ban tổ chức (quét vé)
         */
        @PostMapping("/traces")
        public ResponseEntity<Response<Boolean>> createTicketItemTrace(
                        Principal principal,
                        @Valid @RequestBody ValidateTicketItemDto createTicketItemTraceDto) {
                var res = ticketService.createTicketItemTrace(Long.valueOf(principal.getName()), createTicketItemTraceDto);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để lấy tất cả các vé của một chương trình theo ID, dùng cho web ban
         * tổ chức khi xem báo cáo
         */
        @GetMapping("/shows/{showId}/items")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:orders')")
        public ResponseEntity<Response<List<TicketItem>>> getTicketItemByShowId(
                        Principal principal,
                        @PathVariable Long showId) {
                var res = ticketService.getTicketItemByShowId(Long.valueOf(principal.getName()), showId);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để gửi phản hồi cho vé (khi sự kiện đã kết thúc), dùng cho mobile
         * app giao diện người dùng (vé của tôi)
         */
        @PostMapping("/items/{ticketItemId}/feedback")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:orders')")
        public ResponseEntity<Response<Boolean>> createTicketItemFeedback(
                        Principal principal,
                        @Valid @RequestBody FeedbackTicketItemDto feedbackTicketItemDto,
                        @PathVariable String ticketItemId) {
                var res = ticketService.createTicketItemFeedback(Long.valueOf(principal.getName()), Long.valueOf(ticketItemId),
                                feedbackTicketItemDto);
                return ResponseEntity.created(null).body(
                                new Response<>(
                                                HttpStatus.CREATED.value(),
                                                Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                                                res));
        }

        /*
         * API dùng để tặng vé cho người khác, dùng cho mobile app giao diện người dùng
         * (vé của tôi)
         */
        @PostMapping("/items/{ticketItemId}/giveaway")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:orders')")
        public ResponseEntity<Response<Boolean>> giveawayTicketItem(
                        Principal principal,
                        @Valid @RequestBody GiveawayTicketItemDto giveawayTicketItemDto,
                        @PathVariable String ticketItemId) {
                var res = ticketService.giveawayTicketItem(Long.valueOf(principal.getName()), Long.valueOf(ticketItemId),
                                giveawayTicketItemDto);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                                                res));
        }

        /*
         * API dùng để lấy 20 vé gần nhất được phản hồi theo id ban tổ chức, dùng trong
         * mobile app giao diện người dùng xem thông tin ban tổ chức
         */
        @GetMapping("/items/feedback/organizations/{organizationId}")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:organizations')")
        public ResponseEntity<Response<List<TicketService.TicketItemDetails>>> getLatestTicketItemFeedbackByOrganization(
                        Principal principal,
                        @PathVariable String organizationId) {
                var res = ticketService.getLatestTicketItemFeedbackByOrganizationId(Long.valueOf(organizationId));
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để lấy tất cả vé được phản hồi theo id sự kiện, dùng cho web ban tổ
         * chức khi xem báo cáo
         */
        @GetMapping("/items/feedback/event/{eventId}")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:orders')")
        public ResponseEntity<Response<List<TicketService.TicketItemDetails>>> getTicketItemFeedbackByEvent(
                        Principal principal,
                        @PathVariable String eventId) {
                var res = ticketService.getTicketItemFeedbackByEventId(Long.valueOf(principal.getName()), Long.valueOf(eventId));
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        @PostMapping("/items/{ticketItemId}/reminder/trigger")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'access:admin')")
        public ResponseEntity<Response<Boolean>> triggerReminder(
                        @PathVariable String ticketItemId) {
                var res = ticketService.triggerReminder(Long.valueOf(ticketItemId));
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }
}
