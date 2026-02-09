package com.sba.ssos.controller.order;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderExpiredRequest;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.dto.response.order.OrderHistoryResponse;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.service.order.OrderService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ApplicationProperties props;
    private final LocaleUtils localeUtils;

    // cmd: cloudflared tunnel run sepay-webhook
    @PostMapping("/sepay/hook")
    public Void verifyOrder(@RequestBody Map<String, Object> payload) {
        orderService.verifyPayment(payload);
        return null;
    }

    @PostMapping("/init")
    public ResponseGeneral<OrderCreateResponse> createPayment(@RequestBody OrderCreateRequest orderCreateRequest) {
        OrderCreateResponse response = orderService.createOrder(orderCreateRequest);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.created"), response);
    }

    @PostMapping("/expired")
    public ResponseGeneral<OrderCreateResponse> handPaymentExpired(@RequestBody OrderExpiredRequest orderExpiredRequest) {
        orderService.handlePaymentTimeout(orderExpiredRequest);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.expired.updated"));
    }

    @GetMapping()
    public ResponseGeneral<List<OrderHistoryResponse>> getPayments(@ModelAttribute OrderHistoryRequest orderHistoryRequest) {
        List<OrderHistoryResponse> orders = orderService.getOrderHistoryByCustomer(orderHistoryRequest);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.order.get"), orders);
    }




}
