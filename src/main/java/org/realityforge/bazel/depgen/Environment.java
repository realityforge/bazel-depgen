package org.realityforge.bazel.depgen;

import java.io.Console;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class Environment
{
  @Nullable
  private final Console _console;
  @Nonnull
  private final Logger _logger;

  Environment( @Nonnull final Logger logger )
  {
    this( null, logger );
  }

  Environment( @Nullable final Console console, @Nonnull final Logger logger )
  {
    _console = console;
    _logger = Objects.requireNonNull( logger );
  }

  @Nullable
  Console console()
  {
    return _console;
  }

  @Nonnull
  Logger logger()
  {
    return _logger;
  }
}
