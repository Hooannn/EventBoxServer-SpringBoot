package com.ht.eventbox.modules.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventTagsDto;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "paypal.checkout.webhook.id=checkout-webhook",
        "paypal.payment.webhook.id=payment-webhook"
})
class EventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getAll_shouldReturnPendingAndPublishedEvents() throws Exception {
        when(eventService.getAllByStatusIn(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)))
                .thenReturn(List.of(sampleEvent(11L)));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(11L));
    }

    @Test
    void publishByAdmin_shouldReturnUpdateResponse() throws Exception {
        when(eventService.publishByAdmin(11L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/events/11/admin/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void archiveByAdmin_shouldReturnUpdateResponse() throws Exception {
        when(eventService.archiveByAdmin(11L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/events/11/admin/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateTags_shouldReturnUpdateResponse() throws Exception {
        when(eventService.updateTags(eq(11L), any(UpdateEventTagsDto.class))).thenReturn(true);

        mockMvc.perform(put("/api/v1/events/11/admin/tags")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateEventTagsDto.builder()
                                .featured(true)
                                .trending(false)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));

        verify(eventService).updateTags(eq(11L), any(UpdateEventTagsDto.class));
    }

    @Test
    void create_shouldReturnCreatedResponse() throws Exception {
        when(eventService.create(eq(42L), any(CreateEventDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/events")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateEventDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Created"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void update_shouldReturnUpdateResponse() throws Exception {
        when(eventService.update(eq(42L), eq(11L), any(UpdateEventDto.class))).thenReturn(true);

        mockMvc.perform(put("/api/v1/events/11")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateEventDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void archive_shouldReturnUpdateResponse() throws Exception {
        when(eventService.archive(42L, 11L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/events/11/archive")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void inactive_shouldReturnUpdateResponse() throws Exception {
        when(eventService.inactive(42L, 11L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/events/11/inactive")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void active_shouldReturnUpdateResponse() throws Exception {
        when(eventService.active(42L, 11L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/events/11/active")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getByOrganizationId_shouldReturnEvents() throws Exception {
        when(eventService.getByOrganizationIdAndStatusIsNot(99L, EventStatus.ARCHIVED))
                .thenReturn(List.of(sampleEvent(11L)));

        mockMvc.perform(get("/api/v1/events/organization/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(11L));
    }

    @Test
    void getPublishedByOrganizationId_shouldReturnEvents() throws Exception {
        when(eventService.getByOrganizationIdAndStatusIs(99L, EventStatus.PUBLISHED))
                .thenReturn(List.of(sampleEvent(11L)));

        mockMvc.perform(get("/api/v1/events/organization/99/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(11L));
    }

    @Test
    void getById_shouldReturnEvent() throws Exception {
        when(eventService.getByIdAndStatusIsNot(11L, EventStatus.ARCHIVED)).thenReturn(sampleEvent(11L));

        mockMvc.perform(get("/api/v1/events/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(11L));
    }

    @Test
    void getShowsById_shouldReturnShows() throws Exception {
        when(eventService.getShowsById(11L)).thenReturn(List.of(sampleShow(21L)));

        mockMvc.perform(get("/api/v1/events/11/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(21L));
    }

    @Test
    void getPublicById_shouldReturnEventWithRealStock() throws Exception {
        when(eventService.getWithRealStockByIdAndStatusIsNot(11L, EventStatus.ARCHIVED)).thenReturn(sampleEvent(11L));

        mockMvc.perform(get("/api/v1/events/public/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(11L));
    }

    @Test
    void getDiscovery_shouldReturnDiscoveryPayload() throws Exception {
        when(eventService.getDiscovery()).thenReturn(EventService.DiscoveryEvents.builder()
                .featuredEvents(List.of(sampleEvent(11L)))
                .trendingEvents(List.of(sampleEvent(12L)))
                .latestEvents(List.of(sampleEvent(13L)))
                .build());

        mockMvc.perform(get("/api/v1/events/discovery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.featured_events[0].id").value(11L))
                .andExpect(jsonPath("$.data.trending_events[0].id").value(12L))
                .andExpect(jsonPath("$.data.latest_events[0].id").value(13L));
    }

    @Test
    void search_shouldPassQueryProvinceAndCategoriesThrough() throws Exception {
        when(eventService.search(eq("music"), eq("Singapore"), eq(List.of(1L, 2L)))).thenReturn(List.of(sampleEvent(11L)));

        mockMvc.perform(get("/api/v1/events/search")
                        .param("q", "music")
                        .param("province", "Singapore")
                        .param("categories", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(11L));

        verify(eventService).search("music", "Singapore", List.of(1L, 2L));
    }

    @Test
    void getByCategoryId_shouldReturnEvents() throws Exception {
        when(eventService.getAllByCategoriesIdAndStatusIs(7L, EventStatus.PUBLISHED))
                .thenReturn(List.of(sampleEvent(11L)));

        mockMvc.perform(get("/api/v1/events/categories/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(11L));
    }

    @Test
    void eventPayout_shouldReturnSuccessResponse() throws Exception {
        when(eventService.eventPayout(42L, 11L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/events/11/payout/request")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.REQUEST_FOR_PAYOUT_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    private Event sampleEvent(Long id) {
        return Event.builder()
                .id(id)
                .title("Event " + id)
                .description("Description")
                .address("Address")
                .placeName("Place")
                .status(EventStatus.PUBLISHED)
                .shows(List.of())
                .build();
    }

    private EventShow sampleShow(Long id) {
        return EventShow.builder()
                .id(id)
                .title("Show " + id)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .saleStartTime(LocalDateTime.now().minusDays(1))
                .saleEndTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    private CreateEventDto sampleCreateEventDto() {
        var ticket = CreateEventDto.CreateShowDto.CreateTicketTypeDto.builder()
                .seatmapBlockId("A1")
                .name("Standard")
                .price(100.0)
                .initialStock(10)
                .build();
        var show = CreateEventDto.CreateShowDto.builder()
                .title("Opening")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .saleStartTime(LocalDateTime.now().minusHours(1))
                .saleEndTime(LocalDateTime.now().plusDays(1))
                .enabledSeatmap(false)
                .ticketTypeInputs(List.of(ticket))
                .build();

        return CreateEventDto.builder()
                .organizationId(9L)
                .title("Summer Fest")
                .description("Description")
                .address("Address")
                .placeName("Place")
                .backgroundBase64("background")
                .logoBase64("logo")
                .categoryIds(List.of(1L))
                .keywords(List.of("tag"))
                .showInputs(List.of(show))
                .build();
    }

    private UpdateEventDto sampleUpdateEventDto() {
        var ticket = CreateEventDto.CreateShowDto.CreateTicketTypeDto.builder()
                .seatmapBlockId("A1")
                .name("Standard")
                .price(100.0)
                .initialStock(10)
                .build();
        var show = CreateEventDto.CreateShowDto.builder()
                .title("Updated")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .saleStartTime(LocalDateTime.now().minusHours(1))
                .saleEndTime(LocalDateTime.now().plusDays(2))
                .enabledSeatmap(false)
                .ticketTypeInputs(List.of(ticket))
                .build();

        return UpdateEventDto.builder()
                .title("Updated title")
                .description("Updated description")
                .address("Updated address")
                .placeName("Updated place")
                .categoryIds(List.of(1L))
                .keywords(List.of("tag"))
                .showInputs(List.of(show))
                .build();
    }
}
