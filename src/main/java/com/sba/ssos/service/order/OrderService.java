package com.sba.ssos.service.order;


import com.sba.ssos.dto.request.order.OrderCreateRequest;
import com.sba.ssos.dto.request.order.OrderItemRequest;
import com.sba.ssos.dto.response.order.OrderCreateResponse;
import com.sba.ssos.entity.*;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.enums.PaymentStatus;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.OrderMapper;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.OrderRepository;
import com.sba.ssos.repository.PaymentRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.service.UserService;
import com.sba.ssos.utils.OrderCodeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ShoeVariantRepository shoeVariantRepository;
    private final UserService userService;
    private final CustomerRepository  customerRepository;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest){
        List<OrderItemRequest> items = orderCreateRequest.items();

        validateItems(items);

        Customer customer = customerRepository
                .findByUser_Id(userService.getCurrentUser().userId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Double totalAmount = calculatePrice(items);

        Order order = orderMapper.toEntity(orderCreateRequest);
        order.setId(UUID.randomUUID());
        order.setOrderCode(OrderCodeUtils.generate("SSOS"));
        order.setCustomer(customer);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setTotalAmount(totalAmount);

        List<OrderDetail> orderDetails = new ArrayList<>();
        for (OrderItemRequest item : items) {
            ShoeVariant variant = findById(item.shoeVariantId());
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setShoeVariant(variant);
            detail.setQuantity(item.quantity());
            orderDetails.add(detail);

            // trừ stock
            variant.setQuantity(variant.getQuantity() - item.quantity());
        }
        order.setOrderDetails(orderDetails);
        orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTotalAmount(totalAmount);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);


        return null;


    }


    private void validateItems(List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            UUID shoeVariantId = item.shoeVariantId();
            ShoeVariant shoeVariant = findById(shoeVariantId);

            if (shoeVariant.getQuantity() < item.quantity()) {
                throw new BadRequestException("Out of stock: " + shoeVariantId);
            }
        }
    }

    private Double calculatePrice(List<OrderItemRequest> items) {
        List<UUID> ids = items.stream()
                .map(OrderItemRequest::shoeVariantId)
                .toList();

        Map<UUID, ShoeVariant> variantMap =
                shoeVariantRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(ShoeVariant::getId, v -> v));

        double totalPrice = 0;

        for (OrderItemRequest item : items) {
            ShoeVariant variant = variantMap.get(item.shoeVariantId());
            if (variant == null) {
                throw new NotFoundException("ShoeVariant not found " + item.shoeVariantId());
            }
            totalPrice += item.quantity() * variant.getShoe().getPrice();
        }

        return totalPrice;
    }



    public ShoeVariant findById(UUID id) {
        return shoeVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ShoeVariant not found " + id));
    }





}
