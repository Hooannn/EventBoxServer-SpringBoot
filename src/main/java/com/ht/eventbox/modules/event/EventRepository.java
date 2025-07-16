package com.ht.eventbox.modules.event;

import com.ht.eventbox.entities.Event;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrganizationRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrganizationId(Long organizationId);

    Optional<Event> findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(Long id, Long userId, OrganizationRole organizationRole);

    List<Event> findAllByOrganizationIdAndStatusIsNot(Long orgId, EventStatus status);

    List<Event> findAllByOrganizationIdAndStatusIsNotOrderByIdAsc(Long orgId, EventStatus status);

    Optional<Event> findByIdAndStatusIsNot(Long eventId, EventStatus eventStatus);

    List<Event> findAllByStatusInOrderByIdAsc(Collection<EventStatus> status);

    Page<Event> findByStatusIn(Collection<EventStatus> status, Pageable pageable);

    Page<Event> findByCategoriesId(Long categoryId, Pageable pageable);

    Page<Event> findByCategoriesIdAndStatusIs(Long categoryId, EventStatus status, Pageable pageable);

    boolean existsByOrganizationId(Long orgId);
}
