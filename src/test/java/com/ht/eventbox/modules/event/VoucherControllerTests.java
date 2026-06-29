package com.ht.eventbox.modules.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.enums.DiscountType;
import com.ht.eventbox.modules.event.dtos.ApplyVoucherDto;
import com.ht.eventbox.modules.event.dtos.CreateVoucherDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "paypal.checkout.webhook.id=checkout-webhook",
        "paypal.payment.webhook.id=payment-webhook"
})
class VoucherControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getUsage_shouldReturnCount() throws Exception {
        when(voucherService.getUsage(42L, 9L, 11L)).thenReturn(4L);

        mockMvc.perform(get("/api/v1/vouchers/9/event/11/usage")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(4L));
    }

    @Test
    void getByOrderId_shouldReturnVoucher() throws Exception {
        when(voucherService.getByOrderId(42L, 100L)).thenReturn(sampleVoucher());

        mockMvc.perform(get("/api/v1/vouchers/order/100")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(9L));
    }

    @Test
    void applyByOrderId_shouldReturnSuccessResponse() throws Exception {
        when(voucherService.applyByOrderId(eq(42L), eq(100L), any(ApplyVoucherDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/vouchers/order/100/apply")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplyVoucherDto.builder().code("summer10").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));

        verify(voucherService).applyByOrderId(eq(42L), eq(100L), any(ApplyVoucherDto.class));
    }

    @Test
    void removeByOrderId_shouldReturnSuccessResponse() throws Exception {
        when(voucherService.removeByOrderId(42L, 100L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/vouchers/order/100/remove")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getAllByEventId_shouldReturnVouchers() throws Exception {
        when(voucherService.getAllByEventId(42L, 11L)).thenReturn(List.of(sampleVoucher()));

        mockMvc.perform(get("/api/v1/vouchers/event/11")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L));
    }

    @Test
    void getAllByEventIdV2_shouldReturnPagedVouchersWithSearch() throws Exception {
        when(voucherService.getAllByEventId(eq(42L), eq(11L), eq("summer"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(sampleVoucher()), org.springframework.data.domain.PageRequest.of(1, 10), 21));

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

        verify(voucherService).getAllByEventId(eq(42L), eq(11L), eq("summer"), any());
    }

    @Test
    void getAllPublicByEventId_shouldReturnPublicVouchers() throws Exception {
        when(voucherService.getAllPublicByEventId(11L)).thenReturn(List.of(sampleVoucher()));

        mockMvc.perform(get("/api/v1/vouchers/event/11/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L));
    }

    @Test
    void createByEventId_shouldReturnCreatedResponse() throws Exception {
        when(voucherService.createByEventId(eq(42L), eq(11L), any(CreateVoucherDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/vouchers/event/11")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateVoucherDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Created"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateByEventId_shouldReturnUpdateResponse() throws Exception {
        when(voucherService.updateByEventId(eq(42L), eq(9L), eq(11L), any(CreateVoucherDto.class))).thenReturn(true);

        mockMvc.perform(put("/api/v1/vouchers/9/event/11")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateVoucherDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void deleteByEventId_shouldReturnSuccessResponse() throws Exception {
        when(voucherService.deleteByEventId(42L, 9L, 11L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/vouchers/9/event/11")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
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

    private CreateVoucherDto sampleCreateVoucherDto() {
        return CreateVoucherDto.builder()
                .code("summer10")
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
