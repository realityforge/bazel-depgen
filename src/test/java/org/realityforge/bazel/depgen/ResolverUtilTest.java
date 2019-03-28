package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ResolverUtilTest
  extends AbstractTest
{
  @Test
  public void getRemoteRepositories()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final HashMap<String, String> repositories = new HashMap<>();
      repositories.put( "central", "https://repo1.maven.org/maven2" );
      repositories.put( "example", "https://example.com/maven2" );

      FileUtil.write( "settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>example</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );

      final Settings settings =
        SettingsUtil.loadSettings( FileUtil.getCurrentDirectory().resolve( "settings.xml" ),
                                   Logger.getAnonymousLogger() );

      final ApplicationConfig source = new ApplicationConfig();
      source.setRepositories( repositories );
      final ApplicationModel model = ApplicationModel.parse( source );
      final List<RemoteRepository> remoteRepositories = ResolverUtil.getRemoteRepositories( model, settings );

      assertEquals( remoteRepositories.size(), 2 );
      final RemoteRepository central = remoteRepositories.get( 0 );
      assertEquals( central.getId(), "central" );
      assertEquals( central.getUrl(), "https://repo1.maven.org/maven2" );
      assertNull( central.getAuthentication() );

      final RemoteRepository example = remoteRepositories.get( 1 );
      assertEquals( example.getId(), "example" );
      assertEquals( example.getUrl(), "https://example.com/maven2" );
      assertNotNull( example.getAuthentication() );
    } );
  }

  @Test
  public void repositorySystemAndSessionAreValid()
    throws Exception
  {
    inIsolatedDirectory( () -> {

      final Logger logger = Logger.getAnonymousLogger();
      final Path localRepository = FileUtil.getCurrentDirectory().resolve( "repository" );
      assertTrue( localRepository.toFile().mkdirs() );
      final RemoteRepository remoteRepository =
        new RemoteRepository.Builder( "local", "default", localRepository.toUri().toString() ).build();

      // Install into local repository
      {
        final RepositorySystem system = ResolverUtil.newRepositorySystem( logger );
        final RepositorySystemSession session =
          ResolverUtil.newRepositorySystemSession( system, localRepository, logger );

        final Artifact jarArtifact =
          new DefaultArtifact( "com.example:myapp:1.0.0" ).setFile( createTempJarFile().toFile() );
        final Artifact pomArtifact =
          new SubArtifact( jarArtifact, "", "pom" )
            .setFile( createTempPomFile( "com.example", "myapp", "1.0.0", "jar" ).toFile() );

        system.deploy( session,
                       new DeployRequest().addArtifact( jarArtifact )
                         .addArtifact( pomArtifact )
                         .setRepository( remoteRepository ) );

        assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.pom" ).toFile().exists() );
        assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.pom.sha1" ).toFile().exists() );
        assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar" ).toFile().exists() );
        assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar.sha1" ).toFile().exists() );
      }

      // Download from local repository to local cacheDir
      {
        final Path cacheDir = FileUtil.getCurrentDirectory().resolve( "cacheDir" );

        final RepositorySystem system = ResolverUtil.newRepositorySystem( logger );
        final RepositorySystemSession session = ResolverUtil.newRepositorySystemSession( system, cacheDir, logger );

        final ArtifactResult artifactResult =
          system.resolveArtifact( session,
                                  new ArtifactRequest( new DefaultArtifact( "com.example:myapp:1.0.0" ),
                                                       Collections.singletonList( remoteRepository ),
                                                       null ) );

        assertTrue( artifactResult.getExceptions().isEmpty() );
        assertTrue( artifactResult.isResolved() );
        assertFalse( artifactResult.isMissing() );

        assertTrue( cacheDir.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar" ).toFile().exists() );
        assertTrue( cacheDir.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar.sha1" ).toFile().exists() );
      }
    } );
  }
}
