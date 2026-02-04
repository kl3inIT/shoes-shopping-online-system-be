package com.sba.ssos.service.cart;

import com.sba.ssos.entity.Cart;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public Cart findByCustomerId(UUID  customerId){
        return cartRepository.findByCustomer_Id(customerId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

    }
}
