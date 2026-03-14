package com.sba.ssos.controller.order;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderExpiredRequest;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.dto.response.order.CustomerOrderHistoryResponse;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.dto.response.order.OrderHistoryResponse;
import com.sba.ssos.dto.response.order.PaymentInfoResponse;
import com.sba.ssos.dto.response.order.sepay.SePayWebhookRequest;
import com.sba.ssos.service.order.OrderService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;


import org.springframework.data.domain.Page;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final LocaleUtils localeUtils;

    // cmd: cloudflared tunnel run sepay-webhook
    @PostMapping("/sepay/hook")
    public Void verifyOrder(@RequestBody SePayWebhookRequest request) {
        orderService.verifyPayment(request);
        return null;
    }

    @PostMapping("/init")
    public ResponseGeneral<OrderCreateResponse> createOrder(@RequestBody OrderCreateRequest orderCreateRequest) {
        OrderCreateResponse response = orderService.createOrder(orderCreateRequest);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.created"), response);
    }

    @GetMapping("/payment-info/{orderId}")
    public ResponseGeneral<PaymentInfoResponse> getPaymentInfo(
            @PathVariable UUID orderId
    ) {
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.order.payment.info"),
                orderService.getPaymentInfo(orderId)
        );
    }


    @PostMapping("/expired")
    public ResponseGeneral<OrderCreateResponse> handlePaymentExpired(@RequestBody OrderExpiredRequest orderExpiredRequest) {
        orderService.handlePaymentTimeout(orderExpiredRequest);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.expired.updated"));
    }

    /**
     * Lấy danh sách đơn hàng theo customer đang đăng nhập.
     * Query params: page (0-based), size, orderStatus, dateFrom (ISO-8601), dateTo (ISO-8601).
     * Trả về phân trang, mỗi đơn có kèm danh sách items (sản phẩm trong đơn).
     */
    @GetMapping()
    public ResponseGeneral<Page<CustomerOrderHistoryResponse>> getOrdersByCustomer(
            @ModelAttribute OrderHistoryRequest orderHistoryRequest
    ) {
        Page<CustomerOrderHistoryResponse> orders = orderService.getOrderHistoryByCustomer(orderHistoryRequest);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.get"), orders);
    }

    /**
     * Lấy chi tiết 1 đơn hàng cho customer đang đăng nhập.
     */
    @GetMapping("/{orderId}")
    public ResponseGeneral<CustomerOrderHistoryResponse> getOrderDetailByCustomer(
            @PathVariable UUID orderId
    ) {
        CustomerOrderHistoryResponse order = orderService.getOrderDetailByCustomer(orderId);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.get"), order);
    }

    @GetMapping("/admin")
    public ResponseGeneral<Page<OrderHistoryResponse>> getOrdersByAdmin(@ModelAttribute OrderHistoryRequest orderHistoryRequest) {
        Page<OrderHistoryResponse> orders = orderService.getOrderHistoryByAdmin(orderHistoryRequest);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.get"), orders);
    }










}
