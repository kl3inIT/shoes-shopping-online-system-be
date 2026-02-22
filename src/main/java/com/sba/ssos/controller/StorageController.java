package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.service.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final MinioStorageService minioStorageService;

    /**
     * Lấy presigned URL để GET (xem ảnh) hoặc PUT (upload).
     * GET /api/storage/presigned-url?objectKey=brands/xxx.png&action=GET
     * GET /api/storage/presigned-url?objectKey=brands/xxx.png&action=PUT
     */
    @GetMapping("/presigned-url")
    public ResponseGeneral<Map<String, String>> getPresignedUrl(
            @RequestParam String objectKey,
            @RequestParam(defaultValue = "GET") String action) {
        String url = "PUT".equalsIgnoreCase(action)
                ? minioStorageService.getPresignedPutUrl(objectKey)
                : minioStorageService.getPresignedGetUrl(objectKey);
        return ResponseGeneral.ofSuccess("OK", Map.of("url", url));
    }

    /**
     * Xóa object trong MinIO.
     * DELETE /api/storage?objectKey=brands/xxx.png
     */
    @DeleteMapping
    public ResponseGeneral<Void> deleteObject(@RequestParam String objectKey) {
        minioStorageService.deleteObject(objectKey);
        return ResponseGeneral.ofSuccess("Deleted");
    }
}
