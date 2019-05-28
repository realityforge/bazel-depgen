package org.realityforge.bazel.depgen;

import javax.annotation.Nullable;

class TerminalStateException
  extends RuntimeException
{
  private final int _exitCode;

  TerminalStateException( final int exitCode )
  {
    this( null, exitCode );
  }

  TerminalStateException( @Nullable final String message, final int exitCode )
  {
    this( message, null, exitCode );
  }

  TerminalStateException( @Nullable final String message, @Nullable final Throwable cause, final int exitCode )
  {
    super( message, cause );
    _exitCode = exitCode;
  }

  int getExitCode()
  {
    return _exitCode;
  }
}
