package com.sba.ssos.ai.rag.tool;

import com.sba.ssos.ai.rag.AbstractDbTool;
import com.sba.ssos.entity.CartItem;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.entity.User;
import com.sba.ssos.repository.CartItemRepository;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Queries the current user's shopping cart directly from the database.
 * Returns real-time cart contents so the LLM can answer questions
 * like "what's in my cart?" or "how many items do I have?".
 */
public class CartQueryTool extends AbstractDbTool {

  private final String userId;
  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final CartItemRepository cartItemRepository;

  public CartQueryTool(
      Consumer<String> logger,
      String userId,
      UserRepository userRepository,
      CustomerRepository customerRepository,
      CartItemRepository cartItemRepository) {
    super(
        "cart_query",
        "Look up the current user's shopping cart to see what items they have, quantities, and total",
        logger);
    this.userId = userId;
    this.userRepository = userRepository;
    this.customerRepository = customerRepository;
    this.cartItemRepository = cartItemRepository;
  }

  @Override
  public String execute(String queryText) {
    logger.accept(">>> Querying cart for user: " + userId);

    User user = userRepository.findByKeycloakId(UUID.fromString(userId)).orElse(null);
    if (user == null) {
      logger.accept("User not found");
      return "Could not find user information.";
    }

    Customer customer = customerRepository.findByUser_Id(user.getId()).orElse(null);
    if (customer == null) {
      logger.accept("Customer profile not found");
      return "No customer profile found for this user.";
    }

    List<CartItem> items = cartItemRepository.findAllByCustomer_IdAndIsActiveTrue(customer.getId());
    if (items.isEmpty()) {
      logger.accept("Cart is empty");
      return "Your shopping cart is currently empty.";
    }

    logger.accept("Found %d items in cart".formatted(items.size()));

    StringBuilder sb = new StringBuilder();
    sb.append("Shopping Cart (").append(items.size()).append(" items):\n");

    double total = 0;
    for (CartItem item : items) {
      ShoeVariant variant = item.getShoeVariant();
      String name = variant.getShoe().getName();
      double price = variant.getShoe().getPrice();
      double subtotal = price * item.getQuantity();
      total += subtotal;

      sb.append("  - ").append(name)
          .append(" | Size: ").append(variant.getSize())
          .append(", Color: ").append(variant.getColor())
          .append(" | Qty: ").append(item.getQuantity())
          .append(" | Price: ").append(String.format("%.0f", price)).append(" VND")
          .append(" | Subtotal: ").append(String.format("%.0f", subtotal)).append(" VND\n");
    }
    sb.append("Total: ").append(String.format("%.0f", total)).append(" VND");

    return sb.toString();
  }
}
