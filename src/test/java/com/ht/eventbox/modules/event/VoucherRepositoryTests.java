package com.ht.eventbox.modules.event;

import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.enums.DiscountType;
import com.ht.eventbox.enums.EventStatus;
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
class VoucherRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VoucherRepository voucherRepository;

    @Test
    void searchAllByEventIdOrderByIdAsc_shouldMatchCodeIdAndName() {
        var event = persistEvent();
        var summerVoucher = persistVoucher(event, "SUMMER10", "Summer Sale");
        var winterVoucher = persistVoucher(event, "WINTER20", "Winter Deal");

        var byCode = voucherRepository.searchAllByEventIdOrderByIdAsc(event.getId(), "summer", PageRequest.of(0, 10));
        assertThat(byCode.getContent()).extracting(Voucher::getId).containsExactly(summerVoucher.getId());
        assertThat(byCode.getTotalElements()).isEqualTo(1);

        var byName = voucherRepository.searchAllByEventIdOrderByIdAsc(event.getId(), "winter deal", PageRequest.of(0, 10));
        assertThat(byName.getContent()).extracting(Voucher::getId).containsExactly(winterVoucher.getId());
        assertThat(byName.getTotalElements()).isEqualTo(1);

        var byId = voucherRepository.searchAllByEventIdOrderByIdAsc(event.getId(), String.valueOf(winterVoucher.getId()), PageRequest.of(0, 10));
        assertThat(byId.getContent()).extracting(Voucher::getId).containsExactly(winterVoucher.getId());
        assertThat(byId.getTotalElements()).isEqualTo(1);
    }

    private Event persistEvent() {
        var organization = entityManager.persistAndFlush(Organization.builder()
                .name("Org")
                .paypalAccount("org@paypal.com")
                .description("Org description")
                .build());

        var event = entityManager.persistAndFlush(Event.builder()
                .organization(organization)
                .status(EventStatus.PUBLISHED)
                .title("Event")
                .description("Event description")
                .address("Event address")
                .placeName("Event place")
                .build());

        return event;
    }

    private Voucher persistVoucher(Event event, String code, String name) {
        var now = LocalDateTime.now();
        return entityManager.persistAndFlush(Voucher.builder()
                .event(event)
                .code(code)
                .name(name)
                .description("Discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(1))
                .usageLimit(5)
                .perUserLimit(2)
                .isActive(true)
                .isPublic(true)
                .minOrderValue(50.0)
                .minTicketQuantity(1)
                .build());
    }
}
