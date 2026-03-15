package com.sba.ssos.ai.ingestion;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.entity.Order;
import com.sba.ssos.entity.OrderDetail;
import com.sba.ssos.entity.Payment;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.repository.order.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class OrderIngester extends AbstractIngester {

  private final OrderRepository orderRepository;
  private final ApplicationProperties properties;

  public OrderIngester(
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      ApplicationProperties properties,
      OrderRepository orderRepository) {
    super(
        vectorStore,
        jdbcTemplate,
        properties.ragProperties().schema(),
        properties.ragProperties().table());
    this.properties = properties;
    this.orderRepository = orderRepository;
  }

  @Override
  public String getType() {
    return "order";
  }

  @Override
  @Transactional
  public String ingestAll() {
    return super.ingestAll();
  }

  @Override
  protected List<String> loadSources() {
    return orderRepository.findAll().stream()
        .map(o -> o.getId().toString())
        .toList();
  }

  @Override
  protected int getSourceLimit() {
    return 0;
  }

  @Override
  @Nullable
  protected Document loadDocument(String source) {
    UUID orderId = UUID.fromString(source);
    Order order = orderRepository.findById(orderId).orElse(null);
    if (order == null) return null;

    String text = buildOrderText(order);
    Map<String, Object> metadata = createMetadata(source, text);
    metadata.put("orderId", order.getId().toString());
    metadata.put("orderCode", order.getOrderCode());
    metadata.put("customerId", order.getCustomer().getId().toString());
    metadata.put("userId", order.getCustomer().getUser().getKeycloakId().toString());
    metadata.put("orderStatus", order.getOrderStatus().name());
    return createDocument(text, metadata);
  }

  @Override
  protected List<Document> splitToChunks(List<Document> documents) {
    var rag = properties.ragProperties();
    TokenTextSplitter splitter =
        TokenTextSplitter.builder()
            .withChunkSize(rag.chunkSize())
            .withMinChunkSizeChars(200)
            .withMinChunkLengthToEmbed(20)
            .withKeepSeparator(true)
            .build();

    List<Document> allChunks = new ArrayList<>();
    for (Document document : documents) {
      if (document.getText().length() < MAX_CHUNK_SIZE) {
        allChunks.add(document);
        continue;
      }
      List<Document> chunks = splitter.apply(List.of(document));
      int index = 0;
      for (Document chunk : chunks) {
        chunk.getMetadata().putAll(document.getMetadata());
        chunk.getMetadata().put("chunkIndex", index++);
      }
      allChunks.addAll(chunks);
    }
    return allChunks;
  }

  private String buildOrderText(Order order) {
    StringBuilder sb = new StringBuilder();
    sb.append("Order Code: ").append(order.getOrderCode()).append("\n");
    sb.append("Status: ").append(order.getOrderStatus()).append("\n");
    sb.append("Total Amount: ").append(String.format("%.0f", order.getTotalAmount()))
        .append(" VND\n");
    sb.append("Shipping Name: ").append(order.getShippingName()).append("\n");
    sb.append("Shipping Address: ").append(order.getShippingAddress()).append("\n");
    sb.append("Shipping Phone: ").append(order.getShippingPhone()).append("\n");

    if (order.getNotes() != null && !order.getNotes().isBlank()) {
      sb.append("Notes: ").append(order.getNotes()).append("\n");
    }

    List<OrderDetail> details = order.getOrderDetails();
    if (details != null && !details.isEmpty()) {
      sb.append("Items:\n");
      for (OrderDetail detail : details) {
        ShoeVariant variant = detail.getShoeVariant();
        sb.append("  - ").append(variant.getShoe().getName())
            .append(" (Size: ").append(variant.getSize())
            .append(", Color: ").append(variant.getColor())
            .append(") x").append(detail.getQuantity()).append("\n");
      }
    }

    List<Payment> payments = order.getPayments();
    if (payments != null && !payments.isEmpty()) {
      sb.append("Payment:\n");
      for (Payment payment : payments) {
        sb.append("  - Method: ").append(payment.getPaymentMethod())
            .append(", Status: ").append(payment.getPaymentStatus())
            .append(", Amount: ").append(String.format("%.0f", payment.getTotalAmount()))
            .append(" VND");
        if (payment.getAmountReceived() != null) {
          sb.append(", Received: ").append(String.format("%.0f", payment.getAmountReceived()))
              .append(" VND");
        }
        sb.append("\n");
      }
    }

    return sb.toString();
  }
}
