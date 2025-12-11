package com.example.back_end.controller;

import com.example.back_end.util.UploadStorage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private record UploadResult(String url, String path) {}

    private UploadResult saveFile(MultipartFile file, String kind) throws IOException {
        LocalDate d = LocalDate.now();
        Path root = UploadStorage.resolveRoot();
        Path base = UploadStorage.ensureKindDir(root, kind, d);
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext != null && !ext.isBlank() ? "." + ext : "");
        Path target = base.resolve(filename);
        Files.createDirectories(target.getParent());
        try (var in = file.getInputStream()) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        String url = UploadStorage.buildPublicUrl(root, target);
        return new UploadResult(url, target.toAbsolutePath().toString());
    }

    @PostMapping(path = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No file"));
        var res = saveFile(file, "images");
        return ResponseEntity.status(201).body(Map.of("url", res.url(), "path", res.path()));
    }

    @PostMapping(path = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadVideo(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No file"));
        var res = saveFile(file, "videos");
        return ResponseEntity.status(201).body(Map.of("url", res.url(), "path", res.path()));
    }

    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No file"));
        var res = saveFile(file, "files");
        return ResponseEntity.status(201).body(Map.of(
                "url", res.url(),
                "path", res.path(),
                "size", file.getSize(),
                "contentType", file.getContentType(),
                "name", file.getOriginalFilename()
        ));
    }
}
