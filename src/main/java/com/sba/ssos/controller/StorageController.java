package com.sba.ssos.controller;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.service.storage.MinioStorageService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiPaths.STORAGE)
@RequiredArgsConstructor
@Tag(name = "Storage", description = "File storage utility endpoints")
public class StorageController {

    private final MinioStorageService minioStorageService;
    private final LocaleUtils localeUtils;

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
        return ResponseGeneral.ofSuccess(localeUtils.get("success.storage.presigned_url"), Map.of("url", url));
    }

    /**
     * Xóa object trong MinIO.
     * DELETE /api/storage?objectKey=brands/xxx.png
     */
    @DeleteMapping
    public ResponseGeneral<Void> deleteObject(@RequestParam String objectKey) {
        minioStorageService.deleteObject(objectKey);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.storage.deleted"));
    }
}
