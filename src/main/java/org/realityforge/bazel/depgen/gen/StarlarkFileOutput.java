package org.realityforge.bazel.depgen.gen;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  void writeMacroStart( @Nonnull final String name, @Nonnull final List<String> arguments )
    throws IOException
  {
    final int size = arguments.size();
    if ( 0 == size )
    {
      write( "def " + name + "():" );
    }
    else if ( 1 == size )
    {
      write( "def " + name + "(" + arguments.get( 0 ) + "):" );
    }
    else
    {
      write( "def " + name + "(" );
      incIndent();
      incIndent();
      int index = 0;
      for ( final String argument : arguments )
      {
        index++;
        write( argument + ( index == size ? "):" : "," ) );
      }
      decIndent();
      decIndent();
    }
  }

  void writeCall( @Nonnull final String functionName, @Nonnull final LinkedHashMap<String, Object> arguments )
    throws IOException
  {
    if ( arguments.isEmpty() )
    {
      write( functionName + "()" );
    }
    else
    {
      write( functionName + "(" );
      incIndent();
      for ( final Map.Entry<String, Object> entry : arguments.entrySet() )
      {
        final String key = entry.getKey();
        final Object value = entry.getValue();
        if ( null == value )
        {
          write( key + " = None," );
        }
        else if ( value instanceof List )
        {
          final List arg = (List) value;
          if ( arg.isEmpty() )
          {
            write( key + " = []," );
          }
          else if ( 1 == arg.size() )
          {
            write( key + " = [" + arg.get( 0 ) + "]," );
          }
          else
          {
            write( key + " = [" );
            incIndent();
            for ( final Object innerValue : arg )
            {
              write( innerValue + "," );
            }
            decIndent();
            write( "]," );
          }
        }
        else
        {
          write( key + " = " + value + "," );
        }
      }
      decIndent();
      write( ")" );
    }
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
