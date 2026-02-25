package com.sba.ssos.repository;

import com.sba.ssos.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {

    @Query("select coalesce(sum(od.quantity), 0) from OrderDetail od " +
            "where od.order.orderStatus = com.sba.ssos.enums.OrderStatus.DELIVERED")
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

