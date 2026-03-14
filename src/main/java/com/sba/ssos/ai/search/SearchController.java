package com.sba.ssos.ai.search;

import com.sba.ssos.dto.ResponseGeneral;
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
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

  private final SearchService searchService;

  public record SearchRequest(String query) {}

  public record SearchResult(String id, String source, String content, Double score) {}

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
