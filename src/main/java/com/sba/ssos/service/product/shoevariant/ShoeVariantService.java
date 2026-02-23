package com.sba.ssos.service.product.shoevariant;

import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.ShoeVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShoeVariantService {

    private final ShoeVariantRepository shoeVariantRepository;

    public ShoeVariant findById(UUID id) {
        return shoeVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ShoeVariant not found " + id));
    }

    public List<ShoeVariantResponse> getVariantsByShoeId(UUID shoeId) {
        return shoeVariantRepository.findByShoe_IdOrderBySizeAscColorAsc(shoeId)
                .stream()
                .map(v -> new ShoeVariantResponse(
                        v.getId(),
                        v.getShoe().getId(),
                        v.getSize(),
                        v.getColor(),
                        v.getQuantity(),
                        v.getSku()
                ))
                .toList();
    }
}
