package com.sba.ssos.controller.order;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderExpiredRequest;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.dto.response.order.CustomerOrderHistoryResponse;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.dto.response.order.PaymentInfoResponse;
import com.sba.ssos.service.order.OrderService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CUSTOMER_ORDERS)
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer order creation and history endpoints")
public class CustomerOrderController {

  private final OrderService orderService;
  private final LocaleUtils localeUtils;

  @PostMapping("/init")
  public ResponseGeneral<OrderCreateResponse> createOrder(
      @Valid @RequestBody OrderCreateRequest orderCreateRequest) {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.order.created"), orderService.createOrder(orderCreateRequest));
  }

  @GetMapping("/payment-info/{orderId}")
  public ResponseGeneral<PaymentInfoResponse> getPaymentInfo(@PathVariable UUID orderId) {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.order.payment.info"), orderService.getPaymentInfo(orderId));
  }

  @PostMapping("/expired")
  public ResponseGeneral<Void> handlePaymentExpired(
      @Valid @RequestBody OrderExpiredRequest orderExpiredRequest) {
    orderService.handlePaymentTimeout(orderExpiredRequest);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.order.expired.updated"));
  }

  @GetMapping
  public ResponseGeneral<Page<CustomerOrderHistoryResponse>> getOrdersByCustomer(
      @Valid @ModelAttribute OrderHistoryRequest orderHistoryRequest) {
    Page<CustomerOrderHistoryResponse> orders =
        orderService.getOrderHistoryByCustomer(orderHistoryRequest);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.order.get"), orders);
  }

  @GetMapping("/{orderId}")
  public ResponseGeneral<CustomerOrderHistoryResponse> getOrderDetailByCustomer(
      @PathVariable UUID orderId) {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.order.get"), orderService.getOrderDetailByCustomer(orderId));
  }
}
