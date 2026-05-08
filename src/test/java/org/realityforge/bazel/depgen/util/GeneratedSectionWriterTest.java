package org.realityforge.bazel.depgen.util;

import gir.io.FileUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.DepgenException;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class GeneratedSectionWriterTest
  extends AbstractTest
{
  @Test
  public void replaceSection()
    throws Exception
  {
    final Path file = FileUtil.getCurrentDirectory().resolve( "MODULE.bazel" );
    FileUtil.write( file,
                    "alpha\n" +
                    "# start\n" +
                    "old\n" +
                    "# end\n" +
                    "omega\n" );

    GeneratedSectionWriter.replaceSection( file, "# start", "# end", "new\ncontent\n" );

    assertEquals( readFile( file ),
                  "alpha\n" +
                  "# start\n" +
                  "\n" +
                  "new\n" +
                  "content\n" +
                  "\n" +
                  "# end\n" +
                  "omega\n" );
  }

  @Test
  public void replaceSection_missingFile()
  {
    final Path file = FileUtil.getCurrentDirectory().resolve( "MODULE.bazel" );
    final DepgenException exception =
      expectThrows( DepgenException.class, () -> GeneratedSectionWriter.replaceSection( file, "# start", "# end", "" ) );
    assertEquals( exception.getMessage(), "Expected generated output destination file to exist. File: " + file );
  }

  @Test
  public void replaceSection_missingStartToken()
    throws Exception
  {
    final Path file = FileUtil.getCurrentDirectory().resolve( "MODULE.bazel" );
    FileUtil.write( file, "# end\n" );

    final DepgenException exception =
      expectThrows( DepgenException.class,
                    () -> GeneratedSectionWriter.replaceSection( file, "# start", "# end", "" ) );
    assertEquals( exception.getMessage(),
                  "Expected generated output destination file to contain start token '# start'. File: " + file );
  }

  @Test
  public void replaceSection_missingEndToken()
    throws Exception
  {
    final Path file = FileUtil.getCurrentDirectory().resolve( "MODULE.bazel" );
    FileUtil.write( file, "# start\n" );

    final DepgenException exception =
      expectThrows( DepgenException.class,
                    () -> GeneratedSectionWriter.replaceSection( file, "# start", "# end", "" ) );
    assertEquals( exception.getMessage(),
                  "Expected generated output destination file to contain end token '# end' after the start token. " +
                  "File: " + file );
  }

  @Test
  public void ensureSectionExists_addsMarkers()
    throws Exception
  {
    final Path file = FileUtil.getCurrentDirectory().resolve( "BUILD.bazel" );
    FileUtil.write( file, "package(default_visibility = [\"//visibility:public\"])\n" );

    assertTrue( GeneratedSectionWriter.ensureSectionExists( file, "# start", "# end" ) );
    assertEquals( readFile( file ),
                  "package(default_visibility = [\"//visibility:public\"])\n" +
                  "\n" +
                  "# start\n" +
                  "\n" +
                  "# end\n" );
  }

  @Test
  public void ensureSectionExists_noopWhenMarkersAlreadyPresent()
    throws Exception
  {
    final Path file = FileUtil.getCurrentDirectory().resolve( "BUILD.bazel" );
    FileUtil.write( file, "# start\n\n# end\n" );

    assertFalse( GeneratedSectionWriter.ensureSectionExists( file, "# start", "# end" ) );
    assertEquals( readFile( file ), "# start\n\n# end\n" );
  }

  @Test
  public void ensureSectionExists_rejectsIncompleteMarkers()
    throws Exception
  {
    final Path file = FileUtil.getCurrentDirectory().resolve( "BUILD.bazel" );
    FileUtil.write( file, "# start\n" );

    final DepgenException exception =
      expectThrows( DepgenException.class,
                    () -> GeneratedSectionWriter.ensureSectionExists( file, "# start", "# end" ) );
    assertEquals( exception.getMessage(),
                  "Expected generated output destination file to either contain both markers or neither marker. " +
                  "File: " + file );
  }

  private String readFile( final Path file )
    throws Exception
  {
    return Files.readString( file, StandardCharsets.UTF_8 );
  }
}
