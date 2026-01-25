package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "SHIPMENTS",
    indexes = {
      @Index(name = "idx_shipment_order", columnList = "order_id"),
      @Index(name = "idx_shipment_manager", columnList = "manager_id"),
      @Index(name = "idx_shipment_status", columnList = "status"),
      @Index(name = "idx_shipment_tracking_number", columnList = "tracking_number", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ORDER_ID", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "MANAGER_ID")
  private User manager;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private ShipmentStatus status = ShipmentStatus.ASSIGNED;

  @Column(name = "TRACKING_NUMBER", unique = true, length = 100)
  private String trackingNumber;
}
