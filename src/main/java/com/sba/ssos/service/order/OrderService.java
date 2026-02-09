package com.sba.ssos.service.order;


import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderExpiredRequest;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.dto.request.order.OrderItemRequest;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.dto.response.order.OrderHistoryResponse;
import com.sba.ssos.dto.response.order.OrderPaid;
import com.sba.ssos.entity.*;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.enums.PaymentStatus;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CartItemRepository;
import com.sba.ssos.repository.OrderRepository;
import com.sba.ssos.repository.PaymentRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.service.customer.CustomerService;
import com.sba.ssos.service.product.shoevariant.ShoeVariantService;
import com.sba.ssos.utils.OrderCodeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ShoeVariantService shoeVariantService;
    private final ShoeVariantRepository shoeVariantRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationProperties applicationProperties;
    private final CartItemRepository cartItemRepository;
    private final CustomerService customerService;
    private SimpMessagingTemplate messagingTemplate;
    private static final long PAYMENT_EXPIRE_MINUTES = 5;
    private static final String ORDER_CODE_PREFIX = "SSOS";

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest){
        List<OrderItemRequest> requestItems = orderCreateRequest.items();

        Customer customer = customerService.getCurrentCustomer();
        List<CartItem> cartItems = customer.getCartItems();

        validateItems(requestItems, cartItems);

        Double totalAmount = calculatePrice(requestItems);

        Order order = new Order();
        order.setNotes(orderCreateRequest.notes());
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
            // trừ stock
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
                payment.getExpiredAt()

        );

    }

    @Transactional
    public void handlePaymentTimeout(OrderExpiredRequest  orderExpiredRequest) {

        Order order = findOrderById(orderExpiredRequest.orderId());
        order.setOrderStatus(OrderStatus.PAYMENT_EXPIRED);
        orderRepository.saveAndFlush(order);

        List<Payment> payments = order.getPayments();
        for(Payment payment : payments){
            payment.setPaymentStatus(PaymentStatus.TIME_OUT);
        }
        paymentRepository.saveAllAndFlush(payments);

        List<CartItem> cartItems = order.getCustomer().getCartItems();
        for(CartItem cartItem : cartItems){
            cartItem.setActive(true);
        }
        cartItemRepository.saveAllAndFlush(cartItems);

    }


    public List<OrderHistoryResponse> getOrderHistoryByCustomer(OrderHistoryRequest orderHistoryRequest) {

        Customer customer = customerService.getCurrentCustomer();

        Pageable pageable = PageRequest.of(
                orderHistoryRequest.page(),
                orderHistoryRequest.size()
        );

        Page<Order> orderPage = orderRepository.findByCustomer_Id(customer.getId(), pageable);

        if(!orderPage.hasContent()){
            throw  new BadRequestException("No order by you");
        }

        return orderPage.getContent().stream().map(order -> {
            return new OrderHistoryResponse(
                    order.getId(),
                    order.getOrderCode(),
                    order.getCreatedAt(),
                    order.getOrderStatus().toString(),
                    order.getPayments().getFirst().getPaymentStatus(),
                    "ONLINE",
                    orderPage.getContent().size(),
                    order.getTotalAmount()
            );
        }).toList();

    }

    public void verifyPayment( Map<String, Object> payload) {
        String regex = "SSOS-\\d{8}-[A-Z0-9]{6}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher((String) payload.get("content"));

        String orderCode = null;
        if (matcher.find()) {
            orderCode = matcher.group();
        } else {
            throw new NotFoundException("Not Found order code");
        }

        Order order = getOrderByCode(orderCode);
        Double totalPaid = order.getPayments().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                .map(Payment::getAmountReceived)
                .reduce(0.0, Double::sum);
        Double orderTotalAmount = order.getTotalAmount();
        if (totalPaid.compareTo(orderTotalAmount) >= 0) {
            order.setOrderStatus(OrderStatus.PAID); // hoặc COMPLETED
            orderRepository.save(order);

            messagingTemplate.convertAndSendToUser(
                    order.getCustomer().getId().toString(),
                    "/queue/orders",
                    new OrderPaid(order.getOrderCode(), order.getOrderStatus())
            );

//            client.subscribe('/user/queue/orders', (message) => {
//              const data = JSON.parse(message.body);
//
//              console.log('Order event:', data);
//
//              if (data.status === 'PAID') {
//                // check orderCode trong payload để map UI
//                showSuccess(`Đơn ${data.orderCode} đã thanh toán thành công`);
//            }
//});

        } else {
            throw new BadRequestException("Not enough payment amount");
        }
    }

    public Order findOrderById(UUID orderId){
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }



    private void validateItems(List<OrderItemRequest> items, List<CartItem> cartItems) {
        // 1. Check size
        if (cartItems.size() != items.size()) {
            throw new BadRequestException("Items in your cart do not match");
        }

        // 2. Build map from request: shoeVariantId -> quantity
        Map<UUID, Long> requestItemMap = items.stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::shoeVariantId,
                        OrderItemRequest::quantity
                ));

        // 3. Validate cart items match request + active
        for (CartItem cartItem : cartItems) {
            if (!cartItem.isActive()) {
                throw new BadRequestException("Inactive item in cart");
            }

            UUID variantId = cartItem.getShoeVariant().getId();

            if (!requestItemMap.containsKey(variantId)) {
                throw new BadRequestException("Items in your cart do not match");
            }

            cartItem.setActive(false);
        }

        cartItemRepository.saveAllAndFlush(cartItems);

        // 4. Validate stock
        for (OrderItemRequest item : items) {
            ShoeVariant shoeVariant = shoeVariantService.findById(item.shoeVariantId());

            if (shoeVariant.getQuantity() < item.quantity()) {
                throw new BadRequestException(
                        "Out of stock: " + item.shoeVariantId()
                );
            }
        }
    }

//    private void deactiveCartitems()


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
        return orderRepository.findByOrderCode(orderCode).orElseThrow(() -> new NotFoundException("Order not found"));
    }

}
