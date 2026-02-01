package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    Order toEntity(OrderCreateRequest request);

}