package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.Nature;

public final class ReplacementTargetModel
{
  @Nonnull
  private final Nature _nature;
  @Nonnull
  private final String _target;

  public ReplacementTargetModel( @Nonnull final Nature nature, @Nonnull final String target )
  {
    _nature = Objects.requireNonNull( nature );
    _target = Objects.requireNonNull( target );
  }

  @Nonnull
  public Nature getNature()
  {
    return _nature;
  }

  @Nonnull
  public String getTarget()
  {
    return _target;
  }
}
