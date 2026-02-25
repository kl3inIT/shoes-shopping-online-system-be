package com.sba.ssos.ai.rag;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.springframework.ai.document.Document;

public final class Utils {

  private Utils() {}

  public static String getUrlOrSource(Document document) {
    String url = (String) document.getMetadata().get("url");
    return url != null ? url : (String) document.getMetadata().get("source");
  }

  public static String getDocSourcesAsString(List<Document> documents) {
    return documents.stream()
        .map(doc -> "(%.3f) %s".formatted(doc.getScore(), getUrlOrSource(doc)))
        .toList()
        .toString();
  }

  public static String getRerankResultsAsString(List<Reranker.Result> rerankResults) {
    return rerankResults.stream()
        .map(rr -> "(%.3f) %s".formatted(rr.score(), getUrlOrSource(rr.document())))
        .toList()
        .toString();
  }

  public static List<Document> getDistinctDocuments(List<Document> documents) {
    Set<Object> seen = new HashSet<>();
    return documents.stream()
        .sorted(
            (d1, d2) -> {
              Double rs1 = (Double) d1.getMetadata().get("rerankScore");
              Double rs2 = (Double) d2.getMetadata().get("rerankScore");
              if (rs1 != null && rs2 != null) {
                return Double.compare(rs2, rs1);
              }
              if (d1.getScore() != null && d2.getScore() != null) {
                return Double.compare(d2.getScore(), d1.getScore());
              }
              return 0;
            })
        .filter(d -> seen.add(d.getId()))
        .toList();
  }

  public static void addLogMessage(Logger log, List<String> logMessages, String message) {
    String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    logMessages.add(time + " " + message);
    log.debug(message);
  }

  public static String abbreviate(String s, int maxWidth) {
    if (s == null) return "";
    return s.length() <= maxWidth ? s : s.substring(0, maxWidth - 3) + "...";
  }
}
