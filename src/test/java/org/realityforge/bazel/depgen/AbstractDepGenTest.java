package org.realityforge.bazel.depgen;

import gir.Gir;
import gir.Task;
import gir.io.Exec;
import gir.io.FileUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import javax.annotation.Nonnull;
import static org.testng.Assert.*;

abstract class AbstractDepGenTest
{
  @Nonnull
  final String runCommand( @Nonnull final String... additionalArgs )
  {
    return runCommand( 0, additionalArgs );
  }

  @Nonnull
  final String runCommand( final int expectedExitCode, @Nonnull final String... additionalArgs )
  {
    final ArrayList<String> args = new ArrayList<>();
    args.add( "java" );
    args.add( "-Duser.home=" + FileUtil.getCurrentDirectory() );
    args.add( "-jar" );
    args.add( getApplicationJar().toString() );
    Collections.addAll( args, additionalArgs );
    return Exec.capture( b -> Exec.cmd( b, args.toArray( new String[ 0 ] ) ), expectedExitCode );
  }

  final void inIsolatedDirectory( @Nonnull final Task java )
    throws Exception
  {
    Gir.go( () -> FileUtil.inTempDir( java ) );
  }

  @Nonnull
  final Path getApplicationJar()
  {
    return Paths.get( System.getProperty( "depgen.jar" ) ).toAbsolutePath().normalize();
  }

  final void writeWorkspace()
    throws IOException
  {
    FileUtil.write( "WORKSPACE", "" );
  }

  final void writeDependencies( @Nonnull final String content )
    throws IOException
  {
    FileUtil.write( "dependencies.yml", content );
  }

  final void assertOutputContains( @Nonnull final String output, @Nonnull final String text )
  {
    assertTrue( output.contains( text ),
                "Expected output\n---\n" + output + "\n---\nto contain text\n---\n" + text + "\n---\n" );
  }
}
