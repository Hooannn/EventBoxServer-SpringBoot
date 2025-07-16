package com.ht.eventbox.modules.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.messaging.Notification;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.category.CategoryRepository;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventDto;
import com.ht.eventbox.modules.keyword.KeywordRepository;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.storage.CloudinaryService;
import com.ht.eventbox.utils.Helper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    @Getter
    @Setter
    @Builder
    public static class DiscoveryEvents {
        @JsonProperty("featured_events")
        private List<Event> featuredEvents;

        @JsonProperty("trending_events")
        private List<Event> trendingEvents;

        @JsonProperty("latest_events")
        private List<Event> latestEvents;
    }

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryRepository categoryRepository;
    private final KeywordRepository keywordRepository;
    private final AssetRepository assetRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PushNotificationService pushNotificationService;

    public DiscoveryEvents getDiscovery() {
        Pageable pageable = PageRequest.of(0, 4, Sort.by("id").ascending());
        var featuredEvents = eventRepository.findByStatusIn(
                List.of(EventStatus.PUBLISHED), pageable
        ).getContent();

        // Mocking events
        return DiscoveryEvents.builder()
                .featuredEvents(featuredEvents)
                .trendingEvents(featuredEvents)
                .latestEvents(featuredEvents)
                .build();
    }

    @Transactional
    public boolean create(Long userId, CreateEventDto createEventDto) {
        var org = organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(createEventDto.getOrganizationId(), userId, OrganizationRole.OWNER).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );

        var event = Event.builder().organization(org)
                .status(EventStatus.PENDING)
                .title(createEventDto.getTitle())
                .description(createEventDto.getDescription())
                .address(createEventDto.getAddress())
                .placeName(createEventDto.getPlaceName())
                .assets(new HashSet<>())
                .categories(new HashSet<>(categoryRepository.findAllById(createEventDto.getCategoryIds())))
                .build();

        List<EventShow> eventShows = createEventDto.getShowInputs().stream()
                .map(createShowDto -> {
                    var eventShow = EventShow.builder()
                            .event(event)
                            .startTime(createShowDto.getStartTime())
                            .endTime(createShowDto.getEndTime())
                            .saleStartTime(createShowDto.getSaleStartTime())
                            .saleEndTime(createShowDto.getSaleEndTime())
                            .build();

                    List<Ticket> tickets = createShowDto.getTicketTypeInputs().stream()
                            .map(ticketTypeDto -> {
                                var ticket = Ticket.builder()
                                        .eventShow(eventShow)
                                        .available(true)
                                        .name(ticketTypeDto.getName())
                                        .description(ticketTypeDto.getDescription())
                                        .price(ticketTypeDto.getPrice())
                                        .initialStock(ticketTypeDto.getInitialStock())
                                        .stock(ticketTypeDto.getInitialStock())
                                        .build();
                                return ticket;
                            })
                            .toList();

                    eventShow.setTickets(tickets);
                    return eventShow;
                })
                .toList();

        event.setShows(eventShows);

        if (createEventDto.getKeywords() != null && !createEventDto.getKeywords().isEmpty()) {
            Set<Keyword> keywords = createEventDto.getKeywords().stream()
                    .map(name -> keywordRepository.findById(name)
                            .orElseGet(() -> keywordRepository.save(Keyword.builder().name(name).build())))
                    .collect(Collectors.toSet());
            event.setKeywords(keywords);
        }

        Map logoUploadResult = null;
        try {
            logoUploadResult = cloudinaryService.uploadByBase64(
                    createEventDto.getLogoBase64(),
                    Constant.StorageFolder.EVENT_ASSETS
            );
            logger.info("Uploaded image: {}", logoUploadResult);
        } catch (IOException e) {
            throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (logoUploadResult == null)
            throw new HttpException(Constant.ErrorCode.CLOUDINARY_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);

        var logoAsset = Helper.getAssetFromUploadResult(logoUploadResult, AssetUsage.EVENT_LOGO);

        Map backgroundUploadResult = null;
        try {
            backgroundUploadResult = cloudinaryService.uploadByBase64(
                    createEventDto.getBackgroundBase64(),
                    Constant.StorageFolder.EVENT_ASSETS
            );
            logger.info("Uploaded image: {}", backgroundUploadResult);
        } catch (IOException e) {
            throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (backgroundUploadResult == null)
            throw new HttpException(Constant.ErrorCode.CLOUDINARY_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);

        var backgroundAsset = Helper.getAssetFromUploadResult(backgroundUploadResult, AssetUsage.EVENT_BANNER);

        event.getAssets().add(logoAsset);
        event.getAssets().add(backgroundAsset);

        eventRepository.save(event);

        return true;
    }

    @Transactional
    public boolean update(Long userId, Long eventId, UpdateEventDto updateEventDto) {
        var event = eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(eventId, userId, OrganizationRole.OWNER)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (event.getStatus() != EventStatus.PENDING) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.BAD_REQUEST);
        }

        event.setTitle(updateEventDto.getTitle());
        event.setDescription(updateEventDto.getDescription());
        event.setAddress(updateEventDto.getAddress());
        event.setPlaceName(updateEventDto.getPlaceName());
        event.setCategories(new HashSet<>(categoryRepository.findAllById(updateEventDto.getCategoryIds())));

        List<EventShow> eventShows = updateEventDto.getShowInputs().stream()
                .map(createShowDto -> {
                    var eventShow = EventShow.builder()
                            .event(event)
                            .startTime(createShowDto.getStartTime())
                            .endTime(createShowDto.getEndTime())
                            .saleStartTime(createShowDto.getSaleStartTime())
                            .saleEndTime(createShowDto.getSaleEndTime())
                            .build();

                    List<Ticket> tickets = createShowDto.getTicketTypeInputs().stream()
                            .map(ticketTypeDto -> {
                                var ticket = Ticket.builder()
                                        .eventShow(eventShow)
                                        .available(true)
                                        .name(ticketTypeDto.getName())
                                        .description(ticketTypeDto.getDescription())
                                        .price(ticketTypeDto.getPrice())
                                        .initialStock(ticketTypeDto.getInitialStock())
                                        .stock(ticketTypeDto.getInitialStock())
                                        .build();
                                return ticket;
                            })
                            .toList();

                    eventShow.setTickets(tickets);
                    return eventShow;
                })
                .toList();

        event.getShows().clear();
        event.getShows().addAll(eventShows);

        if (updateEventDto.getKeywords() != null && !updateEventDto.getKeywords().isEmpty()) {
            Set<Keyword> keywords = updateEventDto.getKeywords().stream()
                    .map(name -> keywordRepository.findById(name)
                            .orElseGet(() -> keywordRepository.save(Keyword.builder().name(name).build())))
                    .collect(Collectors.toSet());
            event.getKeywords().clear();
            event.getKeywords().addAll(keywords);
        }

        Set<Asset> assetsToRemove = new HashSet<>();
        if (updateEventDto.getLogoBase64() != null && !updateEventDto.getLogoBase64().isEmpty()) {
            event.getAssets().stream().filter(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO).forEach(asset -> {
                try {
                    cloudinaryService.destroyByPublicId(asset.getPublicId(), asset.getResourceType());
                    logger.info("Deleted image: {}", asset.getPublicId());
                } catch (IOException e) {
                    throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
            assetsToRemove.addAll(event.getAssets().stream()
                    .filter(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO)
                    .collect(Collectors.toSet()));
            event.getAssets().removeIf(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO);

            Map logoUploadResult = null;
            try {
                logoUploadResult = cloudinaryService.uploadByBase64(
                        updateEventDto.getLogoBase64(),
                        Constant.StorageFolder.EVENT_ASSETS
                );
                logger.info("Uploaded image: {}", logoUploadResult);
            } catch (IOException e) {
                throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (logoUploadResult == null)
                throw new HttpException(Constant.ErrorCode.CLOUDINARY_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);

            var logoAsset = Helper.getAssetFromUploadResult(logoUploadResult, AssetUsage.EVENT_LOGO);
            event.getAssets().add(logoAsset);
        }

        if (updateEventDto.getBackgroundBase64() != null && !updateEventDto.getBackgroundBase64().isEmpty()) {
            event.getAssets().stream().filter(asset -> asset.getUsage() == AssetUsage.EVENT_BANNER).forEach(asset -> {
                try {
                    cloudinaryService.destroyByPublicId(asset.getPublicId(), asset.getResourceType());
                    logger.info("Deleted image: {}", asset.getPublicId());
                } catch (IOException e) {
                    throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
            assetsToRemove.addAll(event.getAssets().stream()
                    .filter(asset -> asset.getUsage() == AssetUsage.EVENT_BANNER)
                    .collect(Collectors.toSet()));
            event.getAssets().removeIf(asset -> asset.getUsage() == AssetUsage.EVENT_BANNER);

            Map uploadResult = null;
            try {
                uploadResult = cloudinaryService.uploadByBase64(
                        updateEventDto.getLogoBase64(),
                        Constant.StorageFolder.EVENT_ASSETS
                );
                logger.info("Uploaded image: {}", uploadResult);
            } catch (IOException e) {
                throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (uploadResult == null)
                throw new HttpException(Constant.ErrorCode.CLOUDINARY_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);

            var backgroundAsset = Helper.getAssetFromUploadResult(uploadResult, AssetUsage.EVENT_BANNER);
            event.getAssets().add(backgroundAsset);
        }

        eventRepository.save(event);
        if (!assetsToRemove.isEmpty()) {
            assetRepository.deleteAll(assetsToRemove);
        }
        return true;
    }

    public List<Event> getByOrganizationId(Long organizationId) {
        return eventRepository.findAllByOrganizationId(organizationId);
    }

    public List<Event> getAllByCategoriesId(Long categoryId) {
        Pageable pageable = PageRequest.of(0, 4, Sort.by("id").ascending());
        var events = eventRepository.findByCategoriesId(categoryId, pageable);
        return events.getContent();
    }

    public List<Event> getAllByCategoriesIdAndStatusIs(Long categoryId, EventStatus status) {
        Pageable pageable = PageRequest.of(0, 4, Sort.by("id").ascending());
        var events = eventRepository.findByCategoriesIdAndStatusIs(categoryId, status, pageable);
        return events.getContent();
    }

    public boolean publishByAdmin(Long eventId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (event.getStatus() != EventStatus.PENDING) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.BAD_REQUEST);
        }

        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);

        CompletableFuture.runAsync(() -> {
            String sql = "SELECT user_id FROM subscriptions WHERE organization_id = ?";
            List<Long> subscribers = jdbcTemplate.queryForList(sql, Long.class, event.getOrganization().getId());

            try {
                pushNotificationService.push(
                        subscribers,
                        Notification.builder()
                                .setBody(event.getTitle())
                                .setTitle("Sự kiện mới từ " + event.getOrganization().getName())
                                .setImage(event.getAssets().stream()
                                        .filter(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO)
                                        .findFirst()
                                        .map(Asset::getSecureUrl)
                                        .orElse(null))
                                .build(),
                        new HashMap<>(
                                Map.of(
                                        "type", "event",
                                        "event_id", String.valueOf(event.getId())
                                )
                        )
                );
            } catch (Exception e) {
                logger.error("Error sending push notification for event {}: {}", eventId, e.getMessage());
            }
        });

        return true;
    }

    public boolean archiveByAdmin(Long eventId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (event.getStatus() != EventStatus.PENDING) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.BAD_REQUEST);
        }

        event.setStatus(EventStatus.ARCHIVED);
        eventRepository.save(event);

        return true;
    }

    public boolean archive(Long userId, Long eventId) {
        var event = eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(eventId, userId, OrganizationRole.OWNER)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (event.getStatus() != EventStatus.PENDING) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.BAD_REQUEST);
        }

        event.setStatus(EventStatus.ARCHIVED);
        eventRepository.save(event);

        return true;
    }

    public boolean inactive(Long userId, Long eventId) {
        var event = eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(eventId, userId, OrganizationRole.OWNER)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.BAD_REQUEST);
        }

        event.setStatus(EventStatus.DRAFT);
        eventRepository.save(event);

        return true;
    }

    public boolean active(Long userId, Long eventId) {
        var event = eventRepository.findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(eventId, userId, OrganizationRole.OWNER)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.BAD_REQUEST);
        }

        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);

        return true;
    }

    public List<Event> getByOrganizationIdAndStatusIsNot(Long organizationId, EventStatus status) {
        return eventRepository.findAllByOrganizationIdAndStatusIsNotOrderByIdAsc(organizationId, status);
    }

    public Event getByIdAndStatusIsNot(Long eventId, EventStatus eventStatus) {
        return eventRepository.findByIdAndStatusIsNot(eventId, eventStatus)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    public List<Event> getAllByStatusIn(
            List<EventStatus> statuses
    ) {
        return eventRepository.findAllByStatusInOrderByIdAsc(statuses);
    }
}
