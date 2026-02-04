package com.sba.ssos.service.shoevariants;

import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.ShoeVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShoeVariantService {

    private final ShoeVariantRepository shoeVariantRepository;

    public ShoeVariant findById(UUID id) {
        return shoeVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ShoeVariant not found " + id));
    }

}
