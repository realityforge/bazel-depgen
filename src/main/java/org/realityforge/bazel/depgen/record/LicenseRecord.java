package org.realityforge.bazel.depgen.record;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.config.LicenseType;

public class LicenseRecord
{
  @Nullable
  private final LicenseType _type;
  @Nonnull
  private final String _name;

  public LicenseRecord( @Nullable final LicenseType type, @Nonnull final String name )
  {
    _type = type;
    _name = Objects.requireNonNull( name );
  }

  @Nullable
  public LicenseType getType()
  {
    return _type;
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }
}
