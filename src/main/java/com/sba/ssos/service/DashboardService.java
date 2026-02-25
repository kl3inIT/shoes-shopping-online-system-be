package com.sba.ssos.service;

import com.sba.ssos.dto.dashboard.DashboardChartPointResponse;
import com.sba.ssos.dto.dashboard.DashboardLowStockResponse;
import com.sba.ssos.dto.dashboard.DashboardMetricsResponse;
import com.sba.ssos.dto.dashboard.DashboardRecentOrderResponse;
import com.sba.ssos.dto.dashboard.DashboardTopSellingResponse;
import com.sba.ssos.entity.Order;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.repository.OrderDetailRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ShoeVariantRepository shoeVariantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics() {
        Double totalRevenue = orderRepository.sumDeliveredRevenue();
        Long totalOrders = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);
        Long totalCustomers = userRepository.countByRole(UserRole.ROLE_CUSTOMER);
        Long productsSold = orderDetailRepository.sumDeliveredQuantity();

        return new DashboardMetricsResponse(
                totalRevenue != null ? totalRevenue : 0.0,
                totalCustomers != null ? totalCustomers : 0L,
                totalOrders != null ? totalOrders : 0L,
                productsSold != null ? productsSold : 0L
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardChartPointResponse> getChartData(int days) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = today.minusDays(days);
        Instant fromInstant = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> raw = orderRepository.findRevenueByDateSince(fromInstant);

        return raw.stream()
                .map(row -> {
                    Object dateObj = row[0];
                    Object revenueObj = row[1];
                    LocalDate date;
                    if (dateObj instanceof LocalDate ld) {
                        date = ld;
                    } else {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                    }
                    Double revenue = revenueObj != null ? ((Number) revenueObj).doubleValue() : 0.0;
                    // Tạm tính profit là 30% revenue cho mục đích demo
                    Double profit = revenue * 0.3;
                    return new DashboardChartPointResponse(date, revenue, profit);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardRecentOrderResponse> getRecentOrders(int limit) {
        PageRequest page = PageRequest.of(0, limit);
        return orderRepository
                .findByOrderStatusOrderByCreatedAtDesc(OrderStatus.DELIVERED, page)
                .stream()
                .map(this::toRecentOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardTopSellingResponse> getTopSelling(int limit) {
        List<Object[]> raw = orderDetailRepository.findTopSellingShoes();
        if (raw.isEmpty()) {
            return List.of();
        }

        // Lấy danh sách shoeId để tính tồn kho hiện tại
        List<UUID> shoeIds = raw.stream()
                .map(row -> (UUID) row[0])
                .toList();

        List<ShoeVariant> variants = shoeVariantRepository.findAll().stream()
                .filter(v -> shoeIds.contains(v.getShoe().getId()))
                .toList();

        Map<UUID, Long> stockByShoe = variants.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getShoe().getId(),
                        Collectors.summingLong(ShoeVariant::getQuantity)
                ));

        return raw.stream()
                .limit(limit)
                .map(row -> {
                    UUID shoeId = (UUID) row[0];
                    String shoeName = (String) row[1];
                    String categoryName = (String) row[2];
                    Long totalSold = ((Number) row[3]).longValue();
                    Long stock = stockByShoe.getOrDefault(shoeId, 0L);
                    return new DashboardTopSellingResponse(shoeName, categoryName, totalSold, stock);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardLowStockResponse> getLowStock(int threshold) {
        List<ShoeVariant> variants = shoeVariantRepository.findByQuantityLessThan((long) threshold);
        return variants.stream()
                .map(this::toLowStockResponse)
                .toList();
    }

    private DashboardRecentOrderResponse toRecentOrderResponse(Order order) {
        String customerName = order.getCustomer().getUser().getFirstName() + " " +
                order.getCustomer().getUser().getLastName();
        return new DashboardRecentOrderResponse(
                order.getId(),
                order.getOrderCode(),
                customerName,
                order.getCreatedAt(),
                order.getTotalAmount(),
                order.getOrderStatus()
        );
    }

    private DashboardLowStockResponse toLowStockResponse(ShoeVariant variant) {
        Shoe shoe = variant.getShoe();
        Long remaining = variant.getQuantity();
        String status;
        if (remaining <= 0) {
            status = "Out of Stock";
        } else if (remaining <= 2) {
            status = "Critical";
        } else {
            status = "Warning";
        }
        String productName = shoe.getName();
        return new DashboardLowStockResponse(productName, variant.getSize(), remaining, status);
    }
}

