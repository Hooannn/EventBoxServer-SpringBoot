package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.entities.Category;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Keyword;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.entities.UserOrganization;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.backgroundjobs.NotificationJobService;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.category.CategoryRepository;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
import com.ht.eventbox.modules.event.dtos.EventOverviewDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventTagsDto;
import com.ht.eventbox.modules.keyword.KeywordRepository;
import com.ht.eventbox.modules.order.CurrencyConverterServiceV2;
import com.ht.eventbox.modules.order.OrderRepository;
import com.ht.eventbox.modules.order.PayPalService;
import com.ht.eventbox.modules.order.TicketItemRepository;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.storage.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTests {

    @Mock
    private EventShowRepository eventShowRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private NotificationJobService notificationJobService;

    @Mock
    private PayPalService payPalService;

    @Mock
    private CurrencyConverterServiceV2 currencyConverterService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void getById_shouldReturnEventWhenFound() {
        var event = Event.builder().id(7L).build();
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));

        var result = eventService.getById(7L);

        assertThat(result).isSameAs(event);
    }

    @Test
    void getById_shouldThrowWhenMissing() {
        when(eventRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById(7L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.EVENT_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void getDiscovery_shouldQueryFeaturedTrendingAndLatestBranches() {
        var featured = Event.builder().id(1L).build();
        var trending = Event.builder().id(2L).build();
        var latest = Event.builder().id(3L).build();

        when(eventRepository.findDistinctByStatusInAndFeaturedIsTrueAndShowsEndTimeAfter(
                eq(List.of(EventStatus.PUBLISHED)), any(LocalDateTime.class), any()))
                .thenReturn(new PageImpl<>(List.of(featured)));
        when(eventRepository.findDistinctByStatusInAndTrendingIsTrueAndShowsEndTimeAfter(
                eq(List.of(EventStatus.PUBLISHED)), any(LocalDateTime.class), any()))
                .thenReturn(new PageImpl<>(List.of(trending)));
        when(eventRepository.findDistinctByStatusInAndShowsEndTimeAfter(
                eq(List.of(EventStatus.PUBLISHED)), any(LocalDateTime.class), any()))
                .thenReturn(new PageImpl<>(List.of(latest)));

        var result = eventService.getDiscovery();

        assertThat(result.getFeaturedEvents()).containsExactly(featured);
        assertThat(result.getTrendingEvents()).containsExactly(trending);
        assertThat(result.getLatestEvents()).containsExactly(latest);
    }

    @Test
    void getAllByStatusInPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Event.builder().id(11L).build()), PageRequest.of(1, 10), 33);
        when(eventRepository.findAllByStatusInOrderByIdAsc(eq(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)), any()))
                .thenReturn(page);

        var result = eventService.getAllByStatusIn(List.of(EventStatus.PENDING, EventStatus.PUBLISHED), PageRequest.of(1, 10));

        assertThat(result).isSameAs(page);
        verify(eventRepository).findAllByStatusInOrderByIdAsc(eq(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)), eq(PageRequest.of(1, 10)));
    }

    @Test
    void getAllByStatusInSearchPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Event.builder().id(11L).build()), PageRequest.of(1, 10), 33);
        when(eventRepository.searchAllByStatusInOrderByIdAsc(eq(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)), eq("music"), any()))
                .thenReturn(page);

        var result = eventService.getAllByStatusIn(List.of(EventStatus.PENDING, EventStatus.PUBLISHED), "music", PageRequest.of(1, 10));

        assertThat(result).isSameAs(page);
        verify(eventRepository).searchAllByStatusInOrderByIdAsc(eq(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)), eq("music"), eq(PageRequest.of(1, 10)));
    }

    @Test
    void getAllPublishedSearchPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Event.builder().id(12L).build()), PageRequest.of(0, 5), 6);
        when(eventRepository.searchPublishedByStatusOrderByIdAsc(eq(EventStatus.PUBLISHED), eq("festival"), any(LocalDateTime.class), any()))
                .thenReturn(page);

        var result = eventService.getAllPublished("festival", PageRequest.of(0, 5));

        assertThat(result).isSameAs(page);
        verify(eventRepository).searchPublishedByStatusOrderByIdAsc(eq(EventStatus.PUBLISHED), eq("festival"), any(LocalDateTime.class), eq(PageRequest.of(0, 5)));
    }

    @Test
    void getAllEndedSearchPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Event.builder().id(13L).build()), PageRequest.of(2, 7), 15);
        when(eventRepository.searchEndedByStatusOrderByIdAsc(eq(EventStatus.PUBLISHED), eq("concert"), any(LocalDateTime.class), any()))
                .thenReturn(page);

        var result = eventService.getAllEnded("concert", PageRequest.of(2, 7));

        assertThat(result).isSameAs(page);
        verify(eventRepository).searchEndedByStatusOrderByIdAsc(eq(EventStatus.PUBLISHED), eq("concert"), any(LocalDateTime.class), eq(PageRequest.of(2, 7)));
    }

    @Test
    void getOverview_shouldDelegateToCountQueries() {
        when(eventRepository.countSearchAllByStatusIn(eq(List.of(EventStatus.PENDING)), eq("music")))
                .thenReturn(4L);
        when(eventRepository.countSearchPublishedByStatus(eq(EventStatus.PUBLISHED), eq("music"), any(LocalDateTime.class)))
                .thenReturn(7L);
        when(eventRepository.countSearchEndedByStatus(eq(EventStatus.PUBLISHED), eq("music"), any(LocalDateTime.class)))
                .thenReturn(2L);

        var result = eventService.getOverview("music");

        assertThat(result).isEqualTo(EventOverviewDto.builder()
                .pendingCount(4L)
                .publishedCount(7L)
                .endedCount(2L)
                .build());
        verify(eventRepository).countSearchAllByStatusIn(eq(List.of(EventStatus.PENDING)), eq("music"));
        verify(eventRepository).countSearchPublishedByStatus(eq(EventStatus.PUBLISHED), eq("music"), any(LocalDateTime.class));
        verify(eventRepository).countSearchEndedByStatus(eq(EventStatus.PUBLISHED), eq("music"), any(LocalDateTime.class));
    }

    @Test
    void search_shouldUseProvinceBranchWhenProvinceIsPresent() {
        var event = Event.builder().id(11L).build();
        when(eventRepository.searchEvents(eq("music"), eq("Singapore"), eq(List.of(1L, 2L)), eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(event));

        var result = eventService.search("music", "Singapore", List.of(1L, 2L));

        assertThat(result).containsExactly(event);
    }

    @Test
    void search_shouldTreatEmptyCategoryListAsNull() {
        var event = Event.builder().id(12L).build();
        when(eventRepository.searchEvents(eq("music"), eq((List<Long>) null), eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(event));

        var result = eventService.search("music", null, List.of());

        assertThat(result).containsExactly(event);
    }

    @Test
    void create_shouldPersistPendingEventWithShowsAndAssets() throws Exception {
        var dto = sampleCreateEventDto();
        var org = sampleOrganization(42L);
        var category = Category.builder().id(5L).build();
        var keyword = Keyword.builder().name("existing").build();

        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L, OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));
        when(categoryRepository.findAllById(List.of(5L))).thenReturn(List.of(category));
        when(keywordRepository.findById("existing")).thenReturn(Optional.of(keyword));
        when(keywordRepository.findById("new-tag")).thenReturn(Optional.empty());
        when(keywordRepository.save(any(Keyword.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cloudinaryService.uploadByBase64(eq("logo-base64"), anyString())).thenReturn(sampleUploadResult("logo"));
        when(cloudinaryService.uploadByBase64(eq("background-base64"), anyString())).thenReturn(sampleUploadResult("background"));

        var captor = ArgumentCaptor.forClass(Event.class);

        var result = eventService.create(42L, dto);

        assertThat(result).isTrue();
        verify(eventRepository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(saved.getOrganization()).isSameAs(org);
        assertThat(saved.getCategories()).containsExactly(category);
        assertThat(saved.getKeywords()).extracting(Keyword::getName).containsExactlyInAnyOrder("existing", "new-tag");
        assertThat(saved.getShows()).hasSize(1);
        assertThat(saved.getAssets()).extracting(Asset::getUsage).containsExactlyInAnyOrder(AssetUsage.EVENT_LOGO, AssetUsage.EVENT_BANNER);
        assertThat(saved.getShows().get(0).getTickets()).hasSize(1);
    }

    @Test
    void create_shouldRejectMissingOrganization() {
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L, OrganizationRole.OWNER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(42L, sampleCreateEventDto()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.ORGANIZATION_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });

        verifyNoInteractions(cloudinaryService, eventRepository);
    }

    @Test
    void update_shouldRejectNonPendingEvent() {
        var event = sampleEvent(EventStatus.PUBLISHED);
        when(eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(7L, 42L, OrganizationRole.OWNER))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.update(42L, 7L, sampleUpdateEventDto()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void update_shouldPersistPendingEventChanges() {
        var event = sampleEvent(EventStatus.PENDING);
        event.setShows(new java.util.ArrayList<>(List.of(sampleShow())));
        event.setKeywords(new HashSet<>());
        event.setAssets(new HashSet<>());
        when(eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(7L, 42L, OrganizationRole.OWNER))
                .thenReturn(Optional.of(event));
        when(categoryRepository.findAllById(List.of(5L))).thenReturn(List.of(Category.builder().id(5L).build()));
        when(keywordRepository.findById("updated")).thenReturn(Optional.of(Keyword.builder().name("updated").build()));

        var result = eventService.update(42L, 7L, sampleUpdateEventDto());

        assertThat(result).isTrue();
        verify(eventRepository).save(event);
        assertThat(event.getTitle()).isEqualTo("Updated title");
        assertThat(event.getShows()).hasSize(1);
        assertThat(event.getKeywords()).extracting(Keyword::getName).containsExactly("updated");
    }

    @Test
    void publishByAdmin_shouldPublishPendingEvent() {
        var event = sampleEvent(EventStatus.PENDING);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));

        var result = eventService.publishByAdmin(7L);

        assertThat(result).isTrue();
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(eventRepository).save(event);
        verify(notificationJobService).enqueueEventPublished(7L);
    }

    @Test
    void publishByAdmin_shouldRejectNonPendingEvent() {
        when(eventRepository.findById(7L)).thenReturn(Optional.of(sampleEvent(EventStatus.PUBLISHED)));

        assertThatThrownBy(() -> eventService.publishByAdmin(7L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void archiveByAdmin_shouldArchivePendingEvent() {
        var event = sampleEvent(EventStatus.PENDING);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));

        var result = eventService.archiveByAdmin(7L);

        assertThat(result).isTrue();
        assertThat(event.getStatus()).isEqualTo(EventStatus.ARCHIVED);
    }

    @Test
    void archive_shouldRejectNonOwner() {
        when(eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(7L, 42L, OrganizationRole.OWNER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.archive(42L, 7L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.EVENT_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void inactive_shouldMovePublishedEventToDraft() {
        var event = sampleEvent(EventStatus.PUBLISHED);
        when(eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(7L, 42L, OrganizationRole.OWNER))
                .thenReturn(Optional.of(event));

        var result = eventService.inactive(42L, 7L);

        assertThat(result).isTrue();
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void active_shouldMoveDraftEventToPublished() {
        var event = sampleEvent(EventStatus.DRAFT);
        when(eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(7L, 42L, OrganizationRole.OWNER))
                .thenReturn(Optional.of(event));

        var result = eventService.active(42L, 7L);

        assertThat(result).isTrue();
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void updateTags_shouldRejectArchivedEvent() {
        when(eventRepository.findById(7L)).thenReturn(Optional.of(sampleEvent(EventStatus.DRAFT)));

        assertThatThrownBy(() -> eventService.updateTags(7L, UpdateEventTagsDto.builder().featured(true).trending(false).build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void getByIdAndStatusIsNot_shouldReturnEventWhenStatusDiffers() {
        var event = sampleEvent(EventStatus.PUBLISHED);
        when(eventRepository.findByIdAndStatusIsNot(7L, EventStatus.ARCHIVED)).thenReturn(Optional.of(event));

        var result = eventService.getByIdAndStatusIsNot(7L, EventStatus.ARCHIVED);

        assertThat(result).isSameAs(event);
    }

    @Test
    void getWithRealStockByIdAndStatusIsNot_shouldSubtractReservedStock() {
        var ticket = Ticket.builder().id(88L).stock(10).build();
        var show = EventShow.builder().id(9L).tickets(new java.util.ArrayList<>(List.of(ticket))).build();
        var event = Event.builder().id(7L).shows(new java.util.ArrayList<>(List.of(show))).build();
        when(eventRepository.findByIdAndStatusIsNot(7L, EventStatus.ARCHIVED)).thenReturn(Optional.of(event));
        when(ticketItemRepository.countAllByTicketIdAndOrderStatusInAndOrderExpiredAtIsAfter(eq(88L), anyList(), any(LocalDateTime.class)))
                .thenReturn(3L);

        var result = eventService.getWithRealStockByIdAndStatusIsNot(7L, EventStatus.ARCHIVED);

        assertThat(result.getShows().get(0).getTickets().get(0).getStock()).isEqualTo(7);
    }

    @Test
    void getAllByStatusIn_shouldReturnRepositoryResults() {
        var event = sampleEvent(EventStatus.PENDING);
        when(eventRepository.findAllByStatusInOrderByIdAsc(List.of(EventStatus.PENDING, EventStatus.PUBLISHED)))
                .thenReturn(List.of(event));

        var result = eventService.getAllByStatusIn(List.of(EventStatus.PENDING, EventStatus.PUBLISHED));

        assertThat(result).containsExactly(event);
    }

    @Test
    void eventPayout_shouldRejectNotEndedEvents() {
        var show = EventShow.builder().endTime(LocalDateTime.now().plusHours(1)).build();
        var event = sampleEvent(EventStatus.PUBLISHED);
        event.setShows(new java.util.ArrayList<>(List.of(show)));
        when(eventRepository.findByIdAndStatusIs(7L, EventStatus.PUBLISHED)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.eventPayout(42L, 7L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.EVENT_NOT_ENDED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void eventPayout_shouldRejectNonOwner() {
        var show = EventShow.builder().endTime(LocalDateTime.now().minusHours(1)).build();
        var event = sampleEvent(EventStatus.PUBLISHED);
        event.setShows(new java.util.ArrayList<>(List.of(show)));
        event.getOrganization().setUserOrganizations(List.of(sampleUserOrganization(99L, OrganizationRole.MANAGER)));
        when(eventRepository.findByIdAndStatusIs(7L, EventStatus.PUBLISHED)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.eventPayout(42L, 7L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                });
    }

    @Test
    void eventPayout_shouldMarkPaidWhenThereIsNoTotalAmount() {
        var show = EventShow.builder().endTime(LocalDateTime.now().minusHours(1)).build();
        var event = sampleEvent(EventStatus.PUBLISHED);
        event.setShows(new java.util.ArrayList<>(List.of(show)));
        event.getOrganization().setUserOrganizations(List.of(sampleUserOrganization(42L, OrganizationRole.OWNER)));
        when(eventRepository.findByIdAndStatusIs(7L, EventStatus.PUBLISHED)).thenReturn(Optional.of(event));
        when(orderRepository.findAllByItemsTicketEventShowEventIdAndStatusIs(7L, OrderStatus.FULFILLED)).thenReturn(List.of());

        var result = eventService.eventPayout(42L, 7L);

        assertThat(result).isTrue();
        assertThat(event.getPayoutAt()).isNotNull();
        verify(eventRepository).save(event);
        verifyNoInteractions(currencyConverterService, payPalService);
    }

    @Test
    void eventPayout_shouldConvertAndSendPayout() throws Exception {
        var show = EventShow.builder().endTime(LocalDateTime.now().minusHours(1)).build();
        var event = sampleEvent(EventStatus.PUBLISHED);
        event.setShows(new java.util.ArrayList<>(List.of(show)));
        event.getOrganization().setUserOrganizations(List.of(sampleUserOrganization(42L, OrganizationRole.OWNER)));
        when(eventRepository.findByIdAndStatusIs(7L, EventStatus.PUBLISHED)).thenReturn(Optional.of(event));
        when(orderRepository.findAllByItemsTicketEventShowEventIdAndStatusIs(7L, OrderStatus.FULFILLED))
                .thenReturn(List.of(Order.builder()
                        .id(1L)
                        .items(new java.util.ArrayList<>(List.of(TicketItem.builder().placeTotal(100.0).build())))
                        .build()));
        when(currencyConverterService.convertVndToSgd(100.0)).thenReturn(50.0);
        when(payPalService.sendPayout(eq("owner@paypal.example"), eq(50.0), eq("SGD"), any()))
                .thenReturn(true);

        var result = eventService.eventPayout(42L, 7L);

        assertThat(result).isTrue();
        assertThat(event.getPayoutAt()).isNotNull();
        verify(eventRepository).save(event);
    }

    @Test
    void isMember_shouldReturnTrueForMatchingRole() {
        var event = sampleEvent(EventStatus.PUBLISHED);
        event.getOrganization().setUserOrganizations(List.of(sampleUserOrganization(42L, OrganizationRole.OWNER)));
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));

        assertThat(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER, OrganizationRole.MANAGER))).isTrue();
    }

    @Test
    void isMember_shouldReturnFalseForNonMatchingRole() {
        var event = sampleEvent(EventStatus.PUBLISHED);
        event.getOrganization().setUserOrganizations(List.of(sampleUserOrganization(99L, OrganizationRole.MANAGER)));
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));

        assertThat(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).isFalse();
    }

    private CreateEventDto sampleCreateEventDto() {
        var ticket = CreateEventDto.CreateShowDto.CreateTicketTypeDto.builder()
                .seatmapBlockId("A1")
                .name("Standard")
                .description("General admission")
                .price(100.0)
                .initialStock(10)
                .build();

        var show = CreateEventDto.CreateShowDto.builder()
                .title("Opening Night")
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
                .description("Music and food")
                .address("123 Street")
                .placeName("Main Hall")
                .backgroundBase64("background-base64")
                .logoBase64("logo-base64")
                .categoryIds(List.of(5L))
                .keywords(List.of("existing", "new-tag"))
                .showInputs(List.of(show))
                .build();
    }

    private UpdateEventDto sampleUpdateEventDto() {
        var ticket = CreateEventDto.CreateShowDto.CreateTicketTypeDto.builder()
                .seatmapBlockId("B1")
                .name("VIP")
                .description("Front row")
                .price(200.0)
                .initialStock(5)
                .build();

        var show = CreateEventDto.CreateShowDto.builder()
                .title("Updated show")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .saleStartTime(LocalDateTime.now().minusHours(2))
                .saleEndTime(LocalDateTime.now().plusDays(2))
                .enabledSeatmap(false)
                .ticketTypeInputs(List.of(ticket))
                .build();

        return UpdateEventDto.builder()
                .title("Updated title")
                .description("Updated description")
                .address("456 Avenue")
                .placeName("Updated Hall")
                .categoryIds(List.of(5L))
                .keywords(List.of("updated"))
                .showInputs(List.of(show))
                .build();
    }

    private Event sampleEvent(EventStatus status) {
        return Event.builder()
                .id(7L)
                .status(status)
                .title("Summer Fest")
                .description("Music and food")
                .address("123 Street")
                .placeName("Main Hall")
                .organization(sampleOrganization(42L))
                .shows(new java.util.ArrayList<>())
                .assets(new HashSet<>())
                .categories(new HashSet<>())
                .keywords(new HashSet<>())
                .build();
    }

    private Organization sampleOrganization(Long ownerUserId) {
        var organization = Organization.builder()
                .id(9L)
                .name("Eventbox")
                .paypalAccount("owner@paypal.example")
                .description("Org")
                .userOrganizations(new java.util.ArrayList<>())
                .build();
        organization.getUserOrganizations().add(sampleUserOrganization(ownerUserId, OrganizationRole.OWNER));
        return organization;
    }

    private UserOrganization sampleUserOrganization(Long userId, OrganizationRole role) {
        return UserOrganization.builder()
                .user(User.builder().id(userId).build())
                .organization(Organization.builder().id(9L).build())
                .role(role)
                .build();
    }

    private EventShow sampleShow() {
        var ticket = Ticket.builder().id(88L).stock(10).build();
        return EventShow.builder()
                .id(11L)
                .title("Updated show")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .saleStartTime(LocalDateTime.now().minusHours(1))
                .saleEndTime(LocalDateTime.now().plusDays(1))
                .tickets(new java.util.ArrayList<>(List.of(ticket)))
                .build();
    }

    private Map<String, Object> sampleUploadResult(String suffix) {
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("resource_type", "image");
        uploadResult.put("public_id", suffix + "-public");
        uploadResult.put("signature", suffix + "-signature");
        uploadResult.put("asset_id", suffix + "-asset");
        uploadResult.put("url", "https://example.com/" + suffix);
        uploadResult.put("secure_url", "https://example.com/" + suffix + "?secure=1");
        uploadResult.put("folder", "event-assets");
        uploadResult.put("format", "png");
        uploadResult.put("width", 100);
        uploadResult.put("height", 100);
        uploadResult.put("bytes", 1234);
        uploadResult.put("etag", suffix + "-etag");
        return uploadResult;
    }
}
