package com.ht.eventbox.modules.order;

import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.enums.OrderStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class OrderControllerV2Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getByShowIdAll_shouldReturnPagedOrdersWithSearch() throws Exception {
        when(orderService.getByShowId(eq(42L), eq(77L), eq("alice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleOrder(61L)), PageRequest.of(1, 10), 25));

        mockMvc.perform(get("/api/v2/orders/shows/77/all")
                        .requestAttr("sub", "42")
                        .param("search", "alice")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(61L))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1));

        verify(orderService).getByShowId(eq(42L), eq(77L), eq("alice"), any(Pageable.class));
    }

    @Test
    void exportByShowIdAll_shouldReturnCsvAttachment() throws Exception {
        when(orderService.exportByShowId(eq(42L), eq(77L), eq("alice")))
                .thenReturn("""
                        order_id,user_name,user_email,status,place_total
                        61,Test User,test@example.com,FULFILLED,75000.0
                        """);

        mockMvc.perform(get("/api/v2/orders/shows/77/all/export")
                        .requestAttr("sub", "42")
                        .param("search", "alice"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("order_id,user_name,user_email,status,place_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("61,Test User,test@example.com,FULFILLED,75000.0")));

        verify(orderService).exportByShowId(eq(42L), eq(77L), eq("alice"));
    }

    private Order sampleOrder(Long id) {
        return Order.builder()
                .id(id)
                .status(OrderStatus.FULFILLED)
                .placeTotal(75000.0)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }
}
