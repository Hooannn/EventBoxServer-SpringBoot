package com.ht.eventbox.modules.event;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.QueryResponse;
import com.ht.eventbox.entities.Voucher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v2/vouchers")
@RequiredArgsConstructor
public class VoucherControllerV2 {
    private final VoucherService voucherService;

    @GetMapping("/event/{eventId}")
    @RequiredPermissions({"read:vouchers"})
    public ResponseEntity<QueryResponse<Voucher>> getAllByEventId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long eventId,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        var res = voucherService.getAllByEventId(Long.valueOf(sub), eventId, search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }
}
