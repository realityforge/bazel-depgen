package org.realityforge.bazel.depgen.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.record.LicenseRecord;
import org.realityforge.bazel.depgen.util.ArtifactUtil;
import org.realityforge.bazel.depgen.util.HashUtil;

final class RecordUtil
{
  private RecordUtil()
  {
  }

  @Nonnull
  static String getLicensesAsString( @Nonnull final File pomFile )
  {
    final List<LicenseRecord> result;
    try
    {
      final Model model = new MavenXpp3Reader().read( new FileInputStream( pomFile ) );
      return model.getLicenses()
        .stream()
        .map( l1 -> new LicenseRecord( LicenseUtil.classifyLicense( l1.getName() ), l1.getName() ) )
        .map( l -> ( null == l.getType() ? "" : l.getType() ) + ":" + l.getName() )
        .collect( Collectors.joining( "|" ) );
    }
    catch ( final IOException | XmlPullParserException e )
    {
      return DepgenMetadata.SENTINEL;
    }
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

  @Nullable
  static String lookupArtifactInRepository( @Nonnull final Artifact artifact,
                                            @Nonnull final RemoteRepository remoteRepository,
                                            @Nonnull final Map<String, AuthenticationContext> authenticationContexts )
  {
    try
    {
      final String repoUrl = remoteRepository.getUrl();
      final URI uri =
        new URI( repoUrl + ( repoUrl.endsWith( "/" ) ? "" : "/" ) + ArtifactUtil.artifactToPath( artifact ) );
      final URL url = uri.toURL();
      final String protocol = url.getProtocol();
      if ( "http".equals( protocol ) || "https".equals( protocol ) )
      {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod( "HEAD" );
        final AuthenticationContext context = authenticationContexts.get( remoteRepository.getId() );
        if ( null != context )
        {
          final String username = context.get( AuthenticationContext.USERNAME );
          final String password = context.get( AuthenticationContext.PASSWORD );
          if ( null != username && null != password )
          {
            final String encoded =
              Base64.getEncoder().encodeToString( ( username + ":" + password ).getBytes( StandardCharsets.UTF_8 ) );
            connection.setRequestProperty( "Authorization", "Basic " + encoded );
          }
        }
        connection.connect();
        final int responseCode = connection.getResponseCode();
        if ( 200 == responseCode )
        {
          return url.toExternalForm();
        }
      }
      else if ( "file".equals( protocol ) )
      {
        // Attempt to open file and if it is present then there should be no exception
        url.openStream().close();
        return uri.toString();
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
    return null;
  }

  @Nonnull
  static String readAnnotationProcessors( @Nonnull final File file )
  {
    if ( file.getName().endsWith( ".jar" ) )
    {
      try
      {
        try ( final JarFile jar = new JarFile( file ) )
        {
          final ZipEntry entry = jar.getEntry( "META-INF/services/javax.annotation.processing.Processor" );
          if ( null != entry )
          {
            try ( final Reader input = new InputStreamReader( jar.getInputStream( entry ) ) )
            {
              try ( final BufferedReader reader = new BufferedReader( input ) )
              {
                final ArrayList<String> processors = new ArrayList<>();
                String line;
                while ( null != ( line = reader.readLine() ) )
                {
                  final String l = line.trim();
                  if ( !l.isEmpty() )
                  {
                    processors.add( l );
                  }
                }
                return String.join( ",", processors );
              }
            }
          }
        }
      }
      catch ( final IOException ignored )
      {
        //
      }
    }

    return DepgenMetadata.SENTINEL;
  }
}
