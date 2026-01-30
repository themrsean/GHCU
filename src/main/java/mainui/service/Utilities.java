/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package mainui.service;

import assignments.model.Assignment;
import assignments.model.Rubric;
import assignments.model.RubricItem;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * Static utility methods used by the GitHub Classroom Utilities application.
 *
 * <p>This class contains the grading pipeline operations, including pulling student
 * repositories, extracting source packages into a unified submissions directory,
 * generating import statements for grading, generating feedback reports, and
 * publishing feedback reports back into student repositories via Git.
 *
 * <p>This is a pure utility class and cannot be instantiated.
 * @author Sean Jones
 */
public final class Utilities {
    private Utilities() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /* Public pipeline methods */
    /**
     * Executes the GitHub Classroom pull command (or other Git command sequence)
     * specified by {@code gitHubLink} and runs it in the given target directory.
     *
     * <p>The {@code gitHubLink} is treated as a full command string (split on whitespace)
     * such as {@code "gh classroom clone student-repos"} or similar. The method starts
     * the process, waits for it to complete, and throws an exception if the command
     * exits with a non-zero status.
     *
     * @param gitHubLink the command to run to pull repositories (whitespace-separated)
     * @param target the directory in which the command should be executed
     * @throws NullPointerException if {@code gitHubLink} or {@code target} is {@code null}
     * @throws IOException if the command fails to start or exits with non-zero status
     * @throws InterruptedException if the current thread is interrupted while waiting
     *                              for the process to finish
     */
    public static void pullRepositories(String gitHubLink, Path target)
            throws IOException, InterruptedException {
        Objects.requireNonNull(gitHubLink, "gitHubLink");
        Objects.requireNonNull(target, "target");
        String[] split = gitHubLink.trim().split("\\s+");
        ProcessBuilder pb = new ProcessBuilder(split);
        pb.directory(target.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            String output = new String(p.getInputStream().readAllBytes());
            throw new IOException("Repository pull failed (exit=" + exit + ")\n" + output);
        }
    }

    /**
     * Extracts all student submission packages from the cloned repositories and copies them
     * into a consolidated {@code submissions} directory.
     *
     * <p>This method walks the directory tree rooted at {@code root} and locates directories
     * named {@code src}. When a {@code src} directory is found, its contents are copied into
     * {@code root/submissions/...} preserving relative structure. Directories and files whose
     * names appear in {@code ignored} are skipped.
     *
     * <p>The method also ensures the {@code submissions} directory exists.
     *
     * @param root the directory containing cloned repositories
     * @param ignored file/directory names to skip while copying
     * @throws NullPointerException if {@code root} or {@code ignored} is {@code null}
     * @throws IOException if {@code root} is not a directory or an I/O error occurs
     *                     during traversal/copying
     */
    public static void extractPackages(Path root, List<String> ignored) throws IOException {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(ignored, "ignored");
        if (!Files.isDirectory(root)) {
            throw new IOException("Root is not a directory: " + root);
        }
        Path submissions = root.resolve("submissions");
        Files.createDirectories(submissions);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            @NonNull
            public FileVisitResult preVisitDirectory(@NonNull Path dir,
                                                     @NonNull BasicFileAttributes attrs)
                    throws IOException {
                if (dir.equals(submissions)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (dir.getFileName().toString().equals("src")) {
                    copySrcDirectory(dir, submissions, ignored);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copySrcDirectory(Path src,
                                         Path submissions,
                                         List<String> ignored) throws IOException {
        Path relative = src.getParent().relativize(src);
        Path targetRoot = submissions.resolve(relative);
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            @NonNull
            public FileVisitResult preVisitDirectory(@NonNull Path dir,
                                                     @NonNull BasicFileAttributes attrs)
                    throws IOException {
                if (ignored.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path target = targetRoot.resolve(src.relativize(dir));
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            @NonNull
            public FileVisitResult visitFile(@NonNull Path file,
                                             @NonNull BasicFileAttributes attrs)
                    throws IOException {
                if (!ignored.contains(file.getFileName().toString())) {
                    Path target = targetRoot.resolve(src.relativize(file));
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /* Report generation helpers */
    /**
     * Generates an {@code imports.txt} file containing Java wildcard import statements for each
     * package directory found in the given directory.
     *
     * <p>This is primarily used to help graders quickly paste a list of imports when grading
     * submissions that are organized into packages.
     *
     * <p>The output file is written to {@code path/imports.txt}. Only immediate subdirectories
     * of {@code path} are included, and they are sorted case-insensitively by directory name.
     *
     * @param path the directory whose subdirectories represent packages
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException if the directory cannot be listed or the output file cannot be written
     */
    public static void generateImports(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Path importsFile = path.resolve("imports.txt");
        List<Path> dirs;
        try (Stream<Path> stream = Files.list(path)) {
            dirs = stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString(),
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        try (PrintWriter pw = new PrintWriter(importsFile.toFile())) {
            for (Path dir : dirs) {
                pw.println("import " + dir.getFileName() + ".*;");
            }
        }
    }

    /**
     * Generates feedback report files for the given assignment across all extracted submissions.
     *
     * <p>The method scans the submissions directory, locating repository directories and package
     * directories, then selects files matching {@link Assignment#getFiles()} for inclusion in the
     * report. Each report is written into {@code submissions/feedback}.
     *
     * <p>If {@code checkStyle} is {@code true}, Checkstyle is executed on each included file and
     * any detected errors are rendered as comments in the report.
     *
     * <p>The returned map provides a mapping from each generated report file to the repository
     * directory that produced it. This mapping is used later to publish the report back into
     * the correct Git repository.
     *
     * @param submissions the directory containing extracted student submissions
     * @param assignment the assignment definition containing file list and rubric
     * @param checkStyle whether Checkstyle should be run and included in the report
     * @return a mapping of generated report paths to the corresponding local repository paths
     * @throws NullPointerException if {@code submissions} or {@code assignment} is {@code null}
     * @throws IOException if {@code submissions} is not a directory or report output cannot be
     * created
     */
    public static Map<Path, Path> generateReports(Path submissions,
                                                  Assignment assignment,
                                                  boolean checkStyle)
            throws IOException {
        Objects.requireNonNull(submissions, "submissions");
        Objects.requireNonNull(assignment, "assignment");
        if (!Files.isDirectory(submissions)) {
            throw new IOException("Submissions directory not found: " + submissions);
        }
        Map<Path, Path> reportToRepo = new HashMap<>();
        File[] repos = submissions.toFile().listFiles(File::isDirectory);
        if (repos != null) {
            Path feedback = submissions.resolve("feedback");
            Files.createDirectories(feedback);
            for (File repo : repos) {
                Path localRepoPath = repo.toPath();
                File[] packages = repo.listFiles(File::isDirectory);
                if (packages != null) {
                    for (File pkg : packages) {
                        List<File> files =
                                getFiles(assignment.getFiles(), pkg);
                        if (!files.isEmpty()) {
                            Path reportPath = generateReport(
                                    pkg.getName(),
                                    feedback,
                                    assignment,
                                    files,
                                    checkStyle
                            );

                            if (reportPath != null) {
                                reportToRepo.put(reportPath.toAbsolutePath().normalize(),
                                        localRepoPath.toAbsolutePath().normalize());
                            }
                        }
                    }
                }
            }
        }
        return reportToRepo;
    }

    private static List<File> getFiles(List<String> files, File file) {
        File[] children = file.listFiles();
        if (children == null) {
            return List.of();
        }
        List<File> javaFiles = List.of(children);
        List<File> toGenerate = new ArrayList<>();
        for (File f : javaFiles) {
            if (f.isDirectory()) {
                toGenerate.addAll(getFiles(files, f));
            } else if (files.contains(f.getName())) {
                toGenerate.add(f);
            }
        }
        return toGenerate;
    }

    private static Path generateReport(String packageName,
                                       Path path,
                                       Assignment assignment,
                                       List<File> files,
                                       boolean runCheckStyle) {
        Path report = Paths.get(path.toString(), assignment.getShortName() + packageName + ".html");
        try (PrintWriter pw = new PrintWriter(report.toFile())) {
            pw.println(generateHeader(packageName, assignment));
            renderRubric(pw, assignment.getRubric());
            for (File f : files) {
                pw.print("# ");
                pw.println(f.getName());
                pw.println();
                pw.println("```");
                if (runCheckStyle) {
                    String checkStyle = generateCheckStyle(f);
                    if (checkStyle.contains("ERROR")) {
                        pw.println(addCheckstyleComments(checkStyle));
                    }
                }
                try (Scanner in = new Scanner(f)) {
                    while (in.hasNextLine()) {
                        pw.println(in.nextLine());
                    }
                    pw.println("```\n");
                }
            }
            pw.println("</xmp><script type=\"text/javascript\" " +
                    "src=\"https://csse.msoe.us/gradedown.js\"></script></body></html>");
            return report;
        } catch (IOException | InterruptedException e) {
            System.err.println("Could not write file: " + path.getFileName());
            System.err.println(e.getMessage());
            return null;
        }
    }

    private static String generateHeader(String packageName,
                                         Assignment assignment) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>" +
                "<title>" + packageName + "</title>" +
                "</head><body><xmp>\n" +
                "# " + assignment.getFullName() + "\n\n";
    }

    private static void renderRubric(PrintWriter pw, Rubric rubric) {
        final int rubricLineWidth = 76;
        pw.println(">> | Earned | Possible | Criteria                                          |");
        pw.println(">> | ------ | -------- | ------------------------------------------------- |");
        for (RubricItem item : rubric.getItems()) {
            String startingPointsEarned = String.format("%3d", item.getPoints());
            String possible = String.format("%4d", item.getPoints());
            String prefix = String.format(
                    ">> |  %s   |   %s   | ",
                    startingPointsEarned,
                    possible
            );
            int remaining =
                    rubricLineWidth - prefix.length() - 2; // trailing " |"
            String criteria = item.getDescription();
            if (criteria.length() > remaining) {
                criteria = criteria.substring(0, remaining);
            }
            criteria = String.format("%-" + remaining + "s", criteria);
            pw.println(prefix + criteria + " |");
        }
        pw.println(">");
    }

    /* Checkstyle helpers */
    private static String generateCheckStyle(File file) throws IOException, InterruptedException {
        String config = "-c https://csse.msoe.us/csc1110/MSOE_checkStyle.xml";
        Path jar = Paths.get("bin", "lib", "checkstyle-10.23.1-all.jar");
        if (!Files.exists(jar)) {
            throw new IOException("Missing checkstyle jar: " + jar.toAbsolutePath());
        }
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                jar.toString(),
                config,
                file.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = in.readLine()) != null) {
            sb.append(s).append("\n");
        }
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("Checkstyle failed (exit=" + exit + ")\n" + sb);
        }
        return sb.toString();
    }

    private static String addCheckstyleComments(String report) {
        final int maxPoints = 15;
        int count = 0;
        StringBuilder sb = new StringBuilder();
        try (Scanner in = new Scanner(report)) {
            while (in.hasNextLine()) {
                String s = in.nextLine();
                if (s.contains("ERROR")) {
                    ++count;
                    int index = s.lastIndexOf(File.separator);
                    sb.append("> * ").append(s.substring(index + 1)).append("\n");
                }
            }
        }
        count = Math.min(count, maxPoints);
        return "```\n> #### -" + count + " CheckStyle Error\n" + sb + "\n```\n";
    }

    /* Git publishing helpers */
    /**
     * Publishes a feedback report file into a student's repository and pushes the update to the
     * remote.
     *
     * <p>This method:
     * <ol>
     *   <li>Validates that {@code repoDir} is a Git repository (contains {@code .git})</li>
     *   <li>Validates that {@code reportFileInRepo} is inside {@code repoDir}</li>
     *   <li>Runs {@code git pull --rebase}</li>
     *   <li>Stages the report file</li>
     *   <li>Commits and pushes the change only if there are staged changes</li>
     * </ol>
     *
     * <p>If there are no staged changes (i.e., the report did not change), the method performs
     * no commit and no push.
     *
     * @param repoDir the root directory of the student's local Git repository
     * @param reportFileInRepo the report file path inside that repository
     * @throws NullPointerException if {@code repoDir} or {@code reportFileInRepo} is {@code null}
     * @throws IOException if validation fails, Git commands fail, or I/O errors occur
     * @throws InterruptedException if interrupted while waiting for Git commands to complete
     */
    public static void publishFeedbackReport(Path repoDir, Path reportFileInRepo)
            throws IOException, InterruptedException {
        Objects.requireNonNull(repoDir, "repoDir");
        Objects.requireNonNull(reportFileInRepo, "reportFileInRepo");
        Path normalizedRepo = repoDir.toAbsolutePath().normalize();
        Path normalizedReport = reportFileInRepo.toAbsolutePath().normalize();
        if (!normalizedReport.startsWith(normalizedRepo)) {
            throw new IOException("Report file is not inside repo: " + normalizedReport);
        }
        if (!Files.isDirectory(repoDir.resolve(".git"))) {
            throw new IOException("Not a git repository: " + repoDir);
        }
        runGit(repoDir, "pull", "--rebase");
        Path rel = normalizedRepo.relativize(normalizedReport);
        runGit(repoDir, "add", rel.toString());
        boolean hasStagedChanges;
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached", "--quiet");
        pb.directory(repoDir.toFile());
        Process p = pb.start();
        int exit = p.waitFor();
        hasStagedChanges = exit != 0;
        if (hasStagedChanges) {
            runGit(repoDir, "commit", "-m", "Add/update feedback report");
            runGit(repoDir, "push");
        }
    }

    private static void runGit(Path repoDir, String... args)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(repoDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            String output = new String(p.getInputStream().readAllBytes());
            throw new IOException("Git command failed in " + repoDir + ": git "
                    + String.join(" ", args) + "\n" + output);
        }
    }
}