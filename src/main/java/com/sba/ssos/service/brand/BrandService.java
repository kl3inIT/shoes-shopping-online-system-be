package com.sba.ssos.service.brand;

import com.sba.ssos.entity.Brand;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public Brand findById(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new NotFoundException("Brand", brandId));
    }

}
