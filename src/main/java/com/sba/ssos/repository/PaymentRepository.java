package com.sba.ssos.repository;

import com.sba.ssos.entity.Payment;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("""
            select p from Payment p
            join fetch p.order o
            where o.orderStatus = :orderStatus
              and p.paymentStatus = :paymentStatus
              and p.expiredAt < :expiredBefore
            order by p.createdAt asc
            """)
    List<Payment> findExpiredPaymentsForPendingOrders(
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("expiredBefore") LocalDateTime expiredBefore);
}
