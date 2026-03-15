package com.sba.ssos.controller.order;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.response.order.sepay.SePayWebhookRequest;
import com.sba.ssos.service.order.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.WEBHOOKS + "/orders")
@RequiredArgsConstructor
@Tag(name = "Order Webhooks", description = "Order payment webhook endpoints")
public class OrderPaymentWebhookController {

  private final OrderService orderService;

  @PostMapping("/sepay/hook")
  public void verifyOrder(@Valid @RequestBody SePayWebhookRequest request) {
    orderService.verifyPayment(request);
  }
}
