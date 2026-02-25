package com.sba.ssos.ai.rag;

import com.sba.ssos.ai.parameters.ParametersReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.ReflectionUtils;

public abstract class AbstractRagTool {

  protected final String toolName;
  protected final String docType;
  protected final VectorStore vectorStore;
  protected final PostRetrievalProcessor postRetrievalProcessor;
  protected final Reranker reranker;
  protected final List<Document> retrievedDocuments;
  protected final Consumer<String> logger;

  protected String description;
  protected double similarityThreshold;
  protected int topK;
  protected int topReranked;
  protected double minScore;
  protected double minRerankedScore;
  protected String noResultsMessage;

  protected AbstractRagTool(
      String toolName,
      String docType,
      VectorStore vectorStore,
      PostRetrievalProcessor postRetrievalProcessor,
      Reranker reranker,
      ParametersReader reader,
      List<Document> retrievedDocuments,
      Consumer<String> logger) {

    this.toolName = toolName;
    this.docType = docType;
    this.vectorStore = vectorStore;
    this.postRetrievalProcessor = postRetrievalProcessor;
    this.reranker = reranker;
    this.retrievedDocuments = retrievedDocuments;
    this.logger = logger;
    init(reader);
  }

  protected String getToolRootKey() {
    return "tools." + toolName;
  }

  protected void init(ParametersReader reader) {
    description = reader.getString(getToolRootKey() + ".description");
    similarityThreshold = reader.getDouble(getToolRootKey() + ".similarityThreshold");
    topK = reader.getInt(getToolRootKey() + ".topK", 10);
    topReranked = reader.getInt(getToolRootKey() + ".topReranked", 5);
    minScore = reader.getDouble(getToolRootKey() + ".minScore");
    minRerankedScore = reader.getDouble(getToolRootKey() + ".minRerankedScore");
    noResultsMessage =
        reader.getString(
            getToolRootKey() + ".noResultsMessage", "No results found. Try rephrasing your query.");
  }

  public ToolCallback getToolCallback() {
    Method method =
        Objects.requireNonNull(ReflectionUtils.findMethod(getClass(), "execute", String.class));

    return MethodToolCallback.builder()
        .toolDefinition(
            ToolDefinition.builder()
                .name(toolName)
                .description(description)
                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .build())
        .toolObject(this)
        .toolMethod(method)
        .build();
  }

  protected Filter.Expression buildFilterExpression() {
    return new FilterExpressionBuilder().eq("docType", docType).build();
  }

  public String execute(String queryText) {
    logger.accept(
        ">>> Using %s [topK=%d, threshold=%.2f]: %s"
            .formatted(toolName, topK, similarityThreshold, queryText));

    SearchRequest searchRequest =
        SearchRequest.builder()
            .filterExpression(buildFilterExpression())
            .query(queryText)
            .similarityThreshold(similarityThreshold)
            .topK(topK)
            .build();

    List<Document> documents = vectorStore.similaritySearch(searchRequest);
    if (documents.isEmpty()) {
      logger.accept("No documents found for the query");
      return noResultsMessage;
    }

    logger.accept(
        "Found %d documents: %s"
            .formatted(
                documents.size(),
                documents.stream()
                    .map(d -> "(%.3f) %s".formatted(d.getScore(), d.getMetadata().get("source")))
                    .toList()));

    documents = postRetrievalProcessor.process(queryText, documents);
    if (documents.isEmpty()) {
      logger.accept("All documents filtered out by PostRetrievalProcessor");
      return noResultsMessage;
    }

    List<Reranker.Result> rerankResults = reranker.rerank(queryText, documents, topReranked);

    List<Document> filteredDocuments;
    if (rerankResults == null || rerankResults.isEmpty()) {
      logger.accept("Reranking skipped, filtering by minScore=%.2f".formatted(minScore));
      filteredDocuments =
          documents.stream()
              .filter(
                  doc -> minScore <= 0.0 || doc.getScore() == null || doc.getScore() >= minScore)
              .toList();
    } else {
      filteredDocuments =
          rerankResults.stream()
              .filter(rr -> rr.score() >= minRerankedScore)
              .map(Reranker.Result::document)
              .toList();
    }

    if (filteredDocuments.isEmpty()) {
      return noResultsMessage;
    }

    logger.accept("Returning %d documents to LLM".formatted(filteredDocuments.size()));
    retrievedDocuments.addAll(filteredDocuments);

    return filteredDocuments.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
  }
}
