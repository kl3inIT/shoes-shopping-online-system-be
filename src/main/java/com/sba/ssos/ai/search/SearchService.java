package com.sba.ssos.ai.search;

import static com.sba.ssos.ai.rag.Utils.getDistinctDocuments;

import com.sba.ssos.ai.rag.AbstractRagTool;
import com.sba.ssos.ai.rag.ToolsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

  private final ToolsManager toolsManager;

  public List<Document> search(String query) {
    List<Document> retrievedDocuments = new ArrayList<>();
    Consumer<String> logger = log::debug;

    List<AbstractRagTool> tools = toolsManager.getRagTools(retrievedDocuments, logger);

    for (AbstractRagTool tool : tools) {
      tool.execute(query);
    }

    return getDistinctDocuments(retrievedDocuments);
  }
}
