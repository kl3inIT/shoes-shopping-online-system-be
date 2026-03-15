package com.sba.ssos.ai.search;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for raw vector search (no LLM). Useful for debugging
 * the RAG retrieval pipeline.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/search")
@RequiredArgsConstructor
@Tag(name = "AI Search", description = "Raw semantic search endpoints")
public class SearchController {

  private final SearchService searchService;

  @Schema(description = "Semantic search request")
  public record SearchRequest(@Schema(description = "Free-text query") String query) {}

  @Schema(description = "Semantic search result")
  public record SearchResult(
      @Schema(description = "Vector document id") String id,
      @Schema(description = "Document source metadata") String source,
      @Schema(description = "Matched document content") String content,
      @Schema(description = "Similarity score") Double score) {}

  @PostMapping
  public ResponseEntity<ResponseGeneral<List<SearchResult>>> search(
      @RequestBody SearchRequest request) {

    List<Document> documents = searchService.search(request.query());

    List<SearchResult> results =
        documents.stream()
            .map(
                doc ->
                    new SearchResult(
                        doc.getId(),
                        (String) doc.getMetadata().getOrDefault("source", ""),
                        doc.getText(),
                        doc.getScore()))
            .toList();

    return ResponseEntity.ok(
        ResponseGeneral.ofSuccess("Search completed", results));
  }
}
