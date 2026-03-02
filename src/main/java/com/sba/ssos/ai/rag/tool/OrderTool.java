package com.sba.ssos.ai.rag.tool;

import com.sba.ssos.ai.parameters.ParametersReader;
import com.sba.ssos.ai.rag.AbstractRagTool;
import com.sba.ssos.ai.rag.PostRetrievalProcessor;
import com.sba.ssos.ai.rag.Reranker;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

public class OrderTool extends AbstractRagTool {

  private final String userId;

  public OrderTool(
      VectorStore vectorStore,
      PostRetrievalProcessor postRetrievalProcessor,
      Reranker reranker,
      ParametersReader reader,
      List<Document> retrievedDocuments,
      Consumer<String> logger,
      String userId) {
    super(
        "order_retriever",
        "order",
        vectorStore,
        postRetrievalProcessor,
        reranker,
        reader,
        retrievedDocuments,
        logger);
    this.userId = userId;
  }

  @Override
  protected Filter.Expression buildFilterExpression() {
    FilterExpressionBuilder b = new FilterExpressionBuilder();
    return b.and(b.eq("docType", docType), b.eq("userId", userId)).build();
  }
}
