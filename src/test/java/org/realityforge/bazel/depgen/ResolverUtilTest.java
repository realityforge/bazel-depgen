package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.ExcludeModel;
import org.realityforge.bazel.depgen.model.RepositoryModel;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ResolverUtilTest
  extends AbstractTest
{
  @Test
  public void getRemoteRepositories()
    throws Exception
  {
    final List<RepositoryModel> repositories =
      Arrays.asList( RepositoryModel.create( "central", "https://repo1.maven.org/maven2" ),
                     RepositoryModel.create( "example", "https://example.com/maven2" ) );

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

    final List<RemoteRepository> remoteRepositories = ResolverUtil.getRemoteRepositories( repositories, settings );

    assertEquals( remoteRepositories.size(), 2 );
    final RemoteRepository central = remoteRepositories.get( 0 );
    assertEquals( central.getId(), "central" );
    assertEquals( central.getUrl(), "https://repo1.maven.org/maven2" );
    assertNull( central.getAuthentication() );

    final RemoteRepository example = remoteRepositories.get( 1 );
    assertEquals( example.getId(), "example" );
    assertEquals( example.getUrl(), "https://example.com/maven2" );
    assertNotNull( example.getAuthentication() );
  }

  @Test
  public void createResolver()
    throws Exception
  {
    // Install jar into local repository
    // Download from local repository to local cacheDir

    final Path localRepository = FileUtil.getCurrentDirectory().resolve( "repository" );
    assertTrue( localRepository.toFile().mkdirs() );

    // Install jar into local repository
    {
      deployTempArtifactToLocalRepository( localRepository, "com.example:myapp:1.0.0" );

      assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.pom" ).toFile().exists() );
      assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.pom.sha1" ).toFile().exists() );
      assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar" ).toFile().exists() );
      assertTrue( localRepository.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar.sha1" ).toFile().exists() );
    }

    // Download from local repository to local cacheDir
    {
      final Path cacheDir = FileUtil.getCurrentDirectory().resolve( "cacheDir" );

      final Resolver resolver =
        ResolverUtil.createResolver( newEnvironment(), cacheDir, Collections.emptyList(), true, true );

      final RemoteRepository remoteRepository =
        new RemoteRepository.Builder( "local", "default", localRepository.toUri().toString() ).build();
      final ArtifactRequest request =
        new ArtifactRequest( new DefaultArtifact( "com.example:myapp:1.0.0" ),
                             Collections.singletonList( remoteRepository ),
                             null );
      final ArtifactResult artifactResult = resolver.getSystem().resolveArtifact( resolver.getSession(), request );

      assertTrue( artifactResult.getExceptions().isEmpty() );
      assertTrue( artifactResult.isResolved() );
      assertFalse( artifactResult.isMissing() );

      assertTrue( cacheDir.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar" ).toFile().exists() );
      assertTrue( cacheDir.resolve( "com/example/myapp/1.0.0/myapp-1.0.0.jar.sha1" ).toFile().exists() );
    }
  }

  @Test
  public void deriveExclusions_noExcludes()
  {
    final ArtifactModel artifactModel =
      new ArtifactModel( new ArtifactConfig(),
                         "com.example",
                         "myapp",
                         "jar",
                         null,
                         "1.0",
                         Collections.emptyList(),
                         Collections.emptyList() );
    final ArrayList<Exclusion> exclusions = ResolverUtil.deriveExclusions( artifactModel );

    assertTrue( exclusions.isEmpty() );
  }

  @Test
  public void deriveExclusions()
  {
    final ArrayList<ExcludeModel> excludes = new ArrayList<>();
    excludes.add( new ExcludeModel( "org.oss", null ) );
    excludes.add( new ExcludeModel( "com.biz", "zelib" ) );
    final ArtifactModel artifactModel =
      new ArtifactModel( new ArtifactConfig(),
                         "com.example",
                         "myapp",
                         "jar",
                         null,
                         "1.0",
                         excludes,
                         Collections.emptyList() );
    final ArrayList<Exclusion> exclusions = ResolverUtil.deriveExclusions( artifactModel );

    assertFalse( exclusions.isEmpty() );
    final Exclusion exclusion1 = exclusions.get( 0 );
    assertEquals( exclusion1.getGroupId(), "org.oss" );
    assertEquals( exclusion1.getArtifactId(), "*" );
    assertEquals( exclusion1.getExtension(), "*" );
    assertEquals( exclusion1.getClassifier(), "*" );

    final Exclusion exclusion2 = exclusions.get( 1 );
    assertEquals( exclusion2.getGroupId(), "com.biz" );
    assertEquals( exclusion2.getArtifactId(), "zelib" );
    assertEquals( exclusion2.getExtension(), "*" );
    assertEquals( exclusion2.getClassifier(), "*" );
  }

  @Test
  public void deriveGlobalExclusions()
    throws Exception
  {
    writeConfigFile( "excludes:\n" +
                     "  - coord: org.oss:app\n" +
                     "  - coord: com.biz:zelib\n" );
    final ApplicationModel model = loadApplicationModel();

    final ArrayList<Exclusion> exclusions = ResolverUtil.deriveGlobalExclusions( model );

    assertFalse( exclusions.isEmpty() );
    final Exclusion exclusion1 = exclusions.get( 0 );
    assertEquals( exclusion1.getGroupId(), "org.oss" );
    assertEquals( exclusion1.getArtifactId(), "app" );
    assertEquals( exclusion1.getExtension(), "*" );
    assertEquals( exclusion1.getClassifier(), "*" );

    final Exclusion exclusion2 = exclusions.get( 1 );
    assertEquals( exclusion2.getGroupId(), "com.biz" );
    assertEquals( exclusion2.getArtifactId(), "zelib" );
    assertEquals( exclusion2.getExtension(), "*" );
    assertEquals( exclusion2.getClassifier(), "*" );
  }
}
