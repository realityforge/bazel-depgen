package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.OptionsConfig;

public final class OptionsModel
{
  @Nonnull
  private final OptionsConfig _source;

  @Nonnull
  public static OptionsModel parse( @Nonnull final OptionsConfig source )
  {
    return new OptionsModel( source );
  }

  private OptionsModel( @Nonnull final OptionsConfig source )
  {
    _source = Objects.requireNonNull( source );
  }

  @Nonnull
  public OptionsConfig getSource()
  {
    return _source;
  }

  @Nonnull
  public String getWorkspaceDirectory()
  {
    return _source.getWorkspaceDirectory();
  }

  @Nonnull
  public String getExtensionFile()
  {
    return _source.getExtensionFile();
  }

  public boolean failOnInvalidPom()
  {
    return _source.isFailOnInvalidPom();
  }

  public boolean failOnMissingPom()
  {
    return _source.isFailOnMissingPom();
  }
}
