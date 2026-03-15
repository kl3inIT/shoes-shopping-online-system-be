package com.sba.ssos.repository.product.shoe.impl;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sba.ssos.entity.QReview;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.enums.ShoeStatus;
import com.sba.ssos.repository.product.shoe.ShoeRepositoryCustom;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

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
                .leftJoin(shoe.brand).fetchJoin()
                .leftJoin(shoe.category).fetchJoin()
                .where(
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
                .orderBy(resolveSort(pageable))
                .fetch();

        Long total = queryFactory
                .select(shoe.count())
                .from(shoe)
                .where(
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

    private OrderSpecifier<?>[] resolveSort(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return new OrderSpecifier<?>[]{shoe.createdAt.desc()};
        }

        List<OrderSpecifier<?>> specifiers = pageable.getSort().stream()
                .map(this::toOrderSpecifier)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (specifiers.isEmpty()) {
            return new OrderSpecifier<?>[]{shoe.createdAt.desc()};
        }

        boolean sortedByCreatedAt = pageable.getSort().stream()
                .map(Sort.Order::getProperty)
                .filter(Objects::nonNull)
                .anyMatch(property -> "createdAt".equalsIgnoreCase(property));
        if (!sortedByCreatedAt) {
            specifiers.add(shoe.createdAt.desc());
        }

        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order sortOrder) {
        Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
        String property = sortOrder.getProperty() == null
                ? ""
                : sortOrder.getProperty().trim().toLowerCase(Locale.ROOT);

        return switch (property) {
            case "createdat" -> new OrderSpecifier<>(direction, shoe.createdAt);
            case "price" -> new OrderSpecifier<>(direction, shoe.price);
            case "name" -> new OrderSpecifier<>(direction, shoe.name);
            case "rating", "avgrating" -> {
                QReview review = new QReview("shoeReviewSort");
                yield new OrderSpecifier<>(
                        direction,
                        Expressions.numberTemplate(
                                Double.class,
                                "coalesce(({0}), 0)",
                                JPAExpressions
                                        .select(review.numberStars.avg())
                                        .from(review)
                                        .where(review.shoeVariant.shoe.id.eq(shoe.id))
                        )
                );
            }
            case "reviewcount", "popular" -> {
                QReview review = new QReview("shoeReviewCountSort");
                yield new OrderSpecifier<>(
                        direction,
                        Expressions.numberTemplate(
                                Long.class,
                                "coalesce(({0}), 0)",
                                JPAExpressions
                                        .select(review.count())
                                        .from(review)
                                        .where(review.shoeVariant.shoe.id.eq(shoe.id))
                        )
                );
            }
            default -> null;
        };
    }

    /* ===================== PREDICATES ===================== */

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

