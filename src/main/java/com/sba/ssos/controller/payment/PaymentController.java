package com.sba.ssos.controller.payment;

import com.sba.ssos.configuration.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {


    private final ApplicationProperties props;


    @PostMapping("/sepay/hook")
    public Void verifyOrder(@RequestBody Map<String, Object> payload,
                            @RequestHeader("Authorization") String apiKey) {
        // Validate API Key
        if (!apiKey.equals("Apikey " + props.sepayApiKey())) {
            throw new SecurityException("Invalid API Key");
        }

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
}
