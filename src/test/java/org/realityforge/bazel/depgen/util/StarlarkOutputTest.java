package org.realityforge.bazel.depgen.util;

import gir.io.FileUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class StarlarkOutputTest
  extends AbstractTest
{
  @Test
  public void basicOperation()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.write( "A" );
          output.write( "B" );
          output.newLine();
          output.incIndent();
          output.write( "C" );
          output.incIndent();
          output.write( "D" );
          output.write( "E" );
          output.incIndent();
          output.write( "F" );
          output.decIndent();
          output.write( "G" );
          output.incIndent();
          output.write( "H" );
          output.decIndent();
          output.write( "I" );
          output.decIndent();
          output.decIndent();
          output.write( "J" );
        } );

      assertFileContent( file,
                         "A\n" +
                         "B\n" +
                         "\n" +
                         "    C\n" +
                         "        D\n" +
                         "        E\n" +
                         "            F\n" +
                         "        G\n" +
                         "            H\n" +
                         "        I\n" +
                         "J\n" );
    } );
  }

  @Test
  public void writeCall_emptyFunction()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> output.writeCall( "myFunction", new LinkedHashMap<>() ) );

      assertFileContent( file, "myFunction()\n" );
    } );
  }

  @Test
  public void writeCall_indent()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          output.writeCall( "myFunction", new LinkedHashMap<>() );
          output.decIndent();
        } );

      assertFileContent( file, "    myFunction()\n" );
    } );
  }

  @Test
  public void writeCall_singleArg()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
          arguments.put( "name", "'Foo'" );
          output.writeCall( "myFunction", arguments );
          output.decIndent();
        } );

      assertFileContent( file,
                         "    myFunction(\n" +
                         "        name = 'Foo',\n" +
                         "    )\n" );
    } );
  }

  @Test
  public void writeCall_singleArrayArg()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
          arguments.put( "name", Arrays.asList( "1", "2", "3" ) );
          output.writeCall( "myFunction", arguments );
          output.decIndent();
        } );

      assertFileContent( file,
                         "    myFunction(\n" +
                         "        name = [\n" +
                         "            1,\n" +
                         "            2,\n" +
                         "            3,\n" +
                         "        ],\n" +
                         "    )\n" );
    } );
  }

  @Test
  public void writeCall_singleMultiValueArrayArg()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
          arguments.put( "name", Arrays.asList( "1", "2", "3" ) );
          output.writeCall( "myFunction", arguments );
          output.decIndent();
        } );

      assertFileContent( file,
                         "    myFunction(\n" +
                         "        name = [\n" +
                         "            1,\n" +
                         "            2,\n" +
                         "            3,\n" +
                         "        ],\n" +
                         "    )\n" );
    } );
  }

  @Test
  public void writeCall_multiArg()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
          arguments.put( "name", "'com_biz__myartifact'" );
          arguments.put( "actual", "':com_biz__myartifact_42'" );
          arguments.put( "visibility", Collections.singletonList( "'//visibility:public'" ) );
          output.writeCall( "myFunction", arguments );
          output.decIndent();
        } );

      assertFileContent( file,
                         "    myFunction(\n" +
                         "        name = 'com_biz__myartifact',\n" +
                         "        actual = ':com_biz__myartifact_42',\n" +
                         "        visibility = ['//visibility:public'],\n" +
                         "    )\n" );
    } );
  }

  @Test
  public void writeMultilineComment()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = writeFileContent( output -> output.writeMultilineComment( o -> o.write( "Some comment" ) ) );
      assertFileContent( file, "\"\"\"\n" +
                               "    Some comment\n" +
                               "\"\"\"\n" );
    } );
  }

  @Test
  public void writeIfCondition()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> output.writeIfCondition( "not someCondition", o -> o.write( "someCall()" ) ) );

      assertFileContent( file, "if not someCondition:\n    someCall()\n" );
    } );
  }

  @Test
  public void writeMacro_noArgs()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          output.writeMacroStart( "myMacro", Collections.emptyList() );
          output.decIndent();
        } );

      assertFileContent( file, "    def myMacro():\n" );
    } );
  }

  @Test
  public void writeMacro()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> output.writeMacro( "myMacro", Collections.singletonList( "foo" ), o -> {
          o.write( "bar()" );
          o.write( "baz()" );
        } ) );

      assertFileContent( file, "def myMacro(foo):\n" +
                               "    bar()\n" +
                               "    baz()\n" );
    } );
  }

  @Test
  public void writeMacro_singleArg()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          output.writeMacroStart( "myMacro", Collections.singletonList( "foo" ) );
          output.decIndent();
        } );

      assertFileContent( file, "    def myMacro(foo):\n" );
    } );
  }

  @Test
  public void writeMacro_multipleArgs()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file =
        writeFileContent( output -> {
          output.incIndent();
          output.writeMacroStart( "myMacro", Arrays.asList( "foo", "bar = True", "baz = \"yes\"" ) );
          output.decIndent();
        } );

      assertFileContent( file,
                         "    def myMacro(\n" +
                         "            foo,\n" +
                         "            bar = True,\n" +
                         "            baz = \"yes\"):\n" );
    } );
  }

  @FunctionalInterface
  interface WriterCallback
  {
    void process( @Nonnull StarlarkOutput output )
      throws Exception;
  }

  @Nonnull
  private Path writeFileContent( @Nonnull final WriterCallback callback )
    throws Exception
  {
    final Path file = FileUtil.createLocalTempDir().resolve( "file.bzl" );
    final StarlarkOutput output = new StarlarkOutput( file );
    callback.process( output );
    output.close();
    return file;
  }

  private void assertFileContent( @Nonnull final Path file, @Nonnull final String expected )
    throws Exception
  {
    final String content = new String( Files.readAllBytes( file ), StandardCharsets.US_ASCII );
    assertEquals( content, expected );
  }
}
