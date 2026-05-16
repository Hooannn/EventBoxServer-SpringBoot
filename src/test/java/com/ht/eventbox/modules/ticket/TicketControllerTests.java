package com.ht.eventbox.modules.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.entities.TicketItemTrace;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.FeedbackSentimentType;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.modules.ticket.dtos.FeedbackTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.GiveawayTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "paypal.checkout.webhook.id=checkout-webhook",
        "paypal.payment.webhook.id=payment-webhook"
})
class TicketControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getMyTicketItems_shouldReturnTicketItems() throws Exception {
        when(ticketService.getTicketItemsByUserIdAndOrderStatusIn(42L, List.of(OrderStatus.FULFILLED, OrderStatus.APPROVED)))
                .thenReturn(List.of(sampleTicketItemDetails(88L)));

        mockMvc.perform(get("/api/v1/tickets/items/me")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(88L));
    }

    @Test
    void getTicketItemById_shouldReturnTicketItemDetails() throws Exception {
        when(ticketService.getTicketItemById(88L)).thenReturn(sampleTicketItemDetails(88L));

        mockMvc.perform(get("/api/v1/tickets/items/88")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(88L));
    }

    @Test
    void getTicketItemQrCode_shouldReturnQrCode() throws Exception {
        when(ticketService.getTicketItemQrCode(42L, 88L)).thenReturn("qr-token");

        mockMvc.perform(get("/api/v1/tickets/items/88/qrcode")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value("qr-token"));
    }

    @Test
    void validateTicketItem_shouldReturnValidatedTicketItem() throws Exception {
        when(ticketService.validateTicketItem(eq(42L), any(ValidateTicketItemDto.class)))
                .thenReturn(sampleTicketItemDetails(88L));

        mockMvc.perform(post("/api/v1/tickets/validate")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ValidateTicketItemDto.builder()
                                .token("token")
                                .eventShowId(77L)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(88L));
    }

    @Test
    void createTicketItemTrace_shouldReturnSuccessResponse() throws Exception {
        when(ticketService.createTicketItemTrace(eq(42L), any(ValidateTicketItemDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/tickets/traces")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ValidateTicketItemDto.builder()
                                .token("token")
                                .eventShowId(77L)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getTicketItemByShowId_shouldReturnTicketItems() throws Exception {
        when(ticketService.getTicketItemByShowId(42L, 77L)).thenReturn(List.of(sampleTicketItemEntity(88L)));

        mockMvc.perform(get("/api/v1/tickets/shows/77/items")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(88L));
    }

    @Test
    void createTicketItemFeedback_shouldReturnCreatedResponse() throws Exception {
        when(ticketService.createTicketItemFeedback(eq(42L), eq(88L), any(FeedbackTicketItemDto.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/tickets/items/88/feedback")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FeedbackTicketItemDto.builder()
                                .feedback("Great")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void giveawayTicketItem_shouldReturnSuccessResponse() throws Exception {
        when(ticketService.giveawayTicketItem(eq(42L), eq(88L), any(GiveawayTicketItemDto.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/tickets/items/88/giveaway")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(GiveawayTicketItemDto.builder()
                                .recipientEmail("friend@example.com")
                                .password("secret")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getLatestTicketItemFeedbackByOrganization_shouldReturnTicketItems() throws Exception {
        when(ticketService.getLatestTicketItemFeedbackByOrganizationId(9L)).thenReturn(List.of(sampleTicketItemDetails(88L)));

        mockMvc.perform(get("/api/v1/tickets/items/feedback/organizations/9")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(88L));
    }

    @Test
    void getTicketItemFeedbackByEvent_shouldReturnTicketItems() throws Exception {
        when(ticketService.getTicketItemFeedbackByEventId(42L, 77L)).thenReturn(List.of(sampleTicketItemDetails(88L)));

        mockMvc.perform(get("/api/v1/tickets/items/feedback/event/77")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(88L));
    }

    @Test
    void triggerReminder_shouldReturnSuccessResponse() throws Exception {
        when(ticketService.triggerReminder(88L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/tickets/items/88/reminder/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
    }

    private TicketService.TicketItemDetails sampleTicketItemDetails(Long id) {
        return new TicketService.TicketItemDetails() {
            @Override public Long getId() { return id; }
            @Override public Double getPlaceTotal() { return 100000.0; }
            @Override public FeedbackSentimentType getFeedbackType() { return null; }
            @Override public TicketService.TicketView getTicket() { return sampleTicketView(); }
            @Override public String getFeedback() { return null; }
            @Override public TicketService.OrderView getOrder() { return sampleOrderView(); }
            @Override public List<TicketItemTrace> getTraces() { return new ArrayList<>(); }
            @Override public LocalDateTime getFeedbackAt() { return null; }
            @Override public LocalDateTime getCreatedAt() { return null; }
            @Override public LocalDateTime getUpdatedAt() { return null; }
        };
    }

    private TicketItem sampleTicketItemEntity(Long id) {
        return TicketItem.builder()
                .id(id)
                .ticket(Ticket.builder()
                        .id(1L)
                        .name("Standard")
                        .eventShow(EventShow.builder()
                                .id(77L)
                                .title("Show")
                                .startTime(LocalDateTime.now().minusHours(1))
                                .endTime(LocalDateTime.now().plusHours(1))
                                .event(Event.builder()
                                        .id(9L)
                                        .title("Event")
                                        .description("Desc")
                                        .address("Address")
                                        .placeName("Place")
                                        .organization(Organization.builder().id(9L).build())
                                        .assets(Set.of(sampleAsset()))
                                        .build())
                                .build())
                        .build())
                .order(Order.builder()
                        .id(1L)
                        .status(OrderStatus.FULFILLED)
                        .user(User.builder().id(42L).email("user@example.com").firstName("User").lastName("Test").build())
                        .build())
                .build();
    }

    private TicketService.TicketView sampleTicketView() {
        return new TicketService.TicketView() {
            @Override public Long getId() { return 1L; }
            @Override public String getName() { return "Standard"; }
            @Override public String getDescription() { return "Desc"; }
            @Override public TicketService.EventShowView getEventShow() { return sampleEventShowView(); }
        };
    }

    private TicketService.EventShowView sampleEventShowView() {
        return new TicketService.EventShowView() {
            @Override public Long getId() { return 77L; }
            @Override public String getTitle() { return "Show"; }
            @Override public TicketService.EventView getEvent() { return sampleEventView(); }
            @Override public LocalDateTime getStartTime() { return LocalDateTime.now().minusHours(1); }
            @Override public LocalDateTime getEndTime() { return LocalDateTime.now().plusHours(1); }
        };
    }

    private TicketService.EventView sampleEventView() {
        return new TicketService.EventView() {
            @Override public Long getId() { return 9L; }
            @Override public String getTitle() { return "Event"; }
            @Override public String getDescription() { return "Desc"; }
            @Override public String getAddress() { return "Address"; }
            @Override public String getPlaceName() { return "Place"; }
            @Override public TicketService.OrganizationView getOrganization() { return () -> 9L; }
            @Override public Set<Asset> getAssets() { return Set.of(sampleAsset()); }
        };
    }

    private TicketService.OrderView sampleOrderView() {
        return new TicketService.OrderView() {
            @Override public Long getId() { return 1L; }
            @Override public OrderStatus getStatus() { return OrderStatus.FULFILLED; }
            @Override public List<com.ht.eventbox.entities.Payment> getPayments() { return List.of(); }
            @Override public TicketService.UserView getUser() { return sampleUserView(); }
            @Override public Double getPlaceTotal() { return 100000.0; }
            @Override public LocalDateTime getCreatedAt() { return null; }
            @Override public LocalDateTime getUpdatedAt() { return null; }
        };
    }

    private TicketService.UserView sampleUserView() {
        return new TicketService.UserView() {
            @Override public Long getId() { return 42L; }
            @Override public String getEmail() { return "user@example.com"; }
            @Override public String getFirstName() { return "User"; }
            @Override public String getLastName() { return "Test"; }
            @Override public Set<Asset> getAssets() { return Set.of(); }
        };
    }

    private Asset sampleAsset() {
        return Asset.builder()
                .id("asset")
                .signature("sig")
                .publicId("public")
                .originalUrl("https://example.com")
                .secureUrl("https://example.com")
                .usage(AssetUsage.EVENT_LOGO)
                .format("png")
                .resourceType("image")
                .folder("event-assets")
                .eTag("etag")
                .width(100)
                .height(100)
                .bytes(1234)
                .build();
    }
}
