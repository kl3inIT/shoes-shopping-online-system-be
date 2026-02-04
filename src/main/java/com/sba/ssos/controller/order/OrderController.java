package com.sba.ssos.controller.order;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderExpiredRequest;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.service.order.OrderService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        String regex = "[A-Za-z0-9]{12}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher((String) payload.get("content"));
        String transactionCode = null;
        if (matcher.find()) {
            transactionCode = matcher.group();
        } else {
            throw new IllegalArgumentException("Not Found transaction code");
        }
//        paymentService.completePayment(transactionCode);
//
//        String downloadLink = domain + "/api/payments/download/invoice/" + transactionCode;
//        paymentService.sendInvoiceToEmail(payload, transactionCode, downloadLink);
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




}
