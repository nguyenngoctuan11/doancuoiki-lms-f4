package com.example.back_end.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper for resolving the physical upload directory used by both the upload controller
 * and the static resource handler. It gracefully supports the legacy location
 * (projectRoot/uploads) as well as the module specific folder (back-end/uploads).
 */
public final class UploadStorage {

    private UploadStorage() {}

    /**
     * Returns the ordered list of candidate upload roots.
     * First entry is {cwd}/uploads, second is parent/uploads (legacy location).
     */
    public static List<Path> candidateRoots() {
        Path cwd = Paths.get("").toAbsolutePath();
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(cwd.resolve("uploads"));
        Path parent = cwd.getParent();
        if (parent != null) {
            candidates.add(parent.resolve("uploads"));
        }
        return new ArrayList<>(candidates);
    }

    /**
     * Resolve the root directory to store uploads. If a legacy folder already exists it will be reused,
     * otherwise the first candidate (./uploads) is created.
     */
    public static Path resolveRoot() throws IOException {
        for (Path candidate : candidateRoots()) {
            if (Files.exists(candidate)) {
                Files.createDirectories(candidate);
                return candidate.toAbsolutePath();
            }
        }
        Path fallback = candidateRoots().get(0).toAbsolutePath();
        Files.createDirectories(fallback);
        return fallback;
    }

    public static Path ensureKindDir(Path root, String kind, LocalDate date) throws IOException {
        Path base = root.resolve(kind)
                .resolve(String.valueOf(date.getYear()))
                .resolve(String.format("%02d", date.getMonthValue()));
        Files.createDirectories(base);
        return base;
    }

    public static String buildPublicUrl(Path root, Path file) {
        Path relative = root.relativize(file);
        String normalized = relative.toString().replace("\\", "/");
        return "/uploads/" + normalized;
    }
}
