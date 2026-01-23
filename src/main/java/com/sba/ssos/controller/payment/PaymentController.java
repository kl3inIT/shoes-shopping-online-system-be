package com.sba.ssos.controller.payment;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.payment.PaymentRequestDTO;
import com.sba.ssos.dto.response.payment.PaymentResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
        if (!apiKey.equals("Apikey " + props.securityProperties().sepayApiKey())) {
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

    @PostMapping("/init")
    public ResponseEntity<ResponseGeneral<PaymentResponseDTO>> createPayment(@RequestBody PaymentRequestDTO paymentRequestDTO) {
        ResponseGeneral<PaymentResponseDTO> responseDTO = new ResponseGeneral<>();
//        responseDTO.setMessage(SUCCESS);
//        responseDTO.setData(paymentService.createPayment(paymentRequestDTO));
        return ResponseEntity.ok(responseDTO);
    }


}
