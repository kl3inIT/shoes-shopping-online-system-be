package com.sba.ssos.controller.upload;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.service.storage.MinioFileStorageService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseGeneral<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String url = storageService.upload(file);
        return ResponseGeneral.ofCreated(
                localeUtils.get("success.file.uploaded"),
                url
        );
    }
}

