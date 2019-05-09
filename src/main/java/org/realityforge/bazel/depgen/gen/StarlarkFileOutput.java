package org.realityforge.bazel.depgen.gen;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.annotation.Nonnull;

final class StarlarkFileOutput
  implements AutoCloseable
{
  @Nonnull
  private final FileOutputStream _outputStream;
  private int _indent;

  StarlarkFileOutput( @Nonnull final Path extensionFile )
    throws FileNotFoundException
  {
    _outputStream = new FileOutputStream( extensionFile.toFile() );
  }

  void write( @Nonnull final String line )
    throws IOException
  {
    for ( int i = 0; i < _indent; i++ )
    {
      emit( "    " );
    }
    emit( line );
    newLine();
  }

  void newLine()
    throws IOException
  {
    emit( "\n" );
  }

  void incIndent()
  {
    _indent++;
  }

  void decIndent()
  {
    _indent--;
    assert _indent >= 0;
  }

  @Override
  public void close()
    throws Exception
  {
    _outputStream.close();
  }

  private void emit( @Nonnull final String string )
    throws IOException
  {
    _outputStream.write( string.getBytes( StandardCharsets.US_ASCII ) );
  }
}
