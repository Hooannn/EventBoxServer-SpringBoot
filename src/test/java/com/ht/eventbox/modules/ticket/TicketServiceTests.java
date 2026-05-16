package com.ht.eventbox.modules.ticket;

import com.corundumstudio.socketio.SocketIOServer;
import com.ht.eventbox.config.HttpException;
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
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.FeedbackSentimentType;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.enums.TicketItemTraceEvent;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.auth.AuthService;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.event.EventService;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.order.OrderRepository;
import com.ht.eventbox.modules.order.TicketItemRepository;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.sentiment.SentimentAnalystService;
import com.ht.eventbox.modules.ticket.dtos.FeedbackTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.GiveawayTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
import com.ht.eventbox.modules.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventService eventService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TicketItemTraceRepository ticketItemTraceRepository;

    @Mock
    private SocketIOServer socketIOServer;

    @Mock
    private MailService mailService;

    @Mock
    private SentimentAnalystService sentimentAnalystService;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ticketService, "qrcodeSecretKey", "qr-secret");
    }

    @Test
    void getTicketItemById_shouldReturnProjectionWhenFound() {
        var projection = sampleTicketItemDetails(88L);
        when(ticketItemRepository.findById(88L, TicketService.TicketItemDetails.class)).thenReturn(Optional.of(projection));

        var result = ticketService.getTicketItemById(88L);

        assertThat(result).isSameAs(projection);
    }

    @Test
    void getTicketItemById_shouldThrowWhenMissing() {
        when(ticketItemRepository.findById(88L, TicketService.TicketItemDetails.class)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketItemById(88L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.TICKET_ITEM_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void getTicketItemQrCode_shouldRejectBeforeShowStarts() {
        var item = sampleTicketItemDetails(88L, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        when(ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(88L, 42L, OrderStatus.FULFILLED, TicketService.TicketItemDetails.class))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> ticketService.getTicketItemQrCode(42L, 88L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.SHOW_NOT_STARTED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void getTicketItemQrCode_shouldRejectAfterShowEnds() {
        var item = sampleTicketItemDetails(88L, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(1));
        when(ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(88L, 42L, OrderStatus.FULFILLED, TicketService.TicketItemDetails.class))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> ticketService.getTicketItemQrCode(42L, 88L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.SHOW_ENDED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void getTicketItemQrCode_shouldGenerateTokenWhenAllowed() {
        var item = sampleTicketItemDetails(88L, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusHours(1));
        when(ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(88L, 42L, OrderStatus.FULFILLED, TicketService.TicketItemDetails.class))
                .thenReturn(Optional.of(item));
        when(jwtService.generateQrCode(88L)).thenReturn("qr-token");

        var result = ticketService.getTicketItemQrCode(42L, 88L);

        assertThat(result).isEqualTo("qr-token");
    }

    @Test
    void validateTicketItem_shouldRejectInvalidToken() {
        when(jwtService.isTokenValid("bad-token", "qr-secret")).thenReturn(false);

        assertThatThrownBy(() -> ticketService.validateTicketItem(42L, ValidateTicketItemDto.builder()
                        .token("bad-token")
                        .eventShowId(77L)
                        .build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.TICKET_ITEM_INVALID);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void validateTicketItem_shouldRejectNonMember() {
        when(jwtService.isTokenValid("token", "qr-secret")).thenReturn(true);
        when(jwtService.extractSub("token", "qr-secret")).thenReturn("88");
        when(ticketItemRepository.findByIdAndOrderStatusIsAndTicketEventShowId(88L, OrderStatus.FULFILLED, 77L, TicketService.TicketItemDetails.class))
                .thenReturn(Optional.of(sampleTicketItemDetails(88L)));
        when(organizationRepository.existsByIdAndUserOrganizationsUserId(9L, 42L)).thenReturn(false);

        assertThatThrownBy(() -> ticketService.validateTicketItem(42L, ValidateTicketItemDto.builder()
                        .token("token")
                        .eventShowId(77L)
                        .build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.USER_NOT_IN_ORGANIZATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void validateTicketItem_shouldReturnProjectionWhenValid() {
        var projection = sampleTicketItemDetails(88L);
        when(jwtService.isTokenValid("token", "qr-secret")).thenReturn(true);
        when(jwtService.extractSub("token", "qr-secret")).thenReturn("88");
        when(ticketItemRepository.findByIdAndOrderStatusIsAndTicketEventShowId(88L, OrderStatus.FULFILLED, 77L, TicketService.TicketItemDetails.class))
                .thenReturn(Optional.of(projection));
        when(organizationRepository.existsByIdAndUserOrganizationsUserId(9L, 42L)).thenReturn(true);

        var result = ticketService.validateTicketItem(42L, ValidateTicketItemDto.builder()
                .token("token")
                .eventShowId(77L)
                .build());

        assertThat(result).isSameAs(projection);
    }

    @Test
    void createTicketItemTrace_shouldToggleTraceEvents() {
        var projection = sampleTicketItemDetails(88L);
        projection.getTraces().add(sampleTrace(TicketItemTraceEvent.CHECKED_IN));
        when(jwtService.isTokenValid("token", "qr-secret")).thenReturn(true);
        when(jwtService.extractSub("token", "qr-secret")).thenReturn("88");
        when(ticketItemRepository.findByIdAndOrderStatusIsAndTicketEventShowId(88L, OrderStatus.FULFILLED, 77L, TicketService.TicketItemDetails.class))
                .thenReturn(Optional.of(projection));
        when(organizationRepository.existsByIdAndUserOrganizationsUserId(9L, 42L)).thenReturn(true);

        var result = ticketService.createTicketItemTrace(42L, ValidateTicketItemDto.builder()
                .token("token")
                .eventShowId(77L)
                .build());

        assertThat(result).isTrue();
        verify(ticketItemTraceRepository).save(any(TicketItemTrace.class));
    }

    @Test
    void getTicketItemByShowId_shouldRejectUnauthorizedUsers() {
        var event = sampleEvent(9L);
        when(eventRepository.findByShowsId(77L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.getTicketItemByShowId(42L, 77L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                });
    }

    @Test
    void createTicketItemFeedback_shouldRejectUnusedTicket() {
        var projection = sampleTicketItemDetails(88L);
        when(ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(88L, 42L, OrderStatus.FULFILLED, TicketItem.class))
                .thenReturn(Optional.of(sampleTicketItemWithNoTraces()));

        assertThatThrownBy(() -> ticketService.createTicketItemFeedback(42L, 88L, sampleFeedbackDto()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.TICKET_ITEM_NOT_USED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void createTicketItemFeedback_shouldPersistFeedback() {
        var ticketItem = sampleTicketItemWithNoTraces();
        ticketItem.setTraces(new ArrayList<>(List.of(sampleTrace(TicketItemTraceEvent.CHECKED_IN))));
        ticketItem.getTicket().getEventShow().setEndTime(LocalDateTime.now().minusHours(1));
        when(ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(88L, 42L, OrderStatus.FULFILLED, TicketItem.class))
                .thenReturn(Optional.of(ticketItem));

        var result = ticketService.createTicketItemFeedback(42L, 88L, sampleFeedbackDto());

        assertThat(result).isTrue();
        verify(ticketItemRepository).save(ticketItem);
        verify(sentimentAnalystService).updateFeedbackSentiment(88L);
    }

    @Test
    void giveawayTicketItem_shouldRejectSelfGift() {
        var item = sampleFulfilledTicketItemEntity(88L, 42L, 1);
        when(ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(88L, 42L, OrderStatus.FULFILLED, TicketItem.class))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> ticketService.giveawayTicketItem(42L, 88L, sampleGiveawayDto("owner@example.com")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void triggerReminder_shouldReturnFalseWhenMailFails() throws Exception {
        var item = sampleFulfilledTicketItemEntity(88L, 42L, 1);
        when(ticketItemRepository.findById(88L)).thenReturn(Optional.of(item));
        doAnswer(invocation -> {
            throw new jakarta.mail.MessagingException("mail fail");
        }).when(mailService).sendReminderEmail(anyString(), any(Event.class), any(EventShow.class));

        var result = ticketService.triggerReminder(88L);

        assertThat(result).isFalse();
    }

    @Test
    void getLatestTicketItemFeedbackByOrganizationId_shouldRejectMissingOrg() {
        when(organizationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getLatestTicketItemFeedbackByOrganizationId(9L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.ORGANIZATION_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void getTicketItemFeedbackByEventId_shouldRejectNonMembers() {
        when(eventService.isMember(42L, 77L, List.of(OrganizationRole.MANAGER, OrganizationRole.OWNER, OrganizationRole.STAFF)))
                .thenReturn(false);

        assertThatThrownBy(() -> ticketService.getTicketItemFeedbackByEventId(42L, 77L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                });
    }

    private TicketService.TicketItemDetails sampleTicketItemDetails(Long id) {
        return sampleTicketItemDetails(id, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
    }

    private TicketService.TicketItemDetails sampleTicketItemDetails(Long id, LocalDateTime startTime, LocalDateTime endTime) {
        return new TicketService.TicketItemDetails() {
            @Override public Long getId() { return id; }
            @Override public Double getPlaceTotal() { return 100000.0; }
            @Override public FeedbackSentimentType getFeedbackType() { return null; }
            @Override public TicketService.TicketView getTicket() { return sampleTicketView(startTime, endTime); }
            @Override public String getFeedback() { return null; }
            @Override public TicketService.OrderView getOrder() { return sampleOrderView(); }
            @Override public List<TicketItemTrace> getTraces() { return new ArrayList<>(); }
            @Override public LocalDateTime getFeedbackAt() { return null; }
            @Override public LocalDateTime getCreatedAt() { return null; }
            @Override public LocalDateTime getUpdatedAt() { return null; }
        };
    }

    private TicketService.TicketView sampleTicketView(LocalDateTime startTime, LocalDateTime endTime) {
        return new TicketService.TicketView() {
            @Override public Long getId() { return 1L; }
            @Override public String getName() { return "Standard"; }
            @Override public String getDescription() { return "Desc"; }
            @Override public TicketService.EventShowView getEventShow() { return sampleEventShowView(startTime, endTime); }
        };
    }

    private TicketService.EventShowView sampleEventShowView(LocalDateTime startTime, LocalDateTime endTime) {
        return new TicketService.EventShowView() {
            @Override public Long getId() { return 77L; }
            @Override public String getTitle() { return "Show"; }
            @Override public TicketService.EventView getEvent() { return sampleEventView(); }
            @Override public LocalDateTime getStartTime() { return startTime; }
            @Override public LocalDateTime getEndTime() { return endTime; }
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
            @Override public String getEmail() { return "owner@example.com"; }
            @Override public String getFirstName() { return "Owner"; }
            @Override public String getLastName() { return "User"; }
            @Override public Set<Asset> getAssets() { return Set.of(); }
        };
    }

    private TicketItemTrace sampleTrace(com.ht.eventbox.enums.TicketItemTraceEvent event) {
        return TicketItemTrace.builder().event(event).build();
    }

    private TicketItem sampleTicketItemWithNoTraces() {
        return TicketItem.builder()
                .id(88L)
                .ticket(Ticket.builder()
                        .id(1L)
                        .eventShow(EventShow.builder()
                                .id(77L)
                                .startTime(LocalDateTime.now().minusHours(1))
                                .endTime(LocalDateTime.now().plusHours(1))
                                .event(Event.builder()
                                        .id(9L)
                                        .organization(Organization.builder().id(9L).build())
                                        .build())
                                .build())
                        .build())
                .order(Order.builder()
                        .id(1L)
                        .user(User.builder().id(42L).email("owner@example.com").firstName("Owner").lastName("User").build())
                        .build())
                .traces(new ArrayList<>())
                .build();
    }

    private TicketItem sampleFulfilledTicketItemEntity(Long ticketItemId, Long userId, int traceCount) {
        var item = sampleTicketItemWithNoTraces();
        item.setId(ticketItemId);
        item.getOrder().setStatus(OrderStatus.FULFILLED);
        item.getOrder().setUser(User.builder().id(userId).email("owner@example.com").firstName("Owner").lastName("User").build());
        item.setTraces(new ArrayList<>());
        for (int i = 0; i < traceCount; i++) {
            item.getTraces().add(sampleTrace(TicketItemTraceEvent.CHECKED_IN));
        }
        return item;
    }

    private TicketItem sampleFulfilledTicketItem(long ticketItemId, LocalDateTime startTime, LocalDateTime endTime) {
        var item = sampleFulfilledTicketItemEntity(ticketItemId, 42L, 0);
        item.getTicket().getEventShow().setStartTime(startTime);
        item.getTicket().getEventShow().setEndTime(endTime);
        return item;
    }

    private Event sampleEvent(Long orgId) {
        return Event.builder()
                .id(9L)
                .status(EventStatus.PUBLISHED)
                .organization(Organization.builder().id(orgId).userOrganizations(List.of()).build())
                .shows(List.of())
                .build();
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

    private FeedbackTicketItemDto sampleFeedbackDto() {
        return FeedbackTicketItemDto.builder().feedback("Great event").build();
    }

    private GiveawayTicketItemDto sampleGiveawayDto(String recipientEmail) {
        return GiveawayTicketItemDto.builder()
                .recipientEmail(recipientEmail)
                .password("secret")
                .build();
    }
}
