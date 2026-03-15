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
import com.sba.ssos.entity.CartItem;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.Order;
import com.sba.ssos.entity.OrderDetail;
import com.sba.ssos.entity.Payment;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.entity.User;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private static final long PAYMENT_EXPIRE_MINUTES = 5;
  private static final String ORDER_CODE_PREFIX = "SSOS";
  private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("SSOS\\d{8}[A-Z0-9]{6}");

  private final ShoeVariantService shoeVariantService;
  private final ShoeVariantRepository shoeVariantRepository;
  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final ApplicationProperties applicationProperties;
  private final CartItemRepository cartItemRepository;
  private final CustomerService customerService;
  private final SimpMessagingTemplate messagingTemplate;
  private final ShoeImageService shoeImageService;

  @Transactional
  public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest) {
    Customer customer = customerService.getCurrentCustomer();
    log.info("Creating order for customer {}", customer.getId());

    List<OrderItemRequest> requestItems =
        customer.getCartItems().stream()
            .map(
                cartItem ->
                    new OrderItemRequest(
                        cartItem.getShoeVariant().getId(), cartItem.getQuantity()))
            .toList();

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

    log.info("Created order {} for customer {}", order.getId(), customer.getId());

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

  @Transactional(readOnly = true)
  public PaymentInfoResponse getPaymentInfo(UUID orderId) {
    Customer customer = customerService.getCurrentCustomer();
    log.debug("Fetching payment info for order {} by customer {}", orderId, customer.getId());

    Order order = getCustomerOrderById(orderId, customer.getId());
    Payment payment =
        order.getPayments().stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("error.payment.not_found"));

    return new PaymentInfoResponse(
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
    Customer customer = customerService.getCurrentCustomer();
    UUID orderId = orderExpiredRequest.orderId();
    log.info("Handling payment timeout for order {} by customer {}", orderId, customer.getId());

    Order order = getCustomerOrderById(orderId, customer.getId());
    order.setOrderStatus(OrderStatus.PAYMENT_EXPIRED);
    orderRepository.saveAndFlush(order);

    List<Payment> payments = order.getPayments();
    for (Payment payment : payments) {
      payment.setPaymentStatus(PaymentStatus.TIME_OUT);
    }
    paymentRepository.saveAllAndFlush(payments);

    List<CartItem> cartItems = order.getCustomer().getCartItems();
    for (CartItem cartItem : cartItems) {
      cartItem.setActive(true);
    }
    cartItemRepository.saveAllAndFlush(cartItems);
    log.info("Marked order {} as payment expired", orderId);
  }

  @Transactional(readOnly = true)
  public Page<CustomerOrderHistoryResponse> getOrderHistoryByCustomer(
      OrderHistoryRequest orderHistoryRequest) {
    Customer customer = customerService.getCurrentCustomer();
    log.debug(
        "Fetching customer order history for customer {} with page={}, size={}, status={}",
        customer.getId(),
        orderHistoryRequest.page(),
        orderHistoryRequest.size(),
        orderHistoryRequest.orderStatus());

    int page = orderHistoryRequest.page();
    int size = orderHistoryRequest.size() > 0 ? orderHistoryRequest.size() : 10;
    Pageable pageable = PageRequest.of(page, size);

    Page<Order> orderPage =
        orderRepository.findOrderHistoryByCustomer(customer.getId(), orderHistoryRequest, pageable);

    List<Order> content = orderPage.getContent();
    if (content.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<UUID> orderIds = content.stream().map(Order::getId).toList();
    List<Order> ordersWithDetails = orderRepository.findByIdInWithOrderDetails(orderIds);
    Map<UUID, Order> orderById =
        ordersWithDetails.stream().collect(Collectors.toMap(Order::getId, order -> order));

    List<CustomerOrderHistoryResponse> responses =
        content.stream()
            .map(order -> orderById.get(order.getId()))
            .filter(Objects::nonNull)
            .map(this::mapToCustomerOrderHistoryResponse)
            .toList();

    return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public CustomerOrderHistoryResponse getOrderDetailByCustomer(UUID orderId) {
    Customer customer = customerService.getCurrentCustomer();
    log.debug("Fetching customer order detail {} for customer {}", orderId, customer.getId());
    return mapToCustomerOrderHistoryResponse(getCustomerOrderById(orderId, customer.getId()));
  }

  @Transactional(readOnly = true)
  public Page<OrderHistoryResponse> getOrderHistoryByAdmin(OrderHistoryRequest orderHistoryRequest) {
    log.debug(
        "Fetching admin order history with page={}, size={}, search='{}', status={}",
        orderHistoryRequest.page(),
        orderHistoryRequest.size(),
        orderHistoryRequest.nameSearch(),
        orderHistoryRequest.orderStatus());

    Pageable pageable =
        PageRequest.of(
            orderHistoryRequest.page(),
            orderHistoryRequest.size() != 0 ? orderHistoryRequest.size() : 5);

    Page<Order> orderPage = orderRepository.findOrderHistory(orderHistoryRequest, pageable);
    List<Order> content = orderPage.getContent();

    if (content.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<UUID> orderIds = content.stream().map(Order::getId).toList();
    List<Order> ordersWithDetails = orderRepository.findByIdInWithOrderDetails(orderIds);
    Map<UUID, Order> orderById =
        ordersWithDetails.stream().collect(Collectors.toMap(Order::getId, order -> order));

    List<OrderHistoryResponse> responses =
        content.stream()
            .map(order -> orderById.get(order.getId()))
            .filter(Objects::nonNull)
            .map(this::mapToAdminOrderHistoryResponse)
            .toList();

    return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
  }

  @Transactional
  public void verifyPayment(SePayWebhookRequest request) {
    log.info("Processing SePay webhook");

    Matcher matcher = ORDER_CODE_PATTERN.matcher(request.content());
    if (!matcher.find()) {
      log.warn("Order code not found in SePay webhook content");
      throw new NotFoundException("error.order.code.not_found");
    }

    String orderCode = matcher.group();
    log.info("Resolved payment webhook to order code {}", orderCode);
    Order order = getOrderByCode(orderCode);

    double amountReceived = request.transferAmount().doubleValue();
    double orderTotalAmount = order.getTotalAmount();

    Payment payment =
        order.getPayments().stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("error.payment.not_found"));
    payment.setAmountReceived(amountReceived);

    if (amountReceived < orderTotalAmount) {
      log.warn(
          "Received insufficient payment for order {}: expected={}, actual={}",
          orderCode,
          orderTotalAmount,
          amountReceived);
      throw new BadRequestException("error.order.payment.amount_insufficient");
    }

    payment.setPaymentStatus(PaymentStatus.PAID);
    order.setOrderStatus(OrderStatus.CONFIRMED);

    Customer customer = order.getCustomer();
    if (customer != null && customer.getId() != null) {
      cartItemRepository.deleteAllByCustomer_Id(customer.getId());
    }

    orderRepository.saveAndFlush(order);
    paymentRepository.saveAndFlush(payment);

    log.info("Order {} marked as paid", order.getId());
    messagingTemplate.convertAndSend(
        "/topic/orders", new OrderPaid(order.getOrderCode(), order.getOrderStatus()));
  }

  private CustomerOrderHistoryResponse mapToCustomerOrderHistoryResponse(Order order) {
    List<CustomerOrderItemResponse> items =
        (order.getOrderDetails() == null ? List.<OrderDetail>of() : order.getOrderDetails())
            .stream()
            .map(this::mapToCustomerOrderItemResponse)
            .toList();

    return new CustomerOrderHistoryResponse(
        order.getId(),
        order.getOrderCode(),
        order.getOrderStatus().name(),
        order.getCreatedAt(),
        items,
        order.getTotalAmount());
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
        shoe != null ? shoe.getId() : null,
        variant != null ? variant.getId() : null,
        name,
        image,
        price,
        size,
        quantity);
  }

  private OrderHistoryResponse mapToAdminOrderHistoryResponse(Order order) {
    Payment primaryPayment = order.getPayments().stream().findFirst().orElse(null);
    Customer customer = order.getCustomer();
    User user = customer != null ? customer.getUser() : null;

    List<CustomerOrderItemResponse> items =
        (order.getOrderDetails() == null ? List.<OrderDetail>of() : order.getOrderDetails())
            .stream()
            .map(this::mapToCustomerOrderItemResponse)
            .toList();

    Long itemCount =
        items.stream()
            .map(CustomerOrderItemResponse::quantity)
            .filter(Objects::nonNull)
            .reduce(0L, Long::sum);

    return new OrderHistoryResponse(
        order.getId(),
        order.getOrderCode(),
        order.getCreatedAt(),
        resolveCustomerName(user, order.getShippingName()),
        user != null ? user.getEmail() : "",
        order.getOrderStatus().toString(),
        primaryPayment != null ? primaryPayment.getPaymentStatus() : PaymentStatus.PENDING,
        primaryPayment != null && primaryPayment.getPaymentMethod() != null
            ? primaryPayment.getPaymentMethod()
            : PaymentMethod.ONLINE,
        itemCount,
        order.getTotalAmount(),
        items);
  }

  private void validateItems(List<OrderItemRequest> items) {
    for (OrderItemRequest item : items) {
      ShoeVariant shoeVariant = shoeVariantService.findById(item.shoeVariantId());

      if (shoeVariant.getQuantity() < item.quantity()) {
        log.warn(
            "Out of stock for variant {}. requested={}, available={}",
            item.shoeVariantId(),
            item.quantity(),
            shoeVariant.getQuantity());
        throw new BadRequestException(
            "error.order.out_of_stock", "variantId", item.shoeVariantId());
      }
    }
  }

  private Double calculatePrice(List<OrderItemRequest> items) {
    List<UUID> ids = items.stream().map(OrderItemRequest::shoeVariantId).toList();

    Map<UUID, ShoeVariant> variantMap =
        shoeVariantRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(ShoeVariant::getId, variant -> variant));

    double totalPrice = 0;
    for (OrderItemRequest item : items) {
      ShoeVariant variant = variantMap.get(item.shoeVariantId());
      if (variant == null) {
        throw new NotFoundException("error.order.variant.not_found");
      }
      totalPrice += item.quantity() * variant.getShoe().getPrice();
    }

    return totalPrice;
  }

  public Order getOrderByCode(String orderCode) {
    return orderRepository
        .findByOrderCode(orderCode)
        .orElseThrow(() -> new NotFoundException("error.order.not_found"));
  }

  public Order getOrderById(UUID orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(() -> new NotFoundException("error.order.not_found"));
  }

  private Order getCustomerOrderById(UUID orderId, UUID customerId) {
    Order order =
        orderRepository
            .findByIdWithOrderDetails(orderId)
            .orElseThrow(() -> new NotFoundException("error.order.not_found"));

    if (order.getCustomer() == null || !customerId.equals(order.getCustomer().getId())) {
      log.warn("Customer {} attempted to access order {} without ownership", customerId, orderId);
      throw new NotFoundException("error.order.not_found");
    }

    return order;
  }

  private String resolveCustomerName(User user, String fallbackName) {
    if (user == null) {
      return fallbackName == null ? "" : fallbackName;
    }

    String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
    if (!fullName.isBlank()) {
      return fullName;
    }

    return fallbackName == null ? "" : fallbackName;
  }
}
