package org.realityforge.bazel.depgen.gen;

import gir.io.FileUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
      final Path file = FileUtil.getCurrentDirectory().resolve( "file.bzl" );
      final StarlarkFileOutput output = new StarlarkFileOutput( file );
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
      output.close();

      final String content = new String( Files.readAllBytes( file ), StandardCharsets.US_ASCII );
      assertEquals( content,
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
}
