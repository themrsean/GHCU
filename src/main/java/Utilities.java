/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package main.java;

import main.java.model.Assignment;
import main.java.model.Rubric;
import main.java.model.RubricItem;

import javax.annotation.Nonnull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Utilities for GitHub Classroom grading
 */
public class Utilities {
    /**
     * Pulls all the repositories from the given link
     *
     * @param gitHubLink the link to the repositories
     * @param target     the directory to place the repositories
     * @throws IOException          thrown if the directory could not be accessed
     * @throws InterruptedException thrown if the process is interrupted
     */
    public static void pullRepositories(String gitHubLink, Path target) throws IOException,
            InterruptedException {
        String[] split = gitHubLink.split(" ");
        ProcessBuilder pb = new ProcessBuilder(split);
        pb.directory(target.toFile());
        Process p = pb.start();
        p.waitFor();
    }

    /**
     * Extracts packages from GitHub repos
     *
     * @param root    the path to the repositories
     * @param ignored the list of files to exclude
     * @throws IOException if a read error is encountered
     */
    public static void extractPackages(Path root, List<String> ignored) throws IOException {
        Path submissions = root.resolve("submissions");
        Files.createDirectories(submissions);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            @Nonnull
            public FileVisitResult preVisitDirectory(@Nonnull Path dir,
                                                     @Nonnull BasicFileAttributes attrs)
                    throws IOException {
                if (dir.getFileName().toString().equals("src")) {
                    copySrcDirectory(dir, submissions, ignored);
                    return FileVisitResult.SKIP_SUBTREE; // important
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
            @Nonnull
            public FileVisitResult preVisitDirectory(@Nonnull Path dir,
                                                     @Nonnull BasicFileAttributes attrs)
                    throws IOException {
                if (ignored.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path target = targetRoot.resolve(src.relativize(dir));
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            @Nonnull
            public FileVisitResult visitFile(@Nonnull Path file,
                                             @Nonnull BasicFileAttributes attrs)
                    throws IOException {
                if (!ignored.contains(file.getFileName().toString())) {
                    Path target = targetRoot.resolve(src.relativize(file));
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Generates a list of imports for grading
     *
     * @param path the directory to find packages in
     */
    public static void generateImports(Path path) {
        try (PrintWriter pw = new PrintWriter(
                Paths.get(path.toString(), "imports.txt").toFile())) {
            File[] imports = path.toFile().listFiles();
            List<File> files = new ArrayList<>();
            if (imports != null) {
                for (File f : imports) {
                    if (f != null && f.isDirectory()) {
                        files.add(f);
                    }
                }
            }
            files.sort(Comparator.comparing(File::getName));
            for (File f : files) {
                pw.println("import " + f.getName() + ".*;");
            }
        } catch (IOException e) {
            System.err.println("Could not write imports.txt");
        }
    }

    public static void generateReports(Path submissions,
                                       Assignment assignment,
                                       boolean checkStyle)
            throws IOException {
        File[] repos = submissions.toFile().listFiles(File::isDirectory);
        if (repos != null) {
            Path feedback = submissions.resolve("feedback");
            Files.createDirectories(feedback);
            for (File repo : repos) {
                File[] packages = repo.listFiles(File::isDirectory);
                if (packages != null) {
                    for (File pkg : packages) {
                        List<File> files =
                                getFiles(assignment.getFiles(), pkg);
                        if (!files.isEmpty()) {
                            generateReport(
                                    pkg.getName(),
                                    feedback,
                                    assignment,
                                    files,
                                    checkStyle
                            );
                        }
                    }
                }
            }
        }
    }


    private static List<File> getFiles(List<String> files, File file) {
        List<File> javaFiles = List.of(Objects.requireNonNull(file.listFiles()));
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

    private static void generateReport(String student,
                                       Path path,
                                       Assignment assignment,
                                       List<File> files,
                                       boolean runCheckStyle) {
        Path report = Paths.get(path.toString(), assignment.getShortName() + student + ".html");
        try (PrintWriter pw = new PrintWriter(report.toFile())) {
            pw.println(generateHeader(student, assignment));
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
        } catch (FileNotFoundException e) {
            System.err.println("Could not write file: " + path.getFileName());
        } catch (IOException e) {
            System.err.println("Could not write header");
        } catch (InterruptedException e) {
            System.err.println("Could not run Checkstyle");
        }
    }

    /**
     * Generates CheckStyle Comments in the reports
     *
     * @param file the file to check for errors
     * @return the CheckStyle report
     * @throws IOException          thrown if the file could not be accessed
     * @throws InterruptedException thrown if CheckStyle could not be run
     */
    private static String generateCheckStyle(File file) throws IOException, InterruptedException {
        String config = "-c https://csse.msoe.us/csc1110/MSOE_checkStyle.xml";
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                Paths.get("bin", "lib/checkstyle-10.23.1-all.jar").toString(),
                config,
                file.getAbsolutePath());
        Process p = pb.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = in.readLine()) != null) {
            sb.append(s).append("\n");
        }
        p.waitFor();
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

    private static void renderRubric(PrintWriter pw, Rubric rubric) {
        final int rubricLineWidth = 76;
        pw.println(">> | Earned | Possible | Criteria                                     |");
        pw.println(">> | ------ | -------- | -------------------------------------------- |");

        for (RubricItem item : rubric.getItems()) {
            String earned = String.format("%3d", item.getPoints());
            String possible = String.format("%4d", item.getPoints());
            String prefix = String.format(
                    ">> |  %s   |   %s   | ",
                    earned,
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

    private static String generateHeader(String student,
                                         Assignment assignment) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>" +
                "<title>" + student + "</title>" +
                "</head><body><xmp>\n" +
                "# " + assignment.getFullName() + "\n\n";
    }
}
