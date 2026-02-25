package com.sba.ssos.ai.rag.tool;

import com.sba.ssos.ai.parameters.ParametersReader;
import com.sba.ssos.ai.rag.AbstractRagTool;
import com.sba.ssos.ai.rag.PostRetrievalProcessor;
import com.sba.ssos.ai.rag.Reranker;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

public class ReviewTool extends AbstractRagTool {

  public ReviewTool(
      VectorStore vectorStore,
      PostRetrievalProcessor postRetrievalProcessor,
      Reranker reranker,
      ParametersReader reader,
      List<Document> retrievedDocuments,
      Consumer<String> logger) {
    super(
        "review_retriever",
        "review",
        vectorStore,
        postRetrievalProcessor,
        reranker,
        reader,
        retrievedDocuments,
        logger);
  }
}
