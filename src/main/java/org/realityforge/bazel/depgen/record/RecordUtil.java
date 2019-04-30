package org.realityforge.bazel.depgen.record;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.util.HashUtil;

final class RecordUtil
{
  private RecordUtil()
  {
  }

  @Nonnull
  static String toArtifactKey( @Nonnull final ArtifactModel model )
  {
    return model.getGroup() + ":" + model.getId();
  }

  @Nonnull
  static String toArtifactKey( @Nonnull final DependencyNode node )
  {
    final Artifact artifact = node.getArtifact();
    assert null != artifact;
    return artifact.getGroupId() + ":" + artifact.getArtifactId();
  }

  @Nonnull
  static String artifactToPath( @Nonnull final Artifact artifact )
  {
    return artifact.getGroupId().replaceAll( "\\.", "/" ) +
           "/" +
           artifact.getArtifactId() +
           "/" +
           artifact.getVersion() +
           "/" +
           artifact.getArtifactId() +
           "-" +
           artifact.getVersion() +
           ( artifact.getClassifier().isEmpty() ? "" : "-" + artifact.getClassifier() ) +
           "." +
           artifact.getExtension();
  }

  @Nonnull
  static String sha256( @Nonnull final File file )
  {
    try
    {
      return HashUtil.sha256( Files.readAllBytes( file.toPath() ) );
    }
    catch ( final IOException ioe )
    {
      throw new IllegalStateException( "Error generating sha256 hash for file " + file, ioe );
    }
  }

  @Nonnull
  static List<String> deriveUrls( @Nonnull final Artifact artifact, @Nonnull final List<RemoteRepository> repositories )
  {
    final ArrayList<String> urls = new ArrayList<>();
    for ( final RemoteRepository remoteRepository : repositories )
    {
      try
      {
        final String repoUrl = remoteRepository.getUrl();
        final URI uri = new URI( repoUrl + (repoUrl.endsWith( "/" ) ? "" : "/") + artifactToPath( artifact ) );
        final URL url = uri.toURL();
        final String protocol = url.getProtocol();
        if ( "http".equals( protocol ) || "https".equals( protocol ) )
        {
          final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.setRequestMethod( "HEAD" );
          connection.connect();
          final int responseCode = connection.getResponseCode();
          if ( 200 == responseCode )
          {
            urls.add( url.toExternalForm() );
          }
        }
        else if ( "file".equals( protocol ) )
        {
          // Attempt to open file and if it is present then there should be no exception
          url.openStream().close();
          urls.add( uri.toString() );
        }
        else
        {
          final String message = "Unsupported repository protocol for " + artifact + " with url " + url + ".";
          throw new IllegalStateException( message );
        }
      }
      catch ( final IOException | URISyntaxException ignored )
      {
      }
    }

    if ( urls.isEmpty() )
    {
      throw new IllegalStateException( "Unable to locate artifact " + artifact + " in any repository." );
    }
    return urls;
  }
}
