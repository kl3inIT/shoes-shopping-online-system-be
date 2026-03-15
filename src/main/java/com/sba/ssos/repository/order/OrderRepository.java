package com.sba.ssos.repository.order;

import com.sba.ssos.entity.Order;
import com.sba.ssos.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, OrderRepositoryCustom {

    Page<Order> findByCustomer_Id(UUID customerId, Pageable pageable);

    Optional<Order> findByOrderCode(String orderCode);

    @Query("select distinct o from Order o " +
            "left join fetch o.customer c " +
            "left join fetch c.user " +
            "left join fetch o.orderDetails d " +
            "left join fetch d.shoeVariant v " +
            "left join fetch v.shoe " +
            "where o.id in :ids")
    List<Order> findByIdInWithOrderDetails(@Param("ids") Collection<UUID> ids);

    @Query("select distinct o from Order o " +
            "left join fetch o.customer c " +
            "left join fetch c.user " +
            "left join fetch o.orderDetails d " +
            "left join fetch d.shoeVariant v " +
            "left join fetch v.shoe " +
            "where o.id = :id")
    Optional<Order> findByIdWithOrderDetails(@Param("id") UUID id);

    Long countByOrderStatus(OrderStatus status);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.orderStatus = com.sba.ssos.enums.OrderStatus.DELIVERED")
    Double sumDeliveredRevenue();

    @Query("""
            select function('date', o.createdAt) as orderDate,
                   sum(o.totalAmount) as totalRevenue
            from Order o
            where o.orderStatus = com.sba.ssos.enums.OrderStatus.DELIVERED
              and o.createdAt >= :from
            group by function('date', o.createdAt)
            order by orderDate
            """)
    List<Object[]> findRevenueByDateSince(Instant from);

    Page<Order> findByOrderStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
}
