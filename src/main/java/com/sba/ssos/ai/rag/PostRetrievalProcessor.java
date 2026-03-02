package com.sba.ssos.ai.rag;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class PostRetrievalProcessor {

  public List<Document> process(String userQuery, List<Document> documents) {
    return documents;
  }
}
