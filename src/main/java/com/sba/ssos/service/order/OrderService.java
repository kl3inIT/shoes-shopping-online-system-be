package com.sba.ssos.service.order;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderExpiredRequest;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.dto.request.order.OrderItemRequest;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.dto.response.order.OrderHistoryResponse;
import com.sba.ssos.dto.response.order.OrderPaid;
import com.sba.ssos.dto.response.order.sepay.SePayWebhookRequest;
import com.sba.ssos.entity.CartItem;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.Order;
import com.sba.ssos.entity.OrderDetail;
import com.sba.ssos.entity.Payment;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.enums.PaymentMethod;
import com.sba.ssos.enums.PaymentStatus;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CartItemRepository;
import com.sba.ssos.repository.PaymentRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.repository.order.OrderRepository;
import com.sba.ssos.service.customer.CustomerService;
import com.sba.ssos.service.product.shoevariant.ShoeVariantService;
import com.sba.ssos.utils.OrderCodeUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final JPAQueryFactory queryFactory;
    private final ShoeVariantService shoeVariantService;
    private final ShoeVariantRepository shoeVariantRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationProperties applicationProperties;
    private final CartItemRepository cartItemRepository;
    private final CustomerService customerService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final long PAYMENT_EXPIRE_MINUTES = 5;
    private static final String ORDER_CODE_PREFIX = "SSOS";

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest) {
        List<OrderItemRequest> requestItems = orderCreateRequest.items();

        Customer customer = customerService.getCurrentCustomer();
        List<CartItem> cartItems = customer.getCartItems();

        validateItems(requestItems, cartItems);

        Double totalAmount = calculatePrice(requestItems);

        Order order = new Order();
        order.setNotes(StringUtils.defaultString(orderCreateRequest.notes()));
        order.setShippingAddress(orderCreateRequest.shippingAddress());
        order.setShippingName(orderCreateRequest.shippingName());
        order.setShippingPhone(orderCreateRequest.shippingPhone());
        order.setOrderCode(OrderCodeUtils.generate(ORDER_CODE_PREFIX));
        order.setCustomer(customer);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(totalAmount);

        List<OrderDetail> orderDetails = new ArrayList<>();
        for (OrderItemRequest item : requestItems) {
            ShoeVariant variant = shoeVariantService.findById(item.shoeVariantId());
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setShoeVariant(variant);
            detail.setQuantity(item.quantity());
            orderDetails.add(detail);

            variant.setQuantity(variant.getQuantity() - item.quantity());
            shoeVariantRepository.saveAndFlush(variant);
        }

        order.setOrderDetails(orderDetails);
        orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTotalAmount(totalAmount);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setExpiredAt(LocalDateTime.now().plusMinutes(PAYMENT_EXPIRE_MINUTES));
        paymentRepository.save(payment);

        return new OrderCreateResponse(
                order.getId(),
                order.getOrderCode(),
                applicationProperties.bankProperties().bankNumber(),
                applicationProperties.bankProperties().bankCode(),
                applicationProperties.bankProperties().accountHolder(),
                order.getTotalAmount(),
                order.getOrderStatus().toString(),
                payment.getExpiredAt());
    }

    @Transactional
    public void handlePaymentTimeout(OrderExpiredRequest orderExpiredRequest) {
        Order order = findOrderById(orderExpiredRequest.orderId());
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Order is not waiting for payment");
        }

        order.setOrderStatus(OrderStatus.PAYMENT_EXPIRED);
        orderRepository.saveAndFlush(order);

        List<Payment> payments = order.getPayments();
        for (Payment payment : payments) {
            payment.setPaymentStatus(PaymentStatus.TIME_OUT);
        }
        paymentRepository.saveAllAndFlush(payments);

        for (OrderDetail orderDetail : order.getOrderDetails()) {
            ShoeVariant variant = orderDetail.getShoeVariant();
            variant.setQuantity(variant.getQuantity() + orderDetail.getQuantity());
        }
        shoeVariantRepository.saveAllAndFlush(
                order.getOrderDetails().stream().map(OrderDetail::getShoeVariant).toList());

        Set<UUID> orderedVariantIds = order.getOrderDetails().stream()
                .map(OrderDetail::getShoeVariant)
                .map(ShoeVariant::getId)
                .collect(Collectors.toSet());

        List<CartItem> cartItems = order.getCustomer().getCartItems().stream()
                .filter(cartItem -> orderedVariantIds.contains(cartItem.getShoeVariant().getId()))
                .toList();
        for (CartItem cartItem : cartItems) {
            cartItem.setActive(true);
        }
        cartItemRepository.saveAllAndFlush(cartItems);
    }

    public List<OrderHistoryResponse> getOrderHistoryByCustomer(OrderHistoryRequest orderHistoryRequest) {
        Customer customer = customerService.getCurrentCustomer();

        Pageable pageable = PageRequest.of(
                orderHistoryRequest.page(),
                orderHistoryRequest.size());

        Page<Order> orderPage = orderRepository.findByCustomer_Id(customer.getId(), pageable);
        if (!orderPage.hasContent()) {
            return List.of();
        }

        return orderPage.getContent().stream()
                .map(order -> new OrderHistoryResponse(
                        order.getId(),
                        order.getOrderCode(),
                        order.getCreatedAt(),
                        order.getCustomer().getUser().getFirstName() + " " + order.getCustomer().getUser().getLastName(),
                        order.getCustomer().getUser().getEmail(),
                        order.getOrderStatus().toString(),
                        order.getPayments().isEmpty()
                                ? PaymentStatus.PENDING
                                : order.getPayments().getFirst().getPaymentStatus(),
                        PaymentMethod.ONLINE,
                        order.getOrderDetails() == null ? 0L : order.getOrderDetails().stream()
                                .map(OrderDetail::getQuantity)
                                .filter(Objects::nonNull)
                                .reduce(0L, Long::sum),
                        order.getTotalAmount()))
                .toList();
    }

    public Page<OrderHistoryResponse> getOrderHistoryByAdmin(OrderHistoryRequest orderHistoryRequest) {
        Pageable pageable = PageRequest.of(
                orderHistoryRequest.page(),
                orderHistoryRequest.size() != 0 ? orderHistoryRequest.size() : 5);

        Page<Order> pageOrder = orderRepository.findOrderHistory(orderHistoryRequest, pageable);
        if (!pageOrder.hasContent()) {
            return Page.empty(pageable);
        }

        return pageOrder.map(orderItem -> new OrderHistoryResponse(
                orderItem.getId(),
                orderItem.getOrderCode(),
                orderItem.getCreatedAt(),
                orderItem.getCustomer().getUser().getFirstName() + " " + orderItem.getCustomer().getUser().getLastName(),
                orderItem.getCustomer().getUser().getEmail(),
                orderItem.getOrderStatus().toString(),
                orderItem.getPayments().isEmpty()
                        ? PaymentStatus.PENDING
                        : orderItem.getPayments().getFirst().getPaymentStatus(),
                PaymentMethod.ONLINE,
                orderItem.getOrderDetails() == null ? 0L : orderItem.getOrderDetails().stream()
                        .map(OrderDetail::getQuantity)
                        .filter(Objects::nonNull)
                        .reduce(0L, Long::sum),
                orderItem.getTotalAmount()));
    }

    public void verifyPayment(SePayWebhookRequest request) {
        String regex = "SSOS-\\d{8}-[A-Z0-9]{6}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(request.content());

        String orderCode = null;
        if (matcher.find()) {
            orderCode = matcher.group();
        } else {
            throw new NotFoundException("Not Found order code");
        }

        Order order = getOrderByCode(orderCode);
        if (order.getOrderStatus() == OrderStatus.PAYMENT_EXPIRED) {
            throw new BadRequestException("Order payment has expired");
        }
        if (order.getOrderStatus() == OrderStatus.PAID) {
            return;
        }

        Payment payment = order.getPayments().stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Order has no payment request"));

        double amountReceived = request.transferAmount().doubleValue();
        double currentAmountReceived = payment.getAmountReceived() == null ? 0.0 : payment.getAmountReceived();
        payment.setAmountReceived(currentAmountReceived + amountReceived);

        Double totalPaid = order.getPayments().stream()
                .map(Payment::getAmountReceived)
                .filter(Objects::nonNull)
                .reduce(0.0, Double::sum);

        Double orderTotalAmount = order.getTotalAmount();
        if (totalPaid.compareTo(orderTotalAmount) >= 0) {
            payment.setPaymentStatus(PaymentStatus.PAID);
            order.setOrderStatus(OrderStatus.PAID);
            orderRepository.save(order);
            paymentRepository.saveAllAndFlush(order.getPayments());

            messagingTemplate.convertAndSendToUser(
                    order.getCustomer().getId().toString(),
                    "/queue/orders",
                    new OrderPaid(order.getOrderCode(), order.getOrderStatus()));
        }
    }

    public Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private void validateItems(List<OrderItemRequest> items, List<CartItem> cartItems) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Order items are required");
        }

        if (cartItems.size() != items.size()) {
            throw new BadRequestException("Items in your cart do not match");
        }

        Set<UUID> duplicatedVariantIds = new HashSet<>();
        Set<UUID> seenVariantIds = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!seenVariantIds.add(item.shoeVariantId())) {
                duplicatedVariantIds.add(item.shoeVariantId());
            }
        }
        if (!duplicatedVariantIds.isEmpty()) {
            throw new BadRequestException("Duplicate shoe variant in order items");
        }

        Map<UUID, Long> requestItemMap = items.stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::shoeVariantId,
                        OrderItemRequest::quantity));

        for (CartItem cartItem : cartItems) {
            if (!cartItem.isActive()) {
                throw new BadRequestException("Inactive item in cart");
            }

            UUID variantId = cartItem.getShoeVariant().getId();
            if (!requestItemMap.containsKey(variantId)) {
                throw new BadRequestException("Items in your cart do not match");
            }
            if (!Objects.equals(requestItemMap.get(variantId), cartItem.getQuantity())) {
                throw new BadRequestException("Items in your cart do not match");
            }

            cartItem.setActive(false);
        }

        cartItemRepository.saveAllAndFlush(cartItems);

        for (OrderItemRequest item : items) {
            ShoeVariant shoeVariant = shoeVariantService.findById(item.shoeVariantId());
            if (shoeVariant.getQuantity() < item.quantity()) {
                throw new BadRequestException("Out of stock: " + item.shoeVariantId());
            }
        }
    }

    private Double calculatePrice(List<OrderItemRequest> items) {
        List<UUID> ids = items.stream()
                .map(OrderItemRequest::shoeVariantId)
                .toList();

        Map<UUID, ShoeVariant> variantMap =
                shoeVariantRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(ShoeVariant::getId, v -> v));

        double totalPrice = 0;
        for (OrderItemRequest item : items) {
            ShoeVariant variant = variantMap.get(item.shoeVariantId());
            if (variant == null) {
                throw new NotFoundException("ShoeVariant not found " + item.shoeVariantId());
            }
            totalPrice += item.quantity() * variant.getShoe().getPrice();
        }

        return totalPrice;
    }

    public Order getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }
}
