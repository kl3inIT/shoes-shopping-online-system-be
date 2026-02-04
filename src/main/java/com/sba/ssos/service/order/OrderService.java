package com.sba.ssos.service.order;


import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderItemRequest;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.entity.*;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.enums.PaymentStatus;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.OrderMapper;
import com.sba.ssos.repository.*;
import com.sba.ssos.service.UserService;
import com.sba.ssos.service.cart.CartService;
import com.sba.ssos.service.shoevariants.ShoeVariantService;
import com.sba.ssos.utils.OrderCodeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ShoeVariantService shoeVariantService;
    private final ShoeVariantRepository shoeVariantRepository;
    private final UserService userService;
    private final CustomerRepository  customerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationProperties  applicationProperties;
    private final CartService cartService;
    private final CartItemRepository cartItemRepository;

    private static final long PAYMENT_EXPIRE_MINUTES = 5;
    private static final String ORDER_CODE_PREFIX = "SSOS";

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest){
        List<OrderItemRequest> requestItems = orderCreateRequest.items();

        Customer customer = customerRepository
                .findByUser_Id(userService.getCurrentUser().userId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        List<CartItem> cartItems = cartService.findByCustomerId(customer.getId()).getCartItems();

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
    public void handlePaymentTimeout(UUID orderId) {

        Order order = findOrderById(orderId);
        order.setOrderStatus(OrderStatus.PAYMENT_EXPIRED);
        orderRepository.saveAndFlush(order);

        List<Payment> payments = order.getPayments();


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

}
