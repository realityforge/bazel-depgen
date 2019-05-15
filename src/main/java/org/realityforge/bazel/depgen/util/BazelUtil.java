package org.realityforge.bazel.depgen.util;

import javax.annotation.Nonnull;

public final class BazelUtil
{
  private BazelUtil()
  {
  }

  @Nonnull
  public static String cleanNamePart( @Nonnull final String name )
  {
    return name.toLowerCase().replaceAll( "[^a-z0-9]", "_" );
  }
}
