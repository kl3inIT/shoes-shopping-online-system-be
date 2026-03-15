package com.sba.ssos.controller.order;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.dto.response.order.OrderHistoryResponse;
import com.sba.ssos.service.order.OrderService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ADMIN_ORDERS)
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Administrative order monitoring endpoints")
public class AdminOrderController {

  private final OrderService orderService;
  private final LocaleUtils localeUtils;

  @GetMapping
  public ResponseGeneral<Page<OrderHistoryResponse>> getOrdersByAdmin(
      @Valid @ModelAttribute OrderHistoryRequest orderHistoryRequest) {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.order.get"), orderService.getOrderHistoryByAdmin(orderHistoryRequest));
  }
}
