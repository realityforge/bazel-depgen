package org.realityforge.bazel.depgen;

import javax.annotation.Nullable;

public class TerminalStateException
  extends RuntimeException
{
  private final int _exitCode;

  public TerminalStateException( final int exitCode )
  {
    this( null, exitCode );
  }

  public TerminalStateException( @Nullable final String message, final int exitCode )
  {
    this( message, null, exitCode );
  }

  public TerminalStateException( @Nullable final String message, @Nullable final Throwable cause, final int exitCode )
  {
    super( message, cause );
    _exitCode = exitCode;
  }

  public int getExitCode()
  {
    return _exitCode;
  }
}
