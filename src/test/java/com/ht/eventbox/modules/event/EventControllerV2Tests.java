package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.modules.event.dtos.EventOverviewDto;
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
import static org.mockito.Mockito.verify;
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
        when(eventService.getAllByStatusIn(eq(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)), eq("music"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(11L)), PageRequest.of(0, 20), 33));

        mockMvc.perform(get("/api/v2/events")
                .param("search", "music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(11L))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(33))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(1));

        verify(eventService).getAllByStatusIn(eq(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)), eq("music"),
                any(Pageable.class));
    }

    @Test
    void getAllPending_shouldReturnPagedEvents() throws Exception {
        when(eventService.getAllPending(eq("draft"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(12L)), PageRequest.of(1, 10), 11));

        mockMvc.perform(get("/api/v2/events/pending")
                .param("search", "draft")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(12L))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1));

        verify(eventService).getAllPending(eq("draft"), any(Pageable.class));
    }

    @Test
    void getAllPublished_shouldReturnPagedEvents() throws Exception {
        when(eventService.getAllPublished(eq("festival"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(13L)), PageRequest.of(0, 5), 6));

        mockMvc.perform(get("/api/v2/events/published")
                .param("search", "festival")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(13L))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(1));

        verify(eventService).getAllPublished(eq("festival"), any(Pageable.class));
    }

    @Test
    void getAllEnded_shouldReturnPagedEvents() throws Exception {
        when(eventService.getAllEnded(eq("concert"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(14L)), PageRequest.of(2, 7), 15));

        mockMvc.perform(get("/api/v2/events/ended")
                .param("search", "concert")
                .param("page", "2")
                .param("size", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(14L))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.size").value(7))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(1));

        verify(eventService).getAllEnded(eq("concert"), any(Pageable.class));
    }

    @Test
    void getOverview_shouldReturnCounts() throws Exception {
        when(eventService.getOverview(eq("music")))
                .thenReturn(EventOverviewDto.builder()
                        .pendingCount(4L)
                        .publishedCount(7L)
                        .endedCount(2L)
                        .build());

        mockMvc.perform(get("/api/v2/events/overview")
                .param("search", "music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_count").value(4))
                .andExpect(jsonPath("$.data.published_count").value(7))
                .andExpect(jsonPath("$.data.ended_count").value(2));

        verify(eventService).getOverview(eq("music"));
    }

    private Event sampleEvent(Long id) {
        return Event.builder()
                .id(id)
                .build();
    }
}
