package com.sba.ssos.repository.order;

import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderRepositoryCustom {

    Page<Order> findOrderHistory(
            OrderHistoryRequest request,
            Pageable pageable
    );

    /**
     * Lấy danh sách đơn hàng theo customer với filter (status, dateFrom, dateTo).
     */
    Page<Order> findOrderHistoryByCustomer(
            UUID customerId,
            OrderHistoryRequest request,
            Pageable pageable
    );
}
