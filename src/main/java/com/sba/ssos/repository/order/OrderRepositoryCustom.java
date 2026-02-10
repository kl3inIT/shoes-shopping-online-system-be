package com.sba.ssos.repository.order;

import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

    Page<Order> findOrderHistory(
            OrderHistoryRequest request,
            Pageable pageable
    );
}
