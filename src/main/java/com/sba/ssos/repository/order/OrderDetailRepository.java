package com.sba.ssos.repository.order;

import com.sba.ssos.entity.OrderDetail;
import com.sba.ssos.enums.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {

    boolean existsByShoeVariant_Shoe_Id(UUID shoeId);

    boolean existsByShoeVariant_Id(UUID shoeVariantId);

    boolean existsByOrder_Customer_IdAndShoeVariant_IdAndOrder_OrderStatusIn(
            UUID customerId, UUID shoeVariantId, Collection<OrderStatus> statuses);

    List<OrderDetail> findByOrder_Customer_IdAndShoeVariant_Shoe_IdAndOrder_OrderStatusIn(
            UUID customerId, UUID shoeId, Collection<OrderStatus> statuses, Pageable pageable);

    @Query("select coalesce(sum(od.quantity), 0) from OrderDetail od "
            + "where od.order.orderStatus = com.sba.ssos.enums.OrderStatus.DELIVERED")
    Long sumDeliveredQuantity();

    @Query("""
            select od.shoeVariant.shoe.id as shoeId,
                   od.shoeVariant.shoe.name as shoeName,
                   od.shoeVariant.shoe.category.name as categoryName,
                   sum(od.quantity) as totalSold
            from OrderDetail od
            where od.order.orderStatus = com.sba.ssos.enums.OrderStatus.DELIVERED
            group by od.shoeVariant.shoe.id,
                     od.shoeVariant.shoe.name,
                     od.shoeVariant.shoe.category.name
            order by totalSold desc
            """)
    List<Object[]> findTopSellingShoes();
}
