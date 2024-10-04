/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 */
package edu.msoe.csse.jones;

import javafx.collections.ObservableList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Utilities for GitHub Classroom grading
 */
public class Utilities {
    /**
     * Pulls all the repositories from the given link
     * @param gitHubLink the link to the repositories
     * @param target the directory to place the repositories
     * @throws IOException thrown if the directory could not be accessed
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
     * @param filePath the path to the repositories
     * @param ignored the list of files to exclude
     */
    public static void extractPackages(Path filePath, List<String> ignored) {
        final int maxDepth = 4;
        try {
            Path submissions = Paths.get(filePath.toString(), "submissions/");
            if (!submissions.toFile().exists()) {
                Files.createDirectory(submissions);
            }
            File file = submissions.toFile();
            if(!file.setReadable(true)) {
                System.err.println("Could not set read permissions");
            }
            if(!file.setWritable(true)) {
                System.err.println("Could not set write permissions");
            }
            if(!file.setExecutable(true)) {
                System.err.println("Could not set execute permissions");
            }

            Files.walkFileTree(filePath,
                    EnumSet.noneOf(FileVisitOption.class),
                    maxDepth,
                    new SimpleFileVisitor<>() {
                        public FileVisitResult preVisitDirectory(Path dir,
                                                                 BasicFileAttributes attrs)
                                throws IOException {
                            // Check if the directory is a "src" folder.
                            if (dir.getFileName().toString().equals("src")) {
                                // Copy all folders except exclusions
                                copyFoldersExcept(dir, submissions, ignored.toArray(new String[0]));
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            System.err.println("Could not write files");
        }
    }

    private static void copyFoldersExcept(Path srcDir,
                                          Path destinationDir,
                                          String... excludeFolders)
            throws IOException {
        Files.walkFileTree(srcDir, EnumSet.noneOf(FileVisitOption.class),
                Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir,
                                                             BasicFileAttributes attrs)
                            throws IOException {
                        // Check if the current directory should be excluded.
                        for (String excludeFolder : excludeFolders) {
                            if (dir.endsWith(excludeFolder)) {
                                // Skip this directory and its contents.
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        }

                        // Calculate the corresponding path in the "submissions" directory.
                        Path relativePath = srcDir.relativize(dir);
                        Path destPath = destinationDir.resolve(relativePath);

                        // Copy the directory to the "submissions" directory.
                        Files.createDirectories(destPath);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file,
                                                     BasicFileAttributes attrs)
                            throws IOException {
                        // Calculate the corresponding path in the "submissions" directory.
                        Path relativePath = srcDir.relativize(file);
                        Path destPath = destinationDir.resolve(relativePath);

                        // Copy the file to the "submissions" directory.
                        Files.copy(file, destPath);

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
            for(File f : files) {
                pw.println("import " + f.getName() + ".*;");
            }
        } catch(IOException e) {
            System.err.println("Could not write imports.txt");
        }
    }

    /**
     * Generates HTML reports for student feedback
     *
     * @param filePath the path to the submissions directory
     * @param files the files to add to the report
     * @param shortName the short name of the assignment, used for feedback file names
     * @param fullName the name of the assignment
     * @param header the header for the reports
     * @param checkStyle if true, run checkStyle report, otherwise do not
     */
    public static void generateReports(Path filePath,
                                       ObservableList<String> files,
                                        String shortName,
                                        String fullName,
                                        Path header,
                                       boolean checkStyle) {
        String[] directories = filePath.toFile().list((current, name) ->
                new File(current, name).isDirectory());
        Path feedback = Paths.get(filePath.toString(), "feedback");
        if (!feedback.toFile().exists()) {
            if (feedback.toFile().mkdir()) {
                System.out.println("feedback folder generated");
            } else {
                System.out.println("using existing feedback folder");
            }
        }
        if (directories != null) {
            for (String dir : directories) {
                File file = new File(filePath + File.separator + dir + File.separator);
                List<File> toGenerate = getFiles(files, file);
                generateReport(dir, feedback, shortName, fullName, toGenerate, header, checkStyle);
            }
        }
    }

    private static List<File> getFiles(List<String> files, File file) {
        List<File> javaFiles = List.of(Objects.requireNonNull(file.listFiles()));
        List<File> toGenerate = new ArrayList<>();
        for (File f : javaFiles) {
            if(f.isDirectory()) {
                toGenerate.addAll(getFiles(files, f));
            } else if (files.contains(f.getName())) {
                toGenerate.add(f);
            }
        }
        return toGenerate;
    }

    private static void generateReport(String student,
                                       Path path,
                                       String prefix,
                                       String assignmentName,
                                       List<File> files,
                                       Path headerPath,
                                       boolean runCheckStyle) {
        Path report = Paths.get(path.toString(), prefix + student + ".html");
        try (PrintWriter pw = new PrintWriter(report.toFile())) {
            String header = Files.readString(headerPath);
            header = header.replace("##STUDENT##", student);
            header = header.replace("##FULLNAME##", assignmentName);
            pw.println(header);
            for (File f : files) {
                pw.print("# ");
                pw.println(f.getName());
                pw.println();
                pw.println("```");
                if(runCheckStyle) {
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
        } catch(IOException e) {
            System.err.println("Could not write header");
        } catch(InterruptedException e) {
            System.err.println("Could not run Checkstyle");
        }
    }

    /**
     * Generates CheckStyle Comments in the reports
     * @param file the file to check for errors
     * @return the CheckStyle report
     * @throws IOException thrown if the file could not be accessed
     * @throws InterruptedException thrown if CheckStyle could not be run
     */
    private static String generateCheckStyle(File file) throws IOException, InterruptedException {
        String config = "-c https://csse.msoe.us/csc1110/MSOE_checkStyle.xml";
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                Paths.get("bin", "checkstyle-10.9.2-all.jar").toString(),
                config,
                file.getAbsolutePath());
        Process p = pb.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        StringBuilder sb = new StringBuilder();
        while((s = in.readLine()) != null){
            sb.append(s).append("\n");
        }
        p.waitFor();
        return sb.toString();
    }

    private static String addCheckstyleComments(String report) {
        final int maxPoints = 15;
        int count = 0;
        StringBuilder sb = new StringBuilder();
        try(Scanner in = new Scanner(report)) {
            while(in.hasNextLine()) {
                String s = in.nextLine();
                if(s.contains("ERROR")) {
                    ++count;
                    int index = s.lastIndexOf(File.separator);
                    sb.append("> * ").append(s.substring(index + 1)).append("\n");
                }
            }
        }
        count = Math.min(count, maxPoints);
        return "```\n> #### -" + count + " CheckStyle Error\n" + sb + "\n```\n";
    }
}
