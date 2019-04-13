package org.realityforge.bazel.depgen.record;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.FileUtil2;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationRecordTest
  extends AbstractTest
{
  @Test
  public void build_simple_noDeps()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord( dir );

      assertNotNull( record.getNode() );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
    } );
  }

  @Test
  public void build_manyDependencies()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
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
                                           // System collected but ignored at later stage
                                           "com.example:kernel:jar::4.0:system" );

      final ApplicationRecord record = loadApplicationRecord( dir );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 4 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp," +
                    "com.example:mylib," +
                    "com.example:rtA," +
                    "com.example:rtB" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtB" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtA" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtB" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtB" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_singleDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord( dir );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_singleRuntimeDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:jar:33.0" );

      final ApplicationRecord record = loadApplicationRecord( dir );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:rtA" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtA" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_versionlessDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "  - coord: com.example:mylib\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord( dir );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_conflicts()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::32.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:32.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );

      final ApplicationRecord record = loadApplicationRecord( dir );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib,com.example:rtA" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtA" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
        assertEquals( artifactRecord.getNode().getDependency().getArtifact().getVersion(), "33.0" );
      }
    } );
  }

  @Test
  public void build_replacement()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "replacements:\n" +
                         "  - coord: com.example:mylib\n" +
                         "    target: \"@com_example//:mylib\"\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord( dir );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertNull( artifactRecord.getReplacementModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertNotNull( artifactRecord.getDeps().get( 0 ).getReplacementModel() );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertNotNull( artifactRecord.getReplacementModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }
}
