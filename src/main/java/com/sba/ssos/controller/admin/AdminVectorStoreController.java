package com.sba.ssos.controller.admin;

import com.sba.ssos.ai.vectorstore.VectorDocumentResponse;
import com.sba.ssos.ai.vectorstore.VectorStoreAdminService;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/vector-store")
@RequiredArgsConstructor
public class AdminVectorStoreController {

    private final VectorStoreAdminService vectorStoreAdminService;

    @GetMapping("/documents")
    public ResponseGeneral<PageResponse<VectorDocumentResponse>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter) {
        var data = vectorStoreAdminService.getDocuments(page, size, filter);
        return ResponseGeneral.ofSuccess("Vector store documents retrieved", data);
    }

    @GetMapping("/documents/{id}")
    public ResponseGeneral<VectorDocumentResponse> getDocument(@PathVariable String id) {
        var data = vectorStoreAdminService.getDocument(id);
        return ResponseGeneral.ofSuccess("Vector store document retrieved", data);
    }

    public record BulkDeleteByIdsRequest(@NotEmpty List<String> ids) {}

    @DeleteMapping("/documents/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocumentsByIds(@RequestBody @Valid BulkDeleteByIdsRequest request) {
        vectorStoreAdminService.deleteDocumentsByIds(request.ids());
    }

    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable String id) {
        vectorStoreAdminService.deleteDocument(id);
    }

    public record BulkDeleteRequest(@NotBlank String filter) {}

    @DeleteMapping("/documents")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocuments(@RequestBody @Valid BulkDeleteRequest request) {
        vectorStoreAdminService.deleteDocuments(request.filter());
    }
}
