package com.sba.ssos.repository.order;

import com.sba.ssos.entity.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, OrderRepositoryCustom {

    Page<Order> findByCustomer_Id(UUID customerId, Pageable pageable);

    Optional<Order> findByOrderCode(String orderCode);

    /** Load orders với orderDetails, shoeVariant, shoe để tránh N+1 khi map sang response. */
    @Query("select distinct o from Order o " +
            "left join fetch o.orderDetails d " +
            "left join fetch d.shoeVariant v " +
            "left join fetch v.shoe " +
            "where o.id in :ids")
    List<Order> findByIdInWithOrderDetails(@Param("ids") Collection<UUID> ids);

}
