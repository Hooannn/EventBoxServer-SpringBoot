package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.enums.EventStatus;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class EventControllerV2Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getAll_shouldReturnPagedEvents() throws Exception {
        when(eventService.getAllByStatusIn(eq(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(11L)), PageRequest.of(0, 20), 33));

        mockMvc.perform(get("/api/v2/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(11L))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(33))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(1));
    }

    private Event sampleEvent(Long id) {
        return Event.builder()
                .id(id)
                .build();
    }
}
