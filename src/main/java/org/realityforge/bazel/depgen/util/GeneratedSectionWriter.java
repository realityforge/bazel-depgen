package org.realityforge.bazel.depgen.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.DepgenException;

public final class GeneratedSectionWriter {
    private GeneratedSectionWriter() {}

    public static void replaceSection(
            @NonNull final Path path,
            @NonNull final String startToken,
            @NonNull final String endToken,
            @NonNull final String content)
            throws IOException {
        if (!Files.exists(path)) {
            throw new DepgenException("Expected generated output destination file to exist. File: " + path);
        }

        final var data = Files.readString(path, StandardCharsets.UTF_8);
        final var startIndex = data.indexOf(startToken);
        if (-1 == startIndex) {
            throw new DepgenException("Expected generated output destination file to contain start token '" + startToken
                    + "'. File: " + path);
        }
        final var endIndex = data.indexOf(endToken, startIndex + startToken.length());
        if (-1 == endIndex) {
            throw new DepgenException("Expected generated output destination file to contain end token '" + endToken
                    + "' after the start token. File: " + path);
        }

        final var sb = new StringBuilder();
        sb.append(data, 0, startIndex + startToken.length());
        sb.append("\n\n").append(content);
        if (!content.endsWith("\n")) {
            sb.append("\n");
        }
        sb.append("\n");
        sb.append(data.substring(endIndex));
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    public static boolean ensureSectionExists(
            @NonNull final Path path, @NonNull final String startToken, @NonNull final String endToken)
            throws IOException {
        if (!Files.exists(path)) {
            throw new DepgenException("Expected generated output destination file to exist. File: " + path);
        }

        final var data = Files.readString(path, StandardCharsets.UTF_8);
        final var hasStartToken = data.contains(startToken);
        final var hasEndToken = data.contains(endToken);
        if (hasStartToken && hasEndToken) {
            return false;
        } else if (hasStartToken || hasEndToken) {
            throw new DepgenException("Expected generated output destination file to either contain both markers or "
                    + "neither marker. File: " + path);
        }

        final var sb = new StringBuilder(data);
        if (!data.isEmpty() && !data.endsWith("\n")) {
            sb.append("\n");
        }
        if (!data.isEmpty()) {
            sb.append("\n");
        }
        sb.append(startToken).append("\n\n").append(endToken).append("\n");
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        return true;
    }
}
