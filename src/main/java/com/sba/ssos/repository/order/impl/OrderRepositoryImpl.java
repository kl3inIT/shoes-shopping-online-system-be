package com.sba.ssos.repository.order.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sba.ssos.dto.request.order.OrderHistoryRequest;
import com.sba.ssos.entity.Order;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.repository.order.OrderRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static com.sba.ssos.entity.QCustomer.customer;
import static com.sba.ssos.entity.QOrder.order;
import static com.sba.ssos.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {


    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> findOrderHistory(OrderHistoryRequest request, Pageable pageable) {

        List<Order> content = queryFactory
                .selectFrom(order)
                .join(order.customer, customer).fetchJoin()
                .join(customer.user, user).fetchJoin()
                .where(
                        nameSearch(request.nameSearch()),
                        createdFrom(request.dateFrom()),
                        createdTo(request.dateTo()),
                        hasStatus(request.orderStatus())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .join(order.customer, customer)
                .join(customer.user, user)
                .where(
                        nameSearch(request.nameSearch()),
                        createdFrom(request.dateFrom()),
                        createdTo(request.dateTo()),
                        hasStatus(request.orderStatus())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /* ===================== PREDICATES ===================== */

    private BooleanExpression nameSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return order.orderCode.containsIgnoreCase(keyword)
                .or(user.email.containsIgnoreCase(keyword))
                .or(user.firstName.containsIgnoreCase(keyword))
                .or(user.lastName.containsIgnoreCase(keyword));
    }

    private BooleanExpression createdFrom(Instant dateFrom) {
        return dateFrom == null ? null : order.createdAt.goe(dateFrom);
    }

    private BooleanExpression createdTo(Instant dateTo) {
        return dateTo == null ? null : order.createdAt.loe(dateTo);
    }

    private BooleanExpression hasStatus(OrderStatus status) {
        return status == null ? null : order.orderStatus.eq(status);
    }
}
