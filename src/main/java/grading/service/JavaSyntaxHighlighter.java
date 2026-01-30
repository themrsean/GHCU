/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package grading.service;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that computes Java syntax highlighting spans for RichTextFX.
 * <p>
 * This class tokenizes Java source code using regular expressions and produces
 * {@link StyleSpans} containing CSS style class names for keywords, strings,
 * comments, numbers, and annotations.
 * <p>
 * This is a pure utility class and cannot be instantiated.
 *
 * @author Sean Jones
 */
public final class JavaSyntaxHighlighter {
    /* ---------------- Keywords ---------------- */
    private static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };
    /* ---------------- Regex Pattern ---------------- */
    private static final Pattern JAVA_PATTERN = Pattern.compile(
            "(?<KEYWORD>\\b(" + String.join("|", KEYWORDS) + ")\\b)"
                    + "|(?<STRING>\"([^\"\\\\]|\\\\.)*\")"
                    + "|(?<COMMENT>//[^\\n]*|/\\*(?:.|\\R)*?\\*/)"
                    + "|(?<NUMBER>\\b\\d+\\b)"
                    + "|(?<ANNOTATION>@\\w+)"
    );

    private JavaSyntaxHighlighter() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated.");
    }

    /**
     * Computes syntax highlighting spans for the given Java source text.
     * <p>
     * This method scans the input text using a compiled regular expression and
     * generates {@link StyleSpans} suitable for RichTextFX {@code CodeArea}
     * styling. Each matched token is assigned a single CSS style class name:
     * <ul>
     *     <li>{@code keyword} for Java reserved words</li>
     *     <li>{@code string} for string literals</li>
     *     <li>{@code comment} for line and block comments</li>
     *     <li>{@code number} for integer literals</li>
     *     <li>{@code annotation} for annotation identifiers</li>
     * </ul>
     * Regions of text that do not match any token are returned with no styles.
     *
     * @param text Java source code text to highlight
     * @return a {@link StyleSpans} object that covers the entire input text
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spans =
                new StyleSpansBuilder<>();
        Matcher matcher = JAVA_PATTERN.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            spans.add(
                    Collections.emptyList(),
                    matcher.start() - lastEnd
            );
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("STRING") != null ? "string" :
                                    matcher.group("COMMENT") != null ? "comment" :
                                            matcher.group("NUMBER") != null ? "number" :
                                                    matcher.group("ANNOTATION") != null
                                                            ? "annotation" : null;
            spans.add(
                    styleClass == null
                            ? Collections.emptyList()
                            : Collections.singleton(styleClass),
                    matcher.end() - matcher.start()
            );
            lastEnd = matcher.end();
        }
        spans.add(
                Collections.emptyList(),
                text.length() - lastEnd
        );
        return spans.create();
    }
}