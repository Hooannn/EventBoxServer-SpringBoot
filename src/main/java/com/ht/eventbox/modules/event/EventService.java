package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.category.CategoryRepository;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
import com.ht.eventbox.modules.keyword.KeywordRepository;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.storage.CloudinaryService;
import com.ht.eventbox.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryRepository categoryRepository;
    private final KeywordRepository keywordRepository;

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
                    Constant.StorageFolder.ORGANIZATION_ASSETS
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
                    Constant.StorageFolder.ORGANIZATION_ASSETS
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

    public List<Event> getByOrganizationId(Long organizationId) {
        return eventRepository.findAllByOrganizationId(organizationId);
    }
}
