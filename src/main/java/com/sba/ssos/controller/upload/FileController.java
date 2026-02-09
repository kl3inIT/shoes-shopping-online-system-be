package com.sba.ssos.controller.upload;

import com.sba.ssos.dto.response.upload.FileResource;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.service.storage.MinioFileStorageService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioFileStorageService storageService;
    private final LocaleUtils localeUtils;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseGeneral<String> upload(@RequestParam("file") MultipartFile file) {
        String objectKey = storageService.upload(file);
        return ResponseGeneral.ofCreated(
                localeUtils.get("success.file.uploaded"),
                objectKey
        );
    }

    @GetMapping("/{objectKey}")
    public ResponseEntity<Resource> getImage(@PathVariable String objectKey) {
        FileResource file = storageService.getFile(objectKey);
        InputStreamResource resource = new InputStreamResource(file.inputStream());
        MediaType mediaType = MediaType.parseMediaType(file.contentType());

        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(resource);
    }
}

