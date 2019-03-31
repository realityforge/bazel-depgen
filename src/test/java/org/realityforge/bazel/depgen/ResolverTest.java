package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ResolverTest
  extends AbstractTest
{
  @Test
  public void deriveArtifact_missingArtifact()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final TestHandler handler = new TestHandler();

      final Path dir = FileUtil.createTempDir();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ),
                                     dir,
                                     new ArrayList<>(),
                                     true,
                                     true );

      final ArtifactModel model =
        new ArtifactModel( new ArtifactConfig(), "com.example", "myapp", null, null, "1.0", Collections.emptyList() );

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final Artifact artifact = resolver.toArtifact( model, exceptions -> hasFailed.set( true ) );

      assertTrue( hasFailed.get() );
      final ArrayList<LogRecord> records = handler.getRecords();
      assertEquals( records.size(), 1 );
      assertEquals( records.get( 0 ).getMessage(),
                    "Could not find artifact com.example:myapp:jar:1.0" );

      assertNull( artifact.getFile() );
      assertEquals( artifact.toString(), "com.example:myapp:jar:1.0" );
    } );
  }

  @Test
  public void deriveArtifact_pomMissing_when_failOnMissingPom()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final TestHandler handler = new TestHandler();

      final Path dir = FileUtil.createTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      Files.write( artifactDir.resolve( "myapp-1.0.jar" ), new byte[ 0 ] );

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ),
                                     dir,
                                     new ArrayList<>(),
                                     true,
                                     true );

      final ArtifactModel model =
        new ArtifactModel( new ArtifactConfig(), "com.example", "myapp", null, null, "1.0", Collections.emptyList() );

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final Artifact artifact = resolver.toArtifact( model, exceptions -> hasFailed.set( true ) );

      assertTrue( hasFailed.get() );
      final ArrayList<LogRecord> records = handler.getRecords();
      assertEquals( records.size(), 1 );
      assertEquals( records.get( 0 ).getMessage(),
                    "Missing artifact descriptor for com.example:myapp:jar:1.0" );

      assertNull( artifact.getFile() );
      assertEquals( artifact.toString(), "com.example:myapp:jar:1.0" );
    } );
  }

  @Test
  public void deriveArtifact_pomMissing_when_not_failOnMissingPom()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final TestHandler handler = new TestHandler();

      final Path dir = FileUtil.createTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      final Path artifactFile = artifactDir.resolve( "myapp-1.0.jar" );
      Files.write( artifactFile, new byte[ 0 ] );

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ),
                                     dir,
                                     new ArrayList<>(),
                                     false,
                                     true );

      final ArtifactModel model =
        new ArtifactModel( new ArtifactConfig(), "com.example", "myapp", null, null, "1.0", Collections.emptyList() );

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final Artifact artifact = resolver.toArtifact( model, exceptions -> hasFailed.set( true ) );

      assertFalse( hasFailed.get() );
      final ArrayList<LogRecord> records = handler.getRecords();
      assertEquals( records.size(), 1 );
      assertEquals( records.get( 0 ).getMessage(),
                    "Missing artifact descriptor for com.example:myapp:jar:1.0" );

      assertEquals( artifact.getFile().getAbsoluteFile(), artifactFile.toFile().getAbsoluteFile() );
      assertEquals( artifact.toString(), "com.example:myapp:jar:1.0" );
    } );
  }

  @Test
  public void deriveArtifact_pomInvalid_when_failOnInvalidPom()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      Files.write( artifactDir.resolve( "myapp-1.0.jar" ), new byte[ 0 ] );
      Files.write( artifactDir.resolve( "myapp-1.0.pom" ), "".getBytes() );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, new ArrayList<>(), true, true );

      final ArtifactModel model =
        new ArtifactModel( new ArtifactConfig(), "com.example", "myapp", null, null, "1.0", Collections.emptyList() );

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final Artifact artifact = resolver.toArtifact( model, exceptions -> hasFailed.set( true ) );

      assertTrue( hasFailed.get() );
      final ArrayList<LogRecord> records = handler.getRecords();
      assertEquals( records.size(), 1 );
      assertTrue( records.get( 0 )
                    .getMessage()
                    .startsWith( "Invalid artifact descriptor for com.example:myapp:jar:1.0:" ) );

      assertNull( artifact.getFile() );
      assertEquals( artifact.toString(), "com.example:myapp:jar:1.0" );
    } );
  }

  @Test
  public void deriveArtifact_pomInvalid_when_not_failOnInvalidPom()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      final Path artifactFile = artifactDir.resolve( "myapp-1.0.jar" );
      Files.write( artifactFile, new byte[ 0 ] );
      Files.write( artifactDir.resolve( "myapp-1.0.pom" ), "".getBytes() );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, new ArrayList<>(), true, false );

      final ArtifactModel model =
        new ArtifactModel( new ArtifactConfig(), "com.example", "myapp", null, null, "1.0", Collections.emptyList() );

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final Artifact artifact = resolver.toArtifact( model, exceptions -> hasFailed.set( true ) );

      assertFalse( hasFailed.get() );

      final ArrayList<LogRecord> records = handler.getRecords();
      assertEquals( records.size(), 1 );
      assertTrue( records.get( 0 )
                    .getMessage()
                    .startsWith( "Invalid artifact descriptor for com.example:myapp:jar:1.0:" ) );

      assertEquals( artifact.getFile().getAbsoluteFile(), artifactFile.toFile().getAbsoluteFile() );
      assertEquals( artifact.getGroupId(), "com.example" );
      assertEquals( artifact.getArtifactId(), "myapp" );
      assertEquals( artifact.getVersion(), "1.0" );
      assertEquals( artifact.getClassifier(), "" );
      assertEquals( artifact.getExtension(), "jar" );
    } );
  }

  @Test
  public void deriveArtifact_inRemoteRpository()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createTempDir();
      final Path remoteDir = FileUtil.createTempDir();

      deployTempArtifactToLocalRepository( remoteDir, "com.example:myapp:1.0" );
      assertTrue( remoteDir.resolve( "com/example/myapp/1.0/myapp-1.0.pom" ).toFile().exists() );
      assertTrue( remoteDir.resolve( "com/example/myapp/1.0/myapp-1.0.jar" ).toFile().exists() );

      assertFalse( dir.resolve( "com/example/myapp/1.0/myapp-1.0.pom" ).toFile().exists() );
      assertFalse( dir.resolve( "com/example/myapp/1.0/myapp-1.0.jar" ).toFile().exists() );

      final TestHandler handler = new TestHandler();

      final ArrayList<RemoteRepository> repositories = new ArrayList<>();
      repositories.add( new RemoteRepository.Builder( "local", "default", remoteDir.toUri().toString() ).build() );
      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, repositories, true, true );

      final ArtifactModel model =
        new ArtifactModel( new ArtifactConfig(), "com.example", "myapp", null, null, "1.0", Collections.emptyList() );

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final Artifact artifact = resolver.toArtifact( model, exceptions -> hasFailed.set( true ) );

      assertFalse( hasFailed.get() );
      assertTrue( handler.getRecords().isEmpty() );

      assertTrue( dir.resolve( "com/example/myapp/1.0/myapp-1.0.pom" ).toFile().exists() );
      assertTrue( dir.resolve( "com/example/myapp/1.0/myapp-1.0.jar" ).toFile().exists() );

      assertNotNull( artifact.getFile() );
      assertEquals( artifact.toString(), "com.example:myapp:jar:1.0" );
    } );
  }

  @Test
  public void deriveArtifact()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createTempDir();

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
      assertTrue( dir.resolve( "com/example/myapp/1.0/myapp-1.0.pom" ).toFile().exists() );
      assertTrue( dir.resolve( "com/example/myapp/1.0/myapp-1.0.jar" ).toFile().exists() );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, new ArrayList<>(), true, true );

      final ArtifactModel model =
        new ArtifactModel( new ArtifactConfig(), "com.example", "myapp", null, null, "1.0", Collections.emptyList() );

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final Artifact artifact = resolver.toArtifact( model, exceptions -> hasFailed.set( true ) );

      assertFalse( hasFailed.get() );
      assertTrue( handler.getRecords().isEmpty() );

      assertNotNull( artifact.getFile() );
      assertEquals( artifact.toString(), "com.example:myapp:jar:1.0" );
    } );
  }
}
