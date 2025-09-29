package com.ht.eventbox.modules.ticket;

import com.ht.eventbox.annotations.RequiredPermissions;
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

import java.util.List;


@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;

    /*
    API dùng để lấy tất cả các vé của người dùng hiện tại với trạng thái đã thanh toán (FULFILLED), dùng cho mobile app giao diện người dùng (vé của tôi)
    */
    @GetMapping("/items/me")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<List<TicketService.TicketItemDetails>>> getMyTicketItems(
            @RequestAttribute("sub") String sub
    ) {
        var res = ticketService.getTicketItemsByUserIdAndOrderStatusIs(Long.valueOf(sub), OrderStatus.FULFILLED);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy thông tin chi tiết của một vé theo ID, dùng cho mobile app giao diện người dùng (vé của tôi)
    */
    @GetMapping("/items/{ticketItemId}")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<TicketService.TicketItemDetails>> getTicketItemById(
            @RequestAttribute("sub") String sub,
            @PathVariable Long ticketItemId) {
        var res = ticketService.getTicketItemById(ticketItemId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để sinh mã QR cho vé theo ID, dùng cho mobile app giao diện người dùng (vé của tôi)
    */
    @GetMapping("/items/{ticketItemId}/qrcode")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<String>> getTicketItemQrCode(
            @RequestAttribute("sub") String sub,
            @PathVariable String ticketItemId)
    {
        var res = ticketService.getTicketItemQrCode(Long.valueOf(sub), Long.valueOf(ticketItemId));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để xác thực vé, dùng cho mobile app giao diện ban tổ chức (quét vé)
    */
    @PostMapping("/validate")
    public ResponseEntity<Response<TicketService.TicketItemDetails>> validateTicketItem(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody ValidateTicketItemDto validateTicketItemDto)
    {
        var res = ticketService.validateTicketItem(Long.valueOf(sub), validateTicketItemDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để tạo một trace cho vé (check-in hoặc ra ngoài tuỳ vào trace cuối cùng), dùng cho mobile app giao diện ban tổ chức (quét vé)
    */
    @PostMapping("/traces")
    public ResponseEntity<Response<Boolean>> createTicketItemTrace(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody ValidateTicketItemDto createTicketItemTraceDto)
    {
        var res = ticketService.createTicketItemTrace(Long.valueOf(sub), createTicketItemTraceDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả các vé của một chương trình theo ID, dùng cho web ban tổ chức khi xem báo cáo
    */
    @GetMapping("/shows/{showId}/items")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<List<TicketItem>>> getTicketItemByShowId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long showId)
    {
        var res = ticketService.getTicketItemByShowId(Long.valueOf(sub), showId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để gửi phản hồi cho vé (khi sự kiện đã kết thúc), dùng cho mobile app giao diện người dùng (vé của tôi)
    */
    @PostMapping("/items/{ticketItemId}/feedback")
    @RequiredPermissions({"create:orders"})
    public ResponseEntity<Response<Boolean>> createTicketItemFeedback(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody FeedbackTicketItemDto feedbackTicketItemDto,
            @PathVariable String ticketItemId)
    {
        var res = ticketService.createTicketItemFeedback(Long.valueOf(sub), Long.valueOf(ticketItemId), feedbackTicketItemDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để tặng vé cho người khác, dùng cho mobile app giao diện người dùng (vé của tôi)
    */
    @PostMapping("/items/{ticketItemId}/giveaway")
    @RequiredPermissions({"create:orders"})
    public ResponseEntity<Response<Boolean>> giveawayTicketItem(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody GiveawayTicketItemDto giveawayTicketItemDto,
            @PathVariable String ticketItemId)
    {
        var res = ticketService.giveawayTicketItem(Long.valueOf(sub), Long.valueOf(ticketItemId), giveawayTicketItemDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để lấy 20 vé gần nhất được phản hồi theo id ban tổ chức, dùng trong mobile app giao diện người dùng xem thông tin ban tổ chức
    */
    @GetMapping("/items/feedback/organizations/{organizationId}")
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<Response<List<TicketService.TicketItemDetails>>> getLatestTicketItemFeedbackByOrganization(
            @RequestAttribute("sub") String sub,
            @PathVariable String organizationId)
    {
        var res = ticketService.getLatestTicketItemFeedbackByOrganizationId(Long.valueOf(organizationId));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }


    @PostMapping("/items/{ticketItemId}/reminder/trigger")
    @RequiredPermissions({"access:admin"})
    public ResponseEntity<Response<Boolean>> triggerReminder(
            @PathVariable String ticketItemId)
    {
        var res = ticketService.triggerReminder(Long.valueOf(ticketItemId));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
