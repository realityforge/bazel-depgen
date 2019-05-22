package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;

public enum Language
{
  Java( "-java" ),
  J2cl( "-j2cl" );
  @Nonnull
  private final String _targetSuffix;

  Language( @Nonnull final String targetSuffix )
  {
    _targetSuffix = Objects.requireNonNull( targetSuffix );
  }

  @Nonnull
  public String getTargetSuffix()
  {
    return _targetSuffix;
  }
}
