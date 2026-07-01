package org.realityforge.bazel.depgen.util;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;

public class StarlarkOutputTest extends AbstractTest {
    @Test
    public void basicOperation() throws Exception {
        final Path file = writeFileContent(output -> {
            output.write("A");
            output.write("B");
            output.newLine();
            output.incIndent();
            output.write("C");
            output.incIndent();
            output.write("D");
            output.write("E");
            output.incIndent();
            output.write("F");
            output.decIndent();
            output.write("G");
            output.incIndent();
            output.write("H");
            output.decIndent();
            output.write("I");
            output.decIndent();
            output.decIndent();
            output.write("J");
        });

        assertFileContent(file, """
            A
            B

                C
                    D
                    E
                        F
                    G
                        H
                    I
            J
            """);
    }

    @Test
    public void writeCall_emptyFunction() throws Exception {
        final Path file = writeFileContent(output -> output.writeCall("myFunction", new LinkedHashMap<>()));

        assertFileContent(file, "myFunction()\n");
    }

    @Test
    public void writeCall_indent() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            output.writeCall("myFunction", new LinkedHashMap<>());
            output.decIndent();
        });

        assertFileContent(file, "    myFunction()\n");
    }

    @Test
    public void writeCall_singleArg() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            final var arguments = new LinkedHashMap<String, Object>();
            arguments.put("name", "'Foo'");
            output.writeCall("myFunction", arguments);
            output.decIndent();
        });

        assertFileContent(file, """
                myFunction(
                    name = 'Foo',
                )
            """);
    }

    @Test
    public void writeCall_singleArrayArg() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            final var arguments = new LinkedHashMap<String, Object>();
            arguments.put("name", Arrays.asList("1", "2", "3"));
            output.writeCall("myFunction", arguments);
            output.decIndent();
        });

        assertFileContent(file, """
                myFunction(
                    name = [
                        1,
                        2,
                        3,
                    ],
                )
            """);
    }

    @Test
    public void writeCall_singleMultiValueArrayArg() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            final var arguments = new LinkedHashMap<String, Object>();
            arguments.put("name", Arrays.asList("1", "2", "3"));
            output.writeCall("myFunction", arguments);
            output.decIndent();
        });

        assertFileContent(file, """
                myFunction(
                    name = [
                        1,
                        2,
                        3,
                    ],
                )
            """);
    }

    @Test
    public void writeCall_multiArg() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            final var arguments = new LinkedHashMap<String, Object>();
            arguments.put("name", "'com_biz__myartifact'");
            arguments.put("actual", "':com_biz__myartifact_42'");
            arguments.put("visibility", Collections.singletonList("'//visibility:public'"));
            arguments.put("make_peace", Boolean.TRUE);
            arguments.put("make_war", Boolean.FALSE);
            output.writeCall("myFunction", arguments);
            output.decIndent();
        });

        assertFileContent(file, """
                myFunction(
                    name = 'com_biz__myartifact',
                    actual = ':com_biz__myartifact_42',
                    visibility = ['//visibility:public'],
                    make_peace = True,
                    make_war = False,
                )
            """);
    }

    @Test
    public void writeMultilineComment() throws Exception {
        final Path file = writeFileContent(output -> output.writeMultilineComment(o -> o.write("Some comment")));
        assertFileContent(file, """
            ""\"
                Some comment
            ""\"
            """);
    }

    @Test
    public void writeIfCondition() throws Exception {
        final Path file =
                writeFileContent(output -> output.writeIfCondition("not someCondition", o -> o.write("someCall()")));

        assertFileContent(file, "if not someCondition:\n    someCall()\n");
    }

    @Test
    public void writeMacro_noArgs() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            output.writeMacroStart("myMacro", Collections.emptyList());
            output.decIndent();
        });

        assertFileContent(file, "    def myMacro():\n");
    }

    @Test
    public void writeMacro() throws Exception {
        final Path file =
                writeFileContent(output -> output.writeMacro("myMacro", Collections.singletonList("foo"), o -> {
                    o.write("bar()");
                    o.write("baz()");
                }));

        assertFileContent(file, """
            def myMacro(foo):
                bar()
                baz()
            """);
    }

    @Test
    public void writeMacro_singleArg() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            output.writeMacroStart("myMacro", Collections.singletonList("foo"));
            output.decIndent();
        });

        assertFileContent(file, "    def myMacro(foo):\n");
    }

    @Test
    public void writeMacro_multipleArgs() throws Exception {
        final Path file = writeFileContent(output -> {
            output.incIndent();
            output.writeMacroStart("myMacro", Arrays.asList("foo", "bar = True", "baz = \"yes\""));
            output.decIndent();
        });

        assertFileContent(file, """
                def myMacro(
                        foo,
                        bar = True,
                        baz = "yes"):
            """);
    }

    @FunctionalInterface
    interface WriterCallback {
        void process(@NonNull StarlarkOutput output) throws Exception;
    }

    @NonNull
    private Path writeFileContent(@NonNull final WriterCallback callback) throws Exception {
        final Path file = FileUtil.createLocalTempDir().resolve("file.bzl");
        final var output = new StarlarkOutput(file);
        callback.process(output);
        output.close();
        return file;
    }

    private void assertFileContent(@NonNull final Path file, @NonNull final String expected) throws Exception {
        final var content = Files.readString(file, StandardCharsets.US_ASCII);
        assertEquals(content, expected);
    }
}
