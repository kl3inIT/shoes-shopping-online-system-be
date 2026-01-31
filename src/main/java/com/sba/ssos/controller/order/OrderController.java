package com.sba.ssos.controller.order;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.response.payment.OrderCreateResponse;
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


    private final ApplicationProperties props;

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
    public ResponseEntity<ResponseGeneral<OrderCreateResponse>> createPayment(@RequestBody OrderCreateRequest orderCreateRequest) {
        ResponseGeneral<OrderCreateResponse> responseDTO = new ResponseGeneral<>();
//        responseDTO.setMessage(SUCCESS);
//        responseDTO.setData(paymentService.createPayment(paymentRequestDTO));
        return ResponseEntity.ok(responseDTO);
    }


}
