package com.sba.ssos.service.order;


import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderExpiredRequest;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.dto.request.order.OrderItemRequest;
import com.sba.ssos.dto.response.order.CustomerOrderHistoryResponse;
import com.sba.ssos.dto.response.order.CustomerOrderItemResponse;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.dto.response.order.OrderHistoryResponse;
import com.sba.ssos.dto.response.order.OrderPaid;
import com.sba.ssos.dto.response.order.PaymentInfoResponse;
import com.sba.ssos.dto.response.order.sepay.SePayWebhookRequest;
import com.sba.ssos.entity.*;
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
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
import com.sba.ssos.service.product.shoevariant.ShoeVariantService;
import com.sba.ssos.utils.OrderCodeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final ShoeImageService shoeImageService;
    private static final long PAYMENT_EXPIRE_MINUTES = 5;
    private static final String ORDER_CODE_PREFIX = "SSOS";

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest){
        Customer customer = customerService.getCurrentCustomer();

        List<OrderItemRequest> requestItems = customer.getCartItems().stream().map(cartItem -> new OrderItemRequest(
                cartItem.getShoeVariant().getId(),
                cartItem.getQuantity()
        )).toList();


        validateItems(requestItems);

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
        payment.setPaymentMethod(PaymentMethod.ONLINE);
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


    public PaymentInfoResponse getPaymentInfo(UUID orderId) {
        Order order = getOrderById(orderId);
        return new PaymentInfoResponse(
                order.getId(),
                order.getOrderCode(),
                applicationProperties.bankProperties().bankNumber(),
                applicationProperties.bankProperties().bankCode(),
                applicationProperties.bankProperties().accountHolder(),
                order.getTotalAmount(),
                order.getOrderStatus().toString(),
                order.getPayments().getFirst().getExpiredAt()
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

    /**
     * Lấy danh sách đơn hàng theo customer đang đăng nhập, có filter theo status/ngày và trả về kèm items (chi tiết sản phẩm).
     */
    public Page<CustomerOrderHistoryResponse> getOrderHistoryByCustomer(OrderHistoryRequest orderHistoryRequest) {
        Customer customer = customerService.getCurrentCustomer();

        int page = orderHistoryRequest.page();
        int size = orderHistoryRequest.size() > 0 ? orderHistoryRequest.size() : 10;
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orderPage = orderRepository.findOrderHistoryByCustomer(
                customer.getId(),
                orderHistoryRequest,
                pageable
        );

        List<Order> content = orderPage.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<UUID> orderIds = content.stream().map(Order::getId).toList();
        List<Order> ordersWithDetails = orderRepository.findByIdInWithOrderDetails(orderIds);
        Map<UUID, Order> orderById = ordersWithDetails.stream().collect(Collectors.toMap(Order::getId, o -> o));

        List<CustomerOrderHistoryResponse> responses = content.stream()
                .map(order -> orderById.get(order.getId()))
                .filter(Objects::nonNull)
                .map(this::mapToCustomerOrderHistoryResponse)
                .toList();

        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    /**
     * Lấy chi tiết 1 đơn hàng cho customer hiện tại (bao gồm danh sách items).
     */
    public CustomerOrderHistoryResponse getOrderDetailByCustomer(UUID orderId) {
        Customer customer = customerService.getCurrentCustomer();
        Order order = getOrderById(orderId);

        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            throw new NotFoundException("Order not found");
        }

        return mapToCustomerOrderHistoryResponse(order);
    }

    private CustomerOrderHistoryResponse mapToCustomerOrderHistoryResponse(Order order) {
        List<CustomerOrderItemResponse> items = (order.getOrderDetails() == null ? List.<OrderDetail>of() : order.getOrderDetails())
                .stream()
                .map(this::mapToCustomerOrderItemResponse)
                .toList();

        return new CustomerOrderHistoryResponse(
                order.getId(),
                order.getOrderCode(),
                order.getOrderStatus().name(),
                order.getCreatedAt(),
                items,
                order.getTotalAmount()
        );
    }

    private CustomerOrderItemResponse mapToCustomerOrderItemResponse(OrderDetail detail) {
        ShoeVariant variant = detail.getShoeVariant();
        Shoe shoe = variant != null ? variant.getShoe() : null;
        String name = shoe != null ? shoe.getName() : "";
        Double price = shoe != null ? shoe.getPrice() : 0.0;
        String size = variant != null ? variant.getSize() : "";
        Long quantity = detail.getQuantity() != null ? detail.getQuantity() : 0L;

        String image = "";
        if (variant != null) {
            List<String> urls = shoeImageService.getVariantImageUrls(variant);
            image = urls.isEmpty() ? "" : urls.getFirst();
        } else if (shoe != null) {
            List<String> urls = shoeImageService.getShoeImageUrls(shoe, List.of());
            image = urls.isEmpty() ? "" : urls.getFirst();
        }

        return new CustomerOrderItemResponse(
                detail.getId(),
                name,
                image,
                price,
                size,
                quantity
        );
    }

    public Page<OrderHistoryResponse> getOrderHistoryByAdmin(OrderHistoryRequest orderHistoryRequest) {

        Pageable pageable = PageRequest.of(
                orderHistoryRequest.page(),
                orderHistoryRequest.size() != 0 ? orderHistoryRequest.size() : 5
        );

        Page<Order> orderPage = orderRepository.findOrderHistory(orderHistoryRequest, pageable);
        List<Order> content = orderPage.getContent();

        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<UUID> orderIds = content.stream().map(Order::getId).toList();
        List<Order> ordersWithDetails = orderRepository.findByIdInWithOrderDetails(orderIds);
        Map<UUID, Order> orderById = ordersWithDetails.stream().collect(Collectors.toMap(Order::getId, o -> o));

        List<OrderHistoryResponse> responses = content.stream()
                .map(order -> orderById.get(order.getId()))
                .filter(Objects::nonNull)
                .map(this::mapToAdminOrderHistoryResponse)
                .toList();

        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    private OrderHistoryResponse mapToAdminOrderHistoryResponse(Order order) {
        List<CustomerOrderItemResponse> items =
                (order.getOrderDetails() == null ? List.<OrderDetail>of() : order.getOrderDetails())
                        .stream()
                        .map(this::mapToCustomerOrderItemResponse)
                        .toList();

        Long itemCount = items.stream()
                .map(CustomerOrderItemResponse::quantity)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);

        return new OrderHistoryResponse(
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
                itemCount,
                order.getTotalAmount(),
                items
        );
    }

    @Transactional
    public void verifyPayment(SePayWebhookRequest request) {

        String regex = "SSOS\\d{8}[A-Z0-9]{6}";
        Matcher matcher = Pattern.compile(regex).matcher(request.content());

        if (!matcher.find()) {
            throw new NotFoundException("Order code not found in transaction content");
        }

        String orderCode = matcher.group();

        Order order = getOrderByCode(orderCode);

        double amountReceived = request.transferAmount().doubleValue();
        double orderTotalAmount = order.getTotalAmount();

        Payment payment = order.getPayments().getFirst();
        payment.setAmountReceived(amountReceived);

        if (amountReceived < orderTotalAmount) {
            throw new BadRequestException("Not enough payment amount");
        }

        // update trạng thái
        payment.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.CONFIRMED);


        // remove all current cart items of this customer
        Customer customer = order.getCustomer();
        if (customer != null && customer.getId() != null) {
            cartItemRepository.deleteAllByCustomer_Id(customer.getId());
        }


        orderRepository.saveAndFlush(order);
        paymentRepository.saveAndFlush(payment);


        // realtime notify
        messagingTemplate.convertAndSend(
                "/topic/orders",
                new OrderPaid(order.getOrderCode(), order.getOrderStatus())
        );
    }

    public Order findOrderById(UUID orderId){
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private void validateItems(List<OrderItemRequest> items) {

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

    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }


}
