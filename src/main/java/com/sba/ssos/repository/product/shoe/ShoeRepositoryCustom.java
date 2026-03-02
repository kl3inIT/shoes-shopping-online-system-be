package com.sba.ssos.repository.product.shoe;

import com.sba.ssos.entity.Shoe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ShoeRepositoryCustom {

    Page<Shoe> searchShoes(
            String search,
            List<UUID> brandIds,
            List<String> sizes,
            List<UUID> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> statuses,
            List<String> genders,
            Pageable pageable
    );
}

