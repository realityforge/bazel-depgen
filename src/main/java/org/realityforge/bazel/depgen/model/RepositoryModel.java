package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.config.RepositoryConfig;

public final class RepositoryModel
{
  @Nullable
  private final RepositoryConfig _source;
  @Nullable
  private final String _name;
  @Nonnull
  private final String _url;

  @Nonnull
  public static RepositoryModel parse( @Nonnull final RepositoryConfig source )
  {
    final String url = source.getUrl();
    if ( null == url )
    {
      throw new InvalidModelException( "The repository must specify the 'url' property.", source );
    }
    return new RepositoryModel( source, source.getName(), url );
  }

  @Nonnull
  public static RepositoryModel create( @Nullable final String name, @Nonnull final String url )
  {
    return new RepositoryModel( null, name, url );
  }

  private RepositoryModel( @Nullable final RepositoryConfig source,
                           @Nullable final String name,
                           @Nonnull final String url )
  {
    _source = source;
    _name = name;
    _url = Objects.requireNonNull( url );
  }

  @Nullable
  public RepositoryConfig getSource()
  {
    return _source;
  }

  @Nullable
  public String getName()
  {
    return _name;
  }

  @Nonnull
  public String getUrl()
  {
    return _url;
  }
}
