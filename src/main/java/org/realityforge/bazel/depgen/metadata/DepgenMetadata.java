package org.realityforge.bazel.depgen.metadata;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.util.OrderedProperties;

/**
 * The depgen specific metadata for a artifact groupId+artifactId+version combination in the context of a particular context.
 * This includes information such as the sha256 values, urls in particular repositories, annotation processors present etc
 * for each artifact/classifier.
 *
 * <p>The data is derived in the context of a particular {@link ApplicationRecord} so that only the repositories that
 * are registered in <code>dependency.yaml</code> are checked etc.</p>
 */
public final class DepgenMetadata
{
  @Nonnull
  public static final String FILENAME = "_depgen.properties";
  static final String SENTINEL = "-";
  @Nonnull
  private final Path _file;
  @Nullable
  private Properties _properties;

  @Nonnull
  public static DepgenMetadata fromDirectory( @Nonnull final Path dir )
  {
    return new DepgenMetadata( dir.resolve( FILENAME ) );
  }

  DepgenMetadata( @Nonnull final Path file )
  {
    _file = Objects.requireNonNull( file );
  }

  /**
   * Return the value associated with key.
   *
   * @param key the property key.
   * @return the value or null if no such value.
   */
  @Nullable
  public String getProperty( @Nonnull final String key )
  {
    return getCachedProperties().getProperty( key );
  }

  public void updateProperty( @Nonnull final String key, @Nonnull final String value )
  {
    getCachedProperties().setProperty( key, value );
    saveCachedProperties();
  }

  /**
   * Return the sha256 for artifact with filename and classifier.
   *
   * @param classifier the artifacts classifier or the empty string if no classifier.
   * @param file       the artifact file.
   * @return the sha256 of the specified artifact.
   */
  @Nonnull
  public String getSha256( @Nonnull final String classifier, @Nonnull final File file )
  {
    return getOrCompute( classifierAsKey( classifier ) + ".sha256", () -> RecordUtil.sha256( file ) );
  }

  /**
   * Return the urls where the artifact can be found.
   *
   * @param artifact               the artifact.
   * @param repositories           the remote repositories associated with the artifact.
   * @param authenticationContexts the authentication contexts used to authenticate against repositories.
   * @param callback               the callback that used to notify invoker or errors/warnings.
   * @return the urls where the artifact is present.
   */
  @Nonnull
  public List<String> getUrls( @Nonnull final Artifact artifact,
                               @Nonnull final List<RemoteRepository> repositories,
                               @Nonnull final Map<String, AuthenticationContext> authenticationContexts,
                               @Nonnull final RecordBuildCallback callback )
  {
    final ArrayList<String> urls = new ArrayList<>();
    for ( final RemoteRepository remoteRepository : repositories )
    {
      final String key = classifierAsKey( artifact.getClassifier() ) + "." + remoteRepository.getId() + ".url";
      final Properties properties = getCachedProperties();
      final String existing = properties.getProperty( key );
      if ( null != existing && !SENTINEL.equals( existing ) && !existing.startsWith( remoteRepository.getUrl() ) )
      {
        callback.onWarning( "Cache entry '" + key + "' for artifact '" + artifact + "' contains a url '" +
                            existing + "' that does not match the repository url '" + remoteRepository.getUrl() +
                            "'. Removing cache entry." );
        properties.remove( key );
        saveCachedProperties();
      }

      final String url =
        getOrCompute( key, () -> lookupArtifact( artifact, remoteRepository, authenticationContexts ) );
      if ( !SENTINEL.equals( url ) )
      {
        urls.add( url );
      }
    }

    if ( urls.isEmpty() )
    {
      throw new IllegalStateException( "Unable to locate artifact " + artifact + " in any repository." );
    }
    return urls;
  }

  @Nullable
  public List<String> getProcessors( @Nonnull final File file )
  {
    final String processors = getOrCompute( "processors", () -> RecordUtil.readAnnotationProcessors( file ) );
    return SENTINEL.equals( processors ) ?
           null :
           Collections.unmodifiableList( Arrays.asList( processors.split( "," ) ) );
  }

  @Nonnull
  private String lookupArtifact( @Nonnull final Artifact artifact,
                                 @Nonnull final RemoteRepository remoteRepository,
                                 @Nonnull final Map<String, AuthenticationContext> authenticationContexts )
  {
    final String url =
      RecordUtil.lookupArtifactInRepository( artifact, remoteRepository, authenticationContexts );
    return null == url ? SENTINEL : url;
  }

  /**
   * Return value cached under key or compute value and cache it before returning value.
   *
   * @param key    the key under which to cache value.
   * @param action the action to calculate value.
   * @return the value.
   */
  @Nonnull
  private String getOrCompute( @Nonnull final String key, @Nonnull final Supplier<String> action )
  {
    final String existingValue = getProperty( key );
    if ( null != existingValue )
    {
      return existingValue;
    }
    else
    {
      final String value = action.get();
      updateProperty( key, value );
      return value;
    }
  }

  @Nonnull
  private String classifierAsKey( @Nonnull final String classifier )
  {
    return classifier.isEmpty() ? "<default>" : classifier;
  }

  private void saveCachedProperties()
  {
    assert null != _properties;
    try
    {
      try ( final Writer writer = Files.newBufferedWriter( _file ) )
      {
        _properties.store( writer, null );
      }
    }
    catch ( final IOException ioe )
    {
      throw new IllegalStateException( "Failed to write metadata file " + _file + " due to " + ioe, ioe );
    }
  }

  @Nonnull
  private Properties getCachedProperties()
  {
    if ( null == _properties )
    {
      final Properties properties = new OrderedProperties();
      if ( _file.toFile().exists() && _file.toFile().isFile() )
      {
        try
        {
          properties.load( Files.newBufferedReader( _file ) );
        }
        catch ( final IOException ignored )
        {
          //Ignored. Assumed to be invalid formatted file that will be fixed when we write to it.
        }
      }
      _properties = properties;
    }
    return _properties;
  }
}
