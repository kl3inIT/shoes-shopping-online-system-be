package com.sba.ssos.repository.order;

import com.sba.ssos.entity.OrderDetail;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {

  boolean existsByShoeVariant_Shoe_Id(UUID shoeId);
}
