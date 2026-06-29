package com.ht.eventbox.modules.order;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.QueryResponse;
import com.ht.eventbox.entities.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v2/orders")
@RequiredArgsConstructor
public class OrderControllerV2 {
    private final OrderService orderService;

    @GetMapping("/shows/{showId}/all")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<QueryResponse<Order>> getByShowId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long showId,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        var res = orderService.getByShowId(Long.valueOf(sub), showId, search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }

    @GetMapping(value = "/shows/{showId}/all/export", produces = "text/csv")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<byte[]> exportByShowId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long showId,
            @RequestParam(required = false) String search
    ) {
        var csv = orderService.exportByShowId(Long.valueOf(sub), showId, search);
        var fileName = "orders-show-" + showId + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
