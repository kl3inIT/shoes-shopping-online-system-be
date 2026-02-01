package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderCreateRequest request);

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "orderCode", target = "orderCode")
    @Mapping(source = "totalAmount", target = "amount")
    @Mapping(source = "orderStatus", target = "status")
    OrderCreateResponse toCreateResponse(
            Order order,
            String transferContent,
            String bankNumber,
            String bankCode,
            String accountHolder,
            LocalDateTime expiredAt
    );
}