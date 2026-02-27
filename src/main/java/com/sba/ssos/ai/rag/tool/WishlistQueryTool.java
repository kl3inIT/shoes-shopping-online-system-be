package com.sba.ssos.ai.rag.tool;

import com.sba.ssos.ai.rag.AbstractDbTool;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.User;
import com.sba.ssos.entity.Wishlist;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.repository.WishlistRepository;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Queries the current user's wishlist directly from the database. Returns real-time wishlist
 * contents so the LLM can answer questions like "what's in my wishlist?" or "do I have this shoe
 * saved?".
 */
public class WishlistQueryTool extends AbstractDbTool {

  private final String userId;
  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final WishlistRepository wishlistRepository;

  public WishlistQueryTool(
      Consumer<String> logger,
      String userId,
      UserRepository userRepository,
      CustomerRepository customerRepository,
      WishlistRepository wishlistRepository) {
    super(
        "wishlist_query",
        "Look up the current user's wishlist to see which shoes they have saved for later",
        logger);
    this.userId = userId;
    this.userRepository = userRepository;
    this.customerRepository = customerRepository;
    this.wishlistRepository = wishlistRepository;
  }

  @Override
  public String execute(String queryText) {
    logger.accept(">>> Querying wishlist for user: " + userId);

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

    List<Wishlist> items = wishlistRepository.findAllByCustomer_Id(customer.getId());
    if (items.isEmpty()) {
      logger.accept("Wishlist is empty");
      return "Your wishlist is currently empty.";
    }

    logger.accept("Found %d items in wishlist".formatted(items.size()));

    StringBuilder sb = new StringBuilder();
    sb.append("Wishlist (").append(items.size()).append(" items):\n");

    for (Wishlist item : items) {
      Shoe shoe = item.getShoe();
      sb.append("  - ")
          .append(shoe.getName())
          .append(" | Brand: ")
          .append(shoe.getBrand().getName())
          .append(" | Price: ")
          .append(String.format("%.0f", shoe.getPrice()))
          .append(" VND")
          .append(" | Status: ")
          .append(shoe.getStatus())
          .append("\n");
    }

    return sb.toString();
  }
}
