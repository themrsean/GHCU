/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
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
 * In-memory index mapping generated feedback report files to the local student
 * repositories they originated from.
 * <p>
 * This class is used by the grading workflow to support operations such as
 * deploying feedback reports into repositories and publishing those changes via
 * git. The index is intentionally not persisted; it exists only for the duration
 * of the application session.
 * <p>
 * All paths are normalized to absolute, normalized form before being stored or
 * queried to ensure consistent lookup behavior.
 *
 * @author Sean Jones
 */
public final class ReportRepoIndex {
    private final Map<Path, Path> reportToRepo = new HashMap<>();
    private final Set<Path> reposTouched = new HashSet<>();

    /**
     * Registers a mapping between a generated feedback report file and the local
     * repository directory it originated from.
     * <p>
     * This mapping is stored in-memory for the current application session only.
     * If the same report path is registered multiple times, the most recent mapping
     * replaces the previous one.
     *
     * @param reportPath path to the generated report file
     * @param localRepoPath path to the local repository that produced the report
     * @throws NullPointerException if {@code reportPath} or {@code localRepoPath} is null
     */
    public void register(Path reportPath, Path localRepoPath) {
        Objects.requireNonNull(reportPath, "reportPath");
        Objects.requireNonNull(localRepoPath, "localRepoPath");
        Path r = normalize(reportPath);
        Path repo = normalize(localRepoPath);
        reportToRepo.put(r, repo);
    }

    /**
     * Looks up the local repository associated with a given report file.
     *
     * @param reportPath path to a report file
     * @return an {@link Optional} containing the repository path if the report is
     *         known to the index; otherwise {@link Optional#empty()}
     * @throws NullPointerException if {@code reportPath} is null
     */
    public Optional<Path> findRepoForReport(Path reportPath) {
        Objects.requireNonNull(reportPath, "reportPath");
        return Optional.ofNullable(reportToRepo.get(normalize(reportPath)));
    }

    /**
     * Checks whether the given report path is registered in this index.
     *
     * @param reportPath path to a report file
     * @return {@code true} if the report is registered; {@code false} otherwise
     * @throws NullPointerException if {@code reportPath} is null
     */
    public boolean containsReport(Path reportPath) {
        Objects.requireNonNull(reportPath, "reportPath");
        return reportToRepo.containsKey(normalize(reportPath));
    }

    /**
     * Returns an immutable snapshot of all repositories that have been marked as
     * "touched" during the current session.
     * <p>
     * A repository is considered touched if it has had at least one feedback report
     * deployed/published into it.
     *
     * @return an immutable snapshot of touched repository paths
     */
    public Set<Path> touchedReposSnapshot() {
        return Set.copyOf(reposTouched);
    }

    /**
     * Marks a repository as "touched" for this session.
     * <p>
     * This should be called after successfully deploying and/or publishing at least
     * one feedback report into the given repository.
     *
     * @param repoPath path to a local repository
     * @throws NullPointerException if {@code repoPath} is null
     */
    public void markRepoTouched(Path repoPath) {
        Objects.requireNonNull(repoPath, "repoPath");
        reposTouched.add(normalize(repoPath));
    }

    /**
     * Clears all in-memory state held by this index.
     * <p>
     * This removes all report-to-repository mappings and all touched repository
     * records.
     */
    public void clear() {
        reportToRepo.clear();
        reposTouched.clear();
    }

    /**
     * Returns an immutable snapshot of the current report-to-repository mapping.
     * <p>
     * The returned map is a defensive copy and cannot be modified.
     *
     * @return an immutable snapshot of the report-to-repository index
     */
    public Map<Path, Path> reportToSnapshot() {
        return Map.copyOf(reportToRepo);
    }

    private static Path normalize(Path p) {
        return p.toAbsolutePath().normalize();
    }
}

