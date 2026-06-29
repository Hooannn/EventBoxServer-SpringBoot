package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void searchAllByShowId_shouldMatchIdEmailAndUserName() {
        var aliceOrder = persistFulfilledOrder(
                "Alice",
                "Smith",
                "alice@example.com",
                "Alice Smith",
                "alice"
        );
        var bobOrder = persistFulfilledOrder(
                "Bob",
                "Jones",
                "bob@example.com",
                "Bob Jones",
                "bob"
        );

        var byName = orderRepository.searchAllByItemsTicketEventShowIdAndStatusIsOrderByIdAsc(
                aliceOrder.showId(),
                OrderStatus.FULFILLED,
                "alice smith",
                PageRequest.of(0, 10));
        assertThat(byName.getContent()).extracting(Order::getId).containsExactly(aliceOrder.order().getId());
        assertThat(byName.getTotalElements()).isEqualTo(1);

        var byEmail = orderRepository.searchAllByItemsTicketEventShowIdAndStatusIsOrderByIdAsc(
                bobOrder.showId(),
                OrderStatus.FULFILLED,
                "bob@example.com",
                PageRequest.of(0, 10));
        assertThat(byEmail.getContent()).extracting(Order::getId).containsExactly(bobOrder.order().getId());
        assertThat(byEmail.getTotalElements()).isEqualTo(1);

        var byId = orderRepository.searchAllByItemsTicketEventShowIdAndStatusIsOrderByIdAsc(
                bobOrder.showId(),
                OrderStatus.FULFILLED,
                String.valueOf(bobOrder.order().getId()),
                PageRequest.of(0, 10));
        assertThat(byId.getContent()).extracting(Order::getId).containsExactly(bobOrder.order().getId());
        assertThat(byId.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchAllByShowIdList_shouldReturnAllMatchingOrdersForExport() {
        var aliceOrder = persistFulfilledOrder(
                "Alice",
                "Smith",
                "alice@example.com",
                "Alice Smith",
                "alice"
        );

        var results = orderRepository.searchAllByItemsTicketEventShowIdAndStatusIsOrderByIdAsc(
                aliceOrder.showId(),
                OrderStatus.FULFILLED,
                "alice",
                PageRequest.of(0, 50));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getId()).isEqualTo(aliceOrder.order().getId());
    }

    private PersistedOrder persistFulfilledOrder(
            String firstName,
            String lastName,
            String email,
            String eventTitle,
            String ticketName
    ) {
        var user = entityManager.persistAndFlush(User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("secret")
                .build());

        var organization = entityManager.persistAndFlush(Organization.builder()
                .name("Org " + firstName)
                .paypalAccount(firstName.toLowerCase() + "@paypal.com")
                .description("Org description")
                .build());

        var event = entityManager.persistAndFlush(Event.builder()
                .organization(organization)
                .status(EventStatus.PUBLISHED)
                .title(eventTitle)
                .description("Event description")
                .address("Event address")
                .placeName("Event place")
                .build());

        var now = LocalDateTime.now();
        var show = entityManager.persistAndFlush(EventShow.builder()
                .event(event)
                .title(eventTitle + " show")
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(1).plusHours(2))
                .saleStartTime(now.minusDays(1))
                .saleEndTime(now.plusDays(1))
                .build());

        var ticket = entityManager.persistAndFlush(Ticket.builder()
                .eventShow(show)
                .name(ticketName)
                .price(50000.0)
                .initialStock(10)
                .stock(10)
                .available(true)
                .build());

        var order = Order.builder()
                .user(user)
                .status(OrderStatus.FULFILLED)
                .placeTotal(50000.0)
                .expiredAt(now.plusDays(1))
                .build();
        var ticketItem = TicketItem.builder()
                .order(order)
                .ticket(ticket)
                .placeTotal(50000.0)
                .build();
        order.setItems(new java.util.ArrayList<>(List.of(ticketItem)));

        order = entityManager.persistAndFlush(order);
        entityManager.clear();
        var savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        return new PersistedOrder(savedOrder, show.getId());
    }

    private record PersistedOrder(Order order, Long showId) {
    }
}
