package org.realityforge.bazel.depgen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResult;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
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

      final Path dir = FileUtil2.createLocalTempDir();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ),
                                     dir,
                                     Collections.emptyList(),
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

      final Path dir = FileUtil2.createLocalTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      Files.write( artifactDir.resolve( "myapp-1.0.jar" ), new byte[ 0 ] );

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ),
                                     dir,
                                     Collections.emptyList(),
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

      final Path dir = FileUtil2.createLocalTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      final Path artifactFile = artifactDir.resolve( "myapp-1.0.jar" );
      Files.write( artifactFile, new byte[ 0 ] );

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ),
                                     dir,
                                     Collections.emptyList(),
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
      final Path dir = FileUtil2.createLocalTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      Files.write( artifactDir.resolve( "myapp-1.0.jar" ), new byte[ 0 ] );
      Files.write( artifactDir.resolve( "myapp-1.0.pom" ), "".getBytes() );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, Collections.emptyList(), true, true );

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
      final Path dir = FileUtil2.createLocalTempDir();
      final Path artifactDir = dir.resolve( "com/example/myapp/1.0" );
      assertTrue( artifactDir.toFile().mkdirs() );
      final Path artifactFile = artifactDir.resolve( "myapp-1.0.jar" );
      Files.write( artifactFile, new byte[ 0 ] );
      Files.write( artifactDir.resolve( "myapp-1.0.pom" ), "".getBytes() );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, Collections.emptyList(), true, false );

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
  public void deriveArtifact_inRemoteRepository()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();
      final Path remoteDir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( remoteDir, "com.example:myapp:1.0" );
      assertTrue( remoteDir.resolve( "com/example/myapp/1.0/myapp-1.0.pom" ).toFile().exists() );
      assertTrue( remoteDir.resolve( "com/example/myapp/1.0/myapp-1.0.jar" ).toFile().exists() );

      assertFalse( dir.resolve( "com/example/myapp/1.0/myapp-1.0.pom" ).toFile().exists() );
      assertFalse( dir.resolve( "com/example/myapp/1.0/myapp-1.0.jar" ).toFile().exists() );

      final TestHandler handler = new TestHandler();

      final RemoteRepository remoteRepository =
        new RemoteRepository.Builder( "local", "default", remoteDir.toUri().toString() ).build();
      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ),
                                     dir,
                                     Collections.singletonList( remoteRepository ),
                                     true,
                                     true );

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
      final Path dir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
      assertTrue( dir.resolve( "com/example/myapp/1.0/myapp-1.0.pom" ).toFile().exists() );
      assertTrue( dir.resolve( "com/example/myapp/1.0/myapp-1.0.jar" ).toFile().exists() );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, Collections.emptyList(), true, true );

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

  @Test
  public void deriveRootDependencies()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:2.5" );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, Collections.emptyList(), true, true );

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "  - coord: com.example:mylib:2.5\n" +
                         // This next dep is unversioned so it is skipped
                         "  - coord: com.example:mydep\n" );
      final ApplicationModel model = loadApplicationModel();

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final List<Dependency> dependencies =
        resolver.deriveRootDependencies( model, ( artifactModel, exceptions ) -> hasFailed.set( true ) );

      assertFalse( hasFailed.get() );
      assertTrue( handler.getRecords().isEmpty() );

      assertEquals( dependencies.size(), 2 );
      final Dependency dependency1 = dependencies.get( 0 );
      assertEquals( dependency1.toString(), "com.example:myapp:jar:1.0 (compile)" );

      final Dependency dependency2 = dependencies.get( 1 );
      assertEquals( dependency2.toString(), "com.example:mylib:jar:2.5 (compile)" );
    } );
  }

  @Test
  public void resolveDependencies()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtB:jar::2.0:runtime",
                                           "org.test4j:core:jar::44.0:test" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtB:2.0",
                                           // Provided ignored by traversal
                                           "com.example:container:jar::4.0:provided",
                                           // System collected but should be ignored at later stage
                                           "com.example:kernel:jar::4.0:system" );

      final TestHandler handler = new TestHandler();

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger( handler ), dir, Collections.emptyList(), true, true );

      writeDependencies( "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      final ApplicationModel model = loadApplicationModel();

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final DependencyResult result =
        resolver.resolveDependencies( model, ( artifactModel, exceptions ) -> hasFailed.set( true ) );

      assertFalse( hasFailed.get() );
      assertTrue( handler.getRecords().isEmpty() );

      assertTrue( result.getCycles().isEmpty() );
      assertTrue( result.getCollectExceptions().isEmpty() );
      final DependencyNode node1 = result.getRoot();
      assertNotNull( node1 );
      assertNull( node1.getArtifact() );
      assertNull( node1.getDependency() );
      final List<DependencyNode> children1 = node1.getChildren();
      assertEquals( children1.size(), 1 );
      final DependencyNode node2 = children1.get( 0 );
      assertEquals( node2.getArtifact().toString(), "com.example:myapp:jar:1.0" );
      final List<DependencyNode> children2 = node2.getChildren();
      assertEquals( children2.size(), 2 );
      final DependencyNode node3 = children2.get( 0 );
      assertEquals( node3.getDependency().toString(), "com.example:mylib:jar:1.0 (compile)" );
      final DependencyNode node4 = children2.get( 1 );
      assertEquals( node4.getDependency().toString(), "com.example:rtA:jar:33.0 (runtime)" );
      assertEquals( node4.getChildren().size(), 0 );
      final List<DependencyNode> children3 = node3.getChildren();
      assertEquals( children3.size(), 1 );
      final DependencyNode node5 = children3.get( 0 );
      assertEquals( node5.getDependency().toString(), "com.example:rtB:jar:2.0 (runtime)" );
      final List<DependencyNode> children4 = node5.getChildren();
      assertEquals( children4.size(), 0 );
    } );
  }

  @Test
  public void resolveDependencies_passingModel()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtB:jar::2.0:runtime",
                                           "org.test4j:core:jar::44.0:test" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtB:2.0",
                                           // Provided ignored by traversal
                                           "com.example:container:jar::4.0:provided",
                                           // System collected but should be ignored at later stage
                                           "com.example:kernel:jar::4.0:system" );

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger(), dir, Collections.emptyList(), true, true );

      writeDependencies( "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      final ApplicationModel model = loadApplicationModel();

      final AtomicBoolean hasFailed = new AtomicBoolean( false );

      final DependencyResult result = resolver.resolveDependencies( model, ( m, e ) -> hasFailed.set( true ) );

      assertFalse( hasFailed.get() );

      assertTrue( result.getCycles().isEmpty() );
      assertTrue( result.getCollectExceptions().isEmpty() );
      assertNotNull( result.getRoot() );
    } );
  }
}
