package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.enums.DiscountType;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.event.dtos.ApplyVoucherDto;
import com.ht.eventbox.modules.event.dtos.CreateVoucherDto;
import com.ht.eventbox.modules.order.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTests {

    @Mock
    private EventService eventService;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private VoucherService voucherService;

    @Test
    void getAllByEventId_shouldRejectNonMember() {
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.MANAGER, OrganizationRole.STAFF, OrganizationRole.OWNER)))
                .thenReturn(false);

        assertThatThrownBy(() -> voucherService.getAllByEventId(42L, 7L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                });

        verifyNoInteractions(voucherRepository);
    }

    @Test
    void getAllByEventId_shouldReturnVouchersForMember() {
        var voucher = sampleVoucher();
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.MANAGER, OrganizationRole.STAFF, OrganizationRole.OWNER)))
                .thenReturn(true);
        when(voucherRepository.findAllByEventIdOrderByIdAsc(7L)).thenReturn(List.of(voucher));

        var result = voucherService.getAllByEventId(42L, 7L);

        assertThat(result).containsExactly(voucher);
    }

    @Test
    void getAllPublicByEventId_shouldUseCurrentWindowQuery() {
        var voucher = sampleVoucher();
        when(voucherRepository.findAllByEventIdAndIsPublicTrueAndIsActiveTrueAndValidFromIsLessThanEqualAndValidToIsGreaterThanEqualOrderByIdAsc(
                eq(7L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(voucher));

        var result = voucherService.getAllPublicByEventId(7L);

        assertThat(result).containsExactly(voucher);
    }

    @Test
    void createByEventId_shouldRejectDuplicateCode() {
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).thenReturn(true);
        when(voucherRepository.existsByCodeAndEventId("SUMMER10", 7L)).thenReturn(true);

        assertThatThrownBy(() -> voucherService.createByEventId(42L, 7L, sampleCreateVoucherDto("summer10")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void createByEventId_shouldPersistUppercasedCode() {
        var dto = sampleCreateVoucherDto("summer10");
        var event = Event.builder().id(7L).build();
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).thenReturn(true);
        when(voucherRepository.existsByCodeAndEventId("SUMMER10", 7L)).thenReturn(false);
        when(eventService.getById(7L)).thenReturn(event);

        var captor = ArgumentCaptor.forClass(Voucher.class);

        var result = voucherService.createByEventId(42L, 7L, dto);

        assertThat(result).isTrue();
        verify(voucherRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SUMMER10");
        assertThat(captor.getValue().getEvent()).isSameAs(event);
    }

    @Test
    void updateByEventId_shouldRejectUsedVoucher() {
        var voucher = sampleVoucher();
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).thenReturn(true);
        when(voucherRepository.findByIdAndEventId(9L, 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.existsByVoucherId(9L)).thenReturn(true);

        assertThatThrownBy(() -> voucherService.updateByEventId(42L, 9L, 7L, sampleCreateVoucherDto("summer10")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_HAS_BEEN_USED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void updateByEventId_shouldRejectDuplicateReplacementCode() {
        var voucher = sampleVoucher();
        voucher.setCode("SUMMER10");
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).thenReturn(true);
        when(voucherRepository.findByIdAndEventId(9L, 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.existsByVoucherId(9L)).thenReturn(false);
        when(voucherRepository.existsByCodeAndEventId("WINTER10", 7L)).thenReturn(true);

        assertThatThrownBy(() -> voucherService.updateByEventId(42L, 9L, 7L, sampleCreateVoucherDto("winter10")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void updateByEventId_shouldPersistChanges() {
        var voucher = sampleVoucher();
        voucher.setCode("SUMMER10");
        var dto = sampleCreateVoucherDto("winter10");
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).thenReturn(true);
        when(voucherRepository.findByIdAndEventId(9L, 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.existsByVoucherId(9L)).thenReturn(false);

        var result = voucherService.updateByEventId(42L, 9L, 7L, dto);

        assertThat(result).isTrue();
        verify(voucherRepository).save(voucher);
        assertThat(voucher.getCode()).isEqualTo("WINTER10");
        assertThat(voucher.getName()).isEqualTo("Winter sale");
    }

    @Test
    void deleteByEventId_shouldRejectUsedVoucher() {
        var voucher = sampleVoucher();
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).thenReturn(true);
        when(voucherRepository.findByIdAndEventId(9L, 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.existsByVoucherId(9L)).thenReturn(true);

        assertThatThrownBy(() -> voucherService.deleteByEventId(42L, 9L, 7L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_HAS_BEEN_USED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void deleteByEventId_shouldDeleteWhenAllowed() {
        var voucher = sampleVoucher();
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.OWNER))).thenReturn(true);
        when(voucherRepository.findByIdAndEventId(9L, 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.existsByVoucherId(9L)).thenReturn(false);

        var result = voucherService.deleteByEventId(42L, 9L, 7L);

        assertThat(result).isTrue();
        verify(voucherRepository).delete(voucher);
    }

    @Test
    void getUsage_shouldCountFulfilledOrders() {
        when(eventService.isMember(42L, 7L, List.of(OrganizationRole.MANAGER, OrganizationRole.STAFF, OrganizationRole.OWNER)))
                .thenReturn(true);
        when(orderRepository.countByVoucherIdAndStatusIs(9L, OrderStatus.FULFILLED)).thenReturn(4L);

        var result = voucherService.getUsage(42L, 9L, 7L);

        assertThat(result).isEqualTo(4L);
    }

    @Test
    void getByOrderId_shouldReturnVoucherOrNull() {
        var voucher = sampleVoucher();
        when(voucherRepository.findByUserIdAndOrderId(42L, 100L)).thenReturn(Optional.of(voucher));

        var result = voucherService.getByOrderId(42L, 100L);

        assertThat(result).isSameAs(voucher);
    }

    @Test
    void applyByOrderId_shouldRejectMissingOrder() {
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(100L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any(LocalDateTime.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.applyByOrderId(42L, 100L, ApplyVoucherDto.builder().code("summer10").build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.ORDER_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void applyByOrderId_shouldRejectMissingVoucher() {
        var order = sampleOrder(100L, 42L, 7L, 100.0, 2);
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(100L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any(LocalDateTime.class))).thenReturn(Optional.of(order));
        when(voucherRepository.findByCodeIgnoreCaseAndEventIdAndIsActiveTrue("SUMMER10", 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.applyByOrderId(42L, 100L, ApplyVoucherDto.builder().code("summer10").build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void applyByOrderId_shouldRejectTimeWindowMismatch() {
        var order = sampleOrder(100L, 42L, 7L, 100.0, 2);
        var voucher = sampleVoucher();
        voucher.setValidFrom(LocalDateTime.now().plusHours(1));
        voucher.setValidTo(LocalDateTime.now().plusDays(1));
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(100L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any(LocalDateTime.class))).thenReturn(Optional.of(order));
        when(voucherRepository.findByCodeIgnoreCaseAndEventIdAndIsActiveTrue("SUMMER10", 7L)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.applyByOrderId(42L, 100L, ApplyVoucherDto.builder().code("summer10").build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_TIME_NOT_VALID);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void applyByOrderId_shouldRejectConditionMismatch() {
        var order = sampleOrder(100L, 42L, 7L, 50.0, 1);
        var voucher = sampleVoucher();
        voucher.setMinOrderValue(100.0);
        voucher.setMinTicketQuantity(2);
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(100L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any(LocalDateTime.class))).thenReturn(Optional.of(order));
        when(voucherRepository.findByCodeIgnoreCaseAndEventIdAndIsActiveTrue("SUMMER10", 7L)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.applyByOrderId(42L, 100L, ApplyVoucherDto.builder().code("summer10").build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_CONDITION_NOT_MET);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void applyByOrderId_shouldRejectUsageLimitExceeded() {
        var order = sampleOrder(100L, 42L, 7L, 150.0, 3);
        var voucher = sampleVoucher();
        voucher.setUsageLimit(1);
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(100L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any(LocalDateTime.class))).thenReturn(Optional.of(order));
        when(voucherRepository.findByCodeIgnoreCaseAndEventIdAndIsActiveTrue("SUMMER10", 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.countByVoucherId(9L)).thenReturn(1L);

        assertThatThrownBy(() -> voucherService.applyByOrderId(42L, 100L, ApplyVoucherDto.builder().code("summer10").build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_USAGE_LIMIT_EXCEEDED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void applyByOrderId_shouldRejectPerUserLimitExceeded() {
        var order = sampleOrder(100L, 42L, 7L, 150.0, 3);
        var voucher = sampleVoucher();
        voucher.setPerUserLimit(1);
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(100L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any(LocalDateTime.class))).thenReturn(Optional.of(order));
        when(voucherRepository.findByCodeIgnoreCaseAndEventIdAndIsActiveTrue("SUMMER10", 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.countByVoucherId(9L)).thenReturn(0L);
        when(orderRepository.countByUserIdAndVoucherId(42L, 9L)).thenReturn(1L);

        assertThatThrownBy(() -> voucherService.applyByOrderId(42L, 100L, ApplyVoucherDto.builder().code("summer10").build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.VOUCHER_PER_USER_LIMIT_EXCEEDED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void applyByOrderId_shouldPersistVoucherOnSuccess() {
        var order = sampleOrder(100L, 42L, 7L, 150.0, 3);
        var voucher = sampleVoucher();
        voucher.setUsageLimit(10);
        voucher.setPerUserLimit(5);
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(100L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any(LocalDateTime.class))).thenReturn(Optional.of(order));
        when(voucherRepository.findByCodeIgnoreCaseAndEventIdAndIsActiveTrue("SUMMER10", 7L)).thenReturn(Optional.of(voucher));
        when(orderRepository.countByVoucherId(9L)).thenReturn(0L);
        when(orderRepository.countByUserIdAndVoucherId(42L, 9L)).thenReturn(0L);

        var result = voucherService.applyByOrderId(42L, 100L, ApplyVoucherDto.builder().code("summer10").build());

        assertThat(result).isTrue();
        assertThat(order.getVoucher()).isSameAs(voucher);
        verify(orderRepository).save(order);
    }

    @Test
    void removeByOrderId_shouldRejectFulfilledOrder() {
        var order = sampleOrder(100L, 42L, 7L, 150.0, 3);
        order.setStatus(OrderStatus.FULFILLED);
        when(orderRepository.findByIdAndUserId(100L, 42L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> voucherService.removeByOrderId(42L, 100L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                });
    }

    @Test
    void removeByOrderId_shouldClearVoucherOnPendingOrder() {
        var order = sampleOrder(100L, 42L, 7L, 150.0, 3);
        order.setVoucher(sampleVoucher());
        when(orderRepository.findByIdAndUserId(100L, 42L)).thenReturn(Optional.of(order));

        var result = voucherService.removeByOrderId(42L, 100L);

        assertThat(result).isTrue();
        assertThat(order.getVoucher()).isNull();
        verify(orderRepository).save(order);
    }

    private CreateVoucherDto sampleCreateVoucherDto(String code) {
        return CreateVoucherDto.builder()
                .code(code)
                .name("Winter sale")
                .description("Discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .usageLimit(5)
                .perUserLimit(2)
                .isActive(true)
                .isPublic(true)
                .minOrderValue(50.0)
                .minTicketQuantity(1)
                .build();
    }

    private Voucher sampleVoucher() {
        return Voucher.builder()
                .id(9L)
                .code("SUMMER10")
                .name("Summer sale")
                .description("Discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .usageLimit(5)
                .perUserLimit(2)
                .isActive(true)
                .isPublic(true)
                .minOrderValue(50.0)
                .minTicketQuantity(1)
                .event(Event.builder().id(7L).build())
                .build();
    }

    private Order sampleOrder(Long orderId, Long userId, Long eventId, double placeTotal, int itemCount) {
        var event = Event.builder().id(eventId).build();
        var show = EventShow.builder().event(event).build();
        var ticket = Ticket.builder().eventShow(show).build();
        var items = new ArrayList<TicketItem>();
        for (int i = 0; i < itemCount; i++) {
            items.add(TicketItem.builder().ticket(ticket).placeTotal(placeTotal / itemCount).build());
        }

        return Order.builder()
                .id(orderId)
                .user(com.ht.eventbox.entities.User.builder().id(userId).build())
                .status(OrderStatus.WAITING_FOR_PAYMENT)
                .placeTotal(placeTotal)
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .items(items)
                .build();
    }
}
