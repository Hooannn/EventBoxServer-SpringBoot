package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.event.dtos.ApplyVoucherDto;
import com.ht.eventbox.modules.event.dtos.CreateVoucherDto;
import com.ht.eventbox.modules.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(VoucherService.class);

    private final EventService eventService;
    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;

    public List<Voucher> getAllByEventId(Long userId, Long eventId) {
        // Mọi thành viên của tổ chức đều có thể xem voucher
        boolean isMember = eventService.isMember(userId, eventId, List.of(OrganizationRole.MANAGER, OrganizationRole.STAFF, OrganizationRole.OWNER));

        if (!isMember) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        return voucherRepository.findAllByEventIdOrderByIdAsc(eventId);
    }

    public List<Voucher> getAllPublicByEventId(Long eventId) {
        var now = LocalDateTime.now();
        return voucherRepository.findAllByEventIdAndIsPublicTrueAndIsActiveTrueAndValidFromIsLessThanEqualAndValidToIsGreaterThanEqualOrderByIdAsc(eventId, now, now);
    }

    public boolean createByEventId(Long userId, Long eventId, CreateVoucherDto createVoucherDto) {
        // Chỉ có OWNER tổ chức mới có thể tạo voucher
        boolean isMember = eventService.isMember(userId, eventId, List.of(OrganizationRole.OWNER));

        if (!isMember) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        String voucherCode = createVoucherDto.getCode().trim().toUpperCase();

        if (voucherRepository.existsByCodeAndEventId(voucherCode, eventId)) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        var voucher = Voucher.builder()
                .code(voucherCode)
                .isPublic(createVoucherDto.isPublic())
                .name(createVoucherDto.getName())
                .description(createVoucherDto.getDescription())
                .discountType(createVoucherDto.getDiscountType())
                .discountValue(createVoucherDto.getDiscountValue())
                .validFrom(createVoucherDto.getValidFrom())
                .validTo(createVoucherDto.getValidTo())
                .minTicketQuantity(createVoucherDto.getMinTicketQuantity())
                .minOrderValue(createVoucherDto.getMinOrderValue())
                .usageLimit(createVoucherDto.getUsageLimit())
                .perUserLimit(createVoucherDto.getPerUserLimit())
                .isActive(createVoucherDto.isActive())
                .event(eventService.getById(eventId))
                .build();

        voucherRepository.save(voucher);

        return true;
    }

    public boolean updateByEventId(Long userId, Long id, Long eventId, CreateVoucherDto createVoucherDto) {
        // Chỉ có OWNER tổ chức mới có thể cập nhật voucher
        boolean isMember = eventService.isMember(userId, eventId, List.of(OrganizationRole.OWNER));

        if (!isMember) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        String voucherCode = createVoucherDto.getCode().trim().toUpperCase();

        var voucher = voucherRepository.findByIdAndEventId(id, eventId).orElseThrow(() -> new HttpException(Constant.ErrorCode.VOUCHER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (orderRepository.existsByVoucherId(voucher.getId())) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_HAS_BEEN_USED, HttpStatus.BAD_REQUEST);
        }

        if (!voucher.getCode().equals(voucherCode) && voucherRepository.existsByCodeAndEventId(voucherCode, eventId)) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        voucher.setCode(voucherCode);
        voucher.setName(createVoucherDto.getName());
        voucher.setDescription(createVoucherDto.getDescription());
        voucher.setDiscountType(createVoucherDto.getDiscountType());
        voucher.setDiscountValue(createVoucherDto.getDiscountValue());
        voucher.setValidFrom(createVoucherDto.getValidFrom());
        voucher.setValidTo(createVoucherDto.getValidTo());
        voucher.setUsageLimit(createVoucherDto.getUsageLimit());
        voucher.setPerUserLimit(createVoucherDto.getPerUserLimit());
        voucher.setActive(createVoucherDto.isActive());
        voucher.setPublic(createVoucherDto.isPublic());
        voucher.setMinOrderValue(createVoucherDto.getMinOrderValue());
        voucher.setMinTicketQuantity(createVoucherDto.getMinTicketQuantity());

        voucherRepository.save(voucher);

        return true;
    }

    public boolean deleteByEventId(Long userId, Long id, Long eventId) {
        // Chỉ có OWNER tổ chức mới có thể xóa voucher
        boolean isMember = eventService.isMember(userId, eventId, List.of(OrganizationRole.OWNER));

        if (!isMember) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        var voucher = voucherRepository.findByIdAndEventId(id, eventId).orElseThrow(() -> new HttpException(Constant.ErrorCode.VOUCHER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (orderRepository.existsByVoucherId(voucher.getId())) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_HAS_BEEN_USED, HttpStatus.BAD_REQUEST);
        }

        voucherRepository.delete(voucher);
        return true;
    }

    public long getUsage(Long userId, Long id, Long eventId) {
        boolean isMember = eventService.isMember(userId, eventId, List.of(OrganizationRole.MANAGER, OrganizationRole.STAFF, OrganizationRole.OWNER));

        if (!isMember) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        return orderRepository.countByVoucherIdAndStatusIs(id, OrderStatus.FULFILLED);
    }

    public Voucher getByOrderId(Long userId, Long orderId) {
        return voucherRepository.findByUserIdAndOrderId(userId, orderId).orElse(null);
    }

    @Transactional
    public boolean applyByOrderId(Long userId, Long orderId, ApplyVoucherDto applyVoucherDto) {
        var order = orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                orderId,
                userId,
                List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING),
                LocalDateTime.now()
        ).orElseThrow(() -> new HttpException(
                Constant.ErrorCode.ORDER_NOT_FOUND,
                HttpStatus.NOT_FOUND
        ));

        var voucher = voucherRepository.findByCodeIgnoreCaseAndEventIdAndIsActiveTrue(applyVoucherDto.getCode().toUpperCase(), order.getItems().get(0).getTicket().getEventShow().getEvent().getId())
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.VOUCHER_NOT_FOUND, HttpStatus.NOT_FOUND));

        // điều kiện thời gian
        if (voucher.getValidFrom().isAfter(LocalDateTime.now()) || voucher.getValidTo().isBefore(LocalDateTime.now())) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_TIME_NOT_VALID, HttpStatus.BAD_REQUEST);
        }

        // điều kiện số lượng vé tối thiểu và giá trị đơn hàng tối thiểu
        if (order.getPlaceTotal().compareTo(voucher.getMinOrderValue()) < 0 || order.getItems().size() < voucher.getMinTicketQuantity()) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_CONDITION_NOT_MET, HttpStatus.BAD_REQUEST);
        }

        // số lượt sử dụng
        long totalUsageCount = orderRepository.countByVoucherId(voucher.getId());
        if (totalUsageCount >= voucher.getUsageLimit()) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_USAGE_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST);
        }

        // số lượt sử dụng trên mỗi người dùng
        long userUsageCount = orderRepository.countByUserIdAndVoucherId(userId, voucher.getId());
        if (userUsageCount >= voucher.getPerUserLimit()) {
            throw new HttpException(Constant.ErrorCode.VOUCHER_PER_USER_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST);
        }

        order.setVoucher(voucher);
        orderRepository.save(order);

        return true;
    }

    public boolean removeByOrderId(Long userId, Long orderId) {
        var order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> new HttpException(
                Constant.ErrorCode.ORDER_NOT_FOUND,
                HttpStatus.NOT_FOUND
        ));

        if (order.getStatus().equals(OrderStatus.FULFILLED)) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        order.setVoucher(null);
        orderRepository.save(order);

        return true;
    }
}
