package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;

public enum Nature
{
  Java( "-java", false ),
  Plugin( "-plugin", false ),
  J2cl( "-j2cl", true );
  @Nonnull
  private final String _suffix;
  private final boolean _mandatorySuffix;

  Nature( @Nonnull final String suffix, final boolean mandatorySuffix )
  {
    _suffix = Objects.requireNonNull( suffix );
    _mandatorySuffix = mandatorySuffix;
  }

  @Nonnull
  public String suffix( final boolean multipleNatures, @Nonnull final Nature defaultValue )
  {
    return _mandatorySuffix || ( defaultValue != this && multipleNatures ) ? _suffix : "";
  }
}
