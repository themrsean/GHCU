/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package grading.service;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    + "|(?<COMMENT>//[^\\n]*|/\\*(.|\\R)*?\\*/)"
                    + "|(?<NUMBER>\\b\\d+\\b)"
                    + "|(?<ANNOTATION>@\\w+)"
    );

    private JavaSyntaxHighlighter() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated.");
    }

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
                    Collections.singleton(styleClass),
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