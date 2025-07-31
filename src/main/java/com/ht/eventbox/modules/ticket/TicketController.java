package com.ht.eventbox.modules.ticket;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.enums.OrderStatus;
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
}
