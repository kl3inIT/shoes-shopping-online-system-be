package com.sba.ssos.ai.rag;

import com.sba.ssos.ai.parameters.ParametersReader;
import com.sba.ssos.ai.parameters.ParametersService;
import com.sba.ssos.ai.rag.tool.CartQueryTool;
import com.sba.ssos.ai.rag.tool.OrderTool;
import com.sba.ssos.ai.rag.tool.PolicyTool;
import com.sba.ssos.ai.rag.tool.ProductTool;
import com.sba.ssos.ai.rag.tool.ReviewTool;
import com.sba.ssos.ai.rag.tool.WishlistQueryTool;
import com.sba.ssos.repository.CartItemRepository;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.repository.WishlistRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolsManager {

  private final VectorStore vectorStore;
  private final PostRetrievalProcessor postRetrievalProcessor;
  private final Reranker reranker;
  private final ParametersService parametersService;
  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final CartItemRepository cartItemRepository;
  private final WishlistRepository wishlistRepository;

  public List<ToolCallback> getToolCallbacks(
      List<Document> retrievedDocuments, Consumer<String> logger, @Nullable String userId) {

    ParametersReader reader = parametersService.loadReader();
    List<ToolCallback> callbacks = new ArrayList<>();

    if (reader.getBoolean("tools.shop_policies_retriever.enabled", true)) {
      callbacks.add(
          new PolicyTool(
                  vectorStore, postRetrievalProcessor, reranker, reader, retrievedDocuments, logger)
              .getToolCallback());
    }

    if (reader.getBoolean("tools.product_retriever.enabled", true)) {
      callbacks.add(
          new ProductTool(
                  vectorStore, postRetrievalProcessor, reranker, reader, retrievedDocuments, logger)
              .getToolCallback());
    }

    if (reader.getBoolean("tools.review_retriever.enabled", true)) {
      callbacks.add(
          new ReviewTool(
                  vectorStore, postRetrievalProcessor, reranker, reader, retrievedDocuments, logger)
              .getToolCallback());
    }

    if (reader.getBoolean("tools.order_retriever.enabled", true) && userId != null) {
      callbacks.add(
          new OrderTool(
                  vectorStore,
                  postRetrievalProcessor,
                  reranker,
                  reader,
                  retrievedDocuments,
                  logger,
                  userId)
              .getToolCallback());
    }

    if (userId != null) {
      if (reader.getBoolean("tools.cart_query.enabled", true)) {
        callbacks.add(
            new CartQueryTool(
                    logger, userId, userRepository, customerRepository, cartItemRepository)
                .getToolCallback());
      }

      if (reader.getBoolean("tools.wishlist_query.enabled", true)) {
        callbacks.add(
            new WishlistQueryTool(
                    logger, userId, userRepository, customerRepository, wishlistRepository)
                .getToolCallback());
      }
    }

    return callbacks;
  }

  public List<AbstractRagTool> getRagTools(
      List<Document> retrievedDocuments, Consumer<String> logger) {

    ParametersReader reader = parametersService.loadReader();
    List<AbstractRagTool> tools = new ArrayList<>();

    if (reader.getBoolean("tools.shop_policies_retriever.enabled", true)) {
      tools.add(
          new PolicyTool(
              vectorStore, postRetrievalProcessor, reranker, reader, retrievedDocuments, logger));
    }

    if (reader.getBoolean("tools.product_retriever.enabled", true)) {
      tools.add(
          new ProductTool(
              vectorStore, postRetrievalProcessor, reranker, reader, retrievedDocuments, logger));
    }

    if (reader.getBoolean("tools.review_retriever.enabled", true)) {
      tools.add(
          new ReviewTool(
              vectorStore, postRetrievalProcessor, reranker, reader, retrievedDocuments, logger));
    }

    return tools;
  }
}
