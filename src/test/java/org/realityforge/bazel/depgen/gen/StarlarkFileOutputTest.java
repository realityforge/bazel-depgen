package org.realityforge.bazel.depgen.gen;

import gir.io.FileUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class StarlarkFileOutputTest
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
    } );
  }

  @FunctionalInterface
  interface WriterCallback
  {
    void process( @Nonnull StarlarkFileOutput output )
      throws Exception;
  }

  @Nonnull
  private Path writeFileContent( @Nonnull final WriterCallback callback )
    throws Exception
  {
    final Path file = FileUtil.createLocalTempDir().resolve( "file.bzl" );
    final StarlarkFileOutput output = new StarlarkFileOutput( file );
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
