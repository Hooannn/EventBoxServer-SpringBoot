package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.enums.DiscountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class VoucherControllerV2Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getAllByEventId_shouldReturnPagedVouchersWithSearch() throws Exception {
        when(voucherService.getAllByEventId(eq(42L), eq(11L), eq("summer"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleVoucher()), PageRequest.of(1, 10), 21));

        mockMvc.perform(get("/api/v2/vouchers/event/11")
                        .requestAttr("sub", "42")
                        .param("search", "summer")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1));

        verify(voucherService).getAllByEventId(eq(42L), eq(11L), eq("summer"), any(Pageable.class));
    }

    private Voucher sampleVoucher() {
        return Voucher.builder()
                .id(9L)
                .code("SUMMER10")
                .name("Summer sale")
                .description("Discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .usageLimit(5)
                .perUserLimit(2)
                .isActive(true)
                .isPublic(true)
                .minOrderValue(50.0)
                .minTicketQuantity(1)
                .build();
    }
}
