/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package grading.model;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory index mapping generated feedback report files to the local repository
 * they originated from.
 * This is intentionally NOT persisted.
 */
public final class ReportRepoIndex {
    private final Map<Path, Path> reportToRepo = new HashMap<>();
    private final Set<Path> reposTouched = new HashSet<>();


    public void register(Path reportPath, Path localRepoPath) {
        Objects.requireNonNull(reportPath, "reportPath");
        Objects.requireNonNull(localRepoPath, "localRepoPath");
        Path r = normalize(reportPath);
        Path repo = normalize(localRepoPath);
        reportToRepo.put(r, repo);
        reposTouched.add(repo);
    }

    public Optional<Path> findRepoForReport(Path reportPath) {
        Objects.requireNonNull(reportPath, "reportPath");
        return Optional.ofNullable(reportToRepo.get(normalize(reportPath)));
    }

    public boolean containsReport(Path reportPath) {
        Objects.requireNonNull(reportPath, "reportPath");
        return reportToRepo.containsKey(normalize(reportPath));
    }

    public Set<Path> touchedReposSnapshot() {
        return Set.copyOf(reposTouched);
    }

    public void markRepoTouched(Path repoPath) {
        Objects.requireNonNull(repoPath, "repoPath");
        reposTouched.add(normalize(repoPath));
    }

    public void clear() {
        reportToRepo.clear();
    }

    public Map<Path, Path> snapshot() {
        return Map.copyOf(reportToRepo);
    }

    private static Path normalize(Path p) {
        return p.toAbsolutePath().normalize();
    }
}

