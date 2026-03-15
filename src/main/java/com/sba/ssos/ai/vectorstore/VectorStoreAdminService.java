package com.sba.ssos.ai.vectorstore;

import com.sba.ssos.dto.response.PageResponse;
import java.util.List;

/**
 * Admin service interface for vector store document management.
 */
public interface    VectorStoreAdminService {

    PageResponse<VectorDocumentResponse> getDocuments(int page, int size);

    PageResponse<VectorDocumentResponse> getDocuments(int page, int size, String filter);

    VectorDocumentResponse getDocument(String id);

    void deleteDocument(String id);

    void deleteDocuments(String filterExpression);

    void deleteDocumentsByIds(List<String> ids);
}
