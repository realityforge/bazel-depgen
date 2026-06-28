package org.realityforge.bazel.depgen.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public final class StarlarkOutput implements AutoCloseable {
    @FunctionalInterface
    public interface Block {
        void call(@NonNull StarlarkOutput output) throws IOException;
    }

    @NonNull
    private final OutputStream _outputStream;

    private int _indent;

    public StarlarkOutput(@NonNull final Path extensionFile) throws IOException {
        this(Files.newOutputStream(extensionFile.toFile().toPath()));
    }

    public StarlarkOutput(@NonNull final OutputStream outputStream) {
        _outputStream = Objects.requireNonNull(outputStream);
    }

    public void write(@NonNull final String line) throws IOException {
        for (int i = 0; i < _indent; i++) {
            emit("    ");
        }
        emit(line);
        newLine();
    }

    public void newLine() throws IOException {
        emit("\n");
    }

    public void writeMultilineComment(@NonNull final Block body) throws IOException {
        write("\"\"\"");
        incIndent();
        body.call(this);
        decIndent();
        write("\"\"\"");
    }

    public void writeIfCondition(@NonNull final String condition, @NonNull final Block body) throws IOException {
        write("if " + condition + ":");
        incIndent();
        body.call(this);
        decIndent();
    }

    public void writeMacro(@NonNull final String name, @NonNull final List<String> arguments, @NonNull final Block body)
            throws IOException {
        writeMacroStart(name, arguments);
        incIndent();
        body.call(this);
        decIndent();
    }

    void writeMacroStart(@NonNull final String name, @NonNull final List<String> arguments) throws IOException {
        final int size = arguments.size();
        if (0 == size) {
            write("def " + name + "():");
        } else if (1 == size) {
            write("def " + name + "(" + arguments.get(0) + "):");
        } else {
            write("def " + name + "(");
            incIndent();
            incIndent();
            int index = 0;
            for (final String argument : arguments) {
                index++;
                write(argument + (index == size ? "):" : ","));
            }
            decIndent();
            decIndent();
        }
    }

    public void writeCall(@NonNull final String functionName, @NonNull final LinkedHashMap<String, Object> arguments)
            throws IOException {
        if (arguments.isEmpty()) {
            write(functionName + "()");
        } else {
            write(functionName + "(");
            incIndent();
            for (final Map.Entry<String, Object> entry : arguments.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (null == value) {
                    write(key + " = None,");
                } else if (Boolean.TRUE == value) {
                    write(key + " = True,");
                } else if (Boolean.FALSE == value) {
                    write(key + " = False,");
                } else if (value instanceof List) {
                    final var arg = (List<?>) value;
                    if (arg.isEmpty()) {
                        write(key + " = [],");
                    } else if (1 == arg.size()) {
                        write(key + " = [" + arg.get(0) + "],");
                    } else {
                        write(key + " = [");
                        incIndent();
                        for (final Object innerValue : arg) {
                            write(innerValue + ",");
                        }
                        decIndent();
                        write("],");
                    }
                } else {
                    write(key + " = " + value + ",");
                }
            }
            decIndent();
            write(")");
        }
    }

    public void incIndent() {
        _indent++;
    }

    public void decIndent() {
        _indent--;
        assert _indent >= 0;
    }

    @Override
    public void close() throws IOException {
        _outputStream.close();
    }

    private void emit(@NonNull final String string) throws IOException {
        _outputStream.write(string.getBytes(StandardCharsets.US_ASCII));
    }
}
