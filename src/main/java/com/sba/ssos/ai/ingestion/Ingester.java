package com.sba.ssos.ai.ingestion;

/**
 * Contract for a data ingester that pushes domain data into the VectorStore.
 * Modeled after {@code Ingester} in jmix-ai-backend.
 */
public interface Ingester {

  /** Unique type identifier, e.g. "policy", "product". */
  String getType();

  /** Re-ingest all data of this type. Returns a human-readable summary. */
  String ingestAll();
}
