package com.sba.ssos.repository.product.shoe.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.enums.ShoeStatus;
import com.sba.ssos.repository.product.shoe.ShoeRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.sba.ssos.entity.QShoe.shoe;
import static com.sba.ssos.entity.QShoeVariant.shoeVariant;

@Repository
@RequiredArgsConstructor
public class ShoeRepositoryImpl implements ShoeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Shoe> searchShoes(
            String search,
            List<UUID> brandIds,
            List<String> sizes,
            List<UUID> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> statuses,
            List<String> genders,
            Pageable pageable
    ) {
        List<Shoe> content = queryFactory
                .selectFrom(shoe)
                .where(
                        notDeleted(),
                        nameOrSlugContains(search),
                        hasBrands(brandIds),
                        hasCategories(categoryIds),
                        priceGte(minPrice),
                        priceLte(maxPrice),
                        hasStatuses(statuses),
                        hasGenders(genders),
                        hasSizes(sizes)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(shoe.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(shoe.count())
                .from(shoe)
                .where(
                        notDeleted(),
                        nameOrSlugContains(search),
                        hasBrands(brandIds),
                        hasCategories(categoryIds),
                        priceGte(minPrice),
                        priceLte(maxPrice),
                        hasStatuses(statuses),
                        hasGenders(genders),
                        hasSizes(sizes)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /* ===================== PREDICATES ===================== */

    private BooleanExpression notDeleted() {
        return shoe.deleted.isFalse();
    }

    private BooleanExpression nameOrSlugContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim();
        return shoe.name.containsIgnoreCase(normalized)
                .or(shoe.slug.containsIgnoreCase(normalized));
    }

    private BooleanExpression hasBrands(List<UUID> brandIds) {
        return brandIds == null || brandIds.isEmpty()
                ? null
                : shoe.brand.id.in(brandIds);
    }

    private BooleanExpression hasCategories(List<UUID> categoryIds) {
        return categoryIds == null || categoryIds.isEmpty()
                ? null
                : shoe.category.id.in(categoryIds);
    }

    private BooleanExpression priceGte(BigDecimal minPrice) {
        return minPrice == null
                ? null
                : shoe.price.goe(minPrice.doubleValue());
    }

    private BooleanExpression priceLte(BigDecimal maxPrice) {
        return maxPrice == null
                ? null
                : shoe.price.loe(maxPrice.doubleValue());
    }

    private BooleanExpression hasStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        List<ShoeStatus> statusEnums = statuses.stream()
                .map(String::toUpperCase)
                .map(ShoeStatus::valueOf)
                .toList();
        return shoe.status.in(statusEnums);
    }

    private BooleanExpression hasGenders(List<String> genders) {
        return genders == null || genders.isEmpty()
                ? null
                : shoe.gender.stringValue().in(genders);
    }

    private BooleanExpression hasSizes(List<String> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }

        return shoe.id.in(
                JPAExpressions
                        .select(shoeVariant.shoe.id)
                        .from(shoeVariant)
                        .where(shoeVariant.size.in(sizes))
        );
    }
}

