package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.record.ArtifactRecord;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class MainTest
  extends AbstractTest
{
  @Test
  public void printUsage()
  {
    final TestHandler handler = new TestHandler();
    Main.printUsage( newEnvironment( createLogger( handler ) ) );
    assertEquals( handler.toString(),
                  "java org.realityforge.bazel.depgen.Main [options] [command]\n" +
                  "\tPossible Commands:\n" +
                  "\t\tgenerate: Generate the bazel extension from the dependency configuration.\n" +
                  "\t\tprint-graph: Compute and print the dependency graph for the dependency configuration.\n" +
                  "\t\thash: Generate a hash of the content of the dependency configuration.\n" +
                  "\tOptions:\n" +
                  "\t-h, --help\n" +
                  "\t\tprint this message and exit\n" +
                  "\t-q, --quiet\n" +
                  "\t\tDo not output unless an error occurs.\n" +
                  "\t-v, --verbose\n" +
                  "\t\tVerbose output of differences.\n" +
                  "\t-d, --dependencies-file <argument>\n" +
                  "\t\tThe path to the yaml file containing the dependencies. Defau\n" +
                  "\t\tlts to 'dependencies.yml' in the workspace directory.\n" +
                  "\t-s, --settings-file <argument>\n" +
                  "\t\tThe path to the settings.xml used by Maven to extract reposi\n" +
                  "\t\ttory credentials. Defaults to '~/.m2/settings.xml'.\n" +
                  "\t-r, --cache-directory <argument>\n" +
                  "\t\tThe path to the directory in which to cache downloads from r\n" +
                  "\t\temote repositories. Defaults to \"$(bazel info output_base)/.\n" +
                  "\t\tdepgen-cache\".\n" +
                  "\t--reset-cached-metadata\n" +
                  "\t\tRecalculate metadata about an artifact." );
  }

  @Test
  public void processOptions_noCommand()
  {
    assertEquals( processOptions( false ), "Error: No command specified. Please specify a command." );
  }

  @Test
  public void processOptions_defaultDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = processOptions( false, "generate" );
      assertOutputContains( output, "Error: Default dependencies file does not exist: " );
    } );
  }

  @Test
  public void processOptions_unexpectedArgument()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( false, "generate", "Bleep" );
      assertOutputContains( output, "Error: Unknown command: Bleep" );
    } );
  }

  @Test
  public void processOptions_specifiedDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = processOptions( false, "--dependencies-file", "deps.txt", "generate" );
      assertOutputContains( output, "Error: Specified dependencies file does not exist. Specified value: deps.txt" );
    } );
  }

  @Test
  public void processOptions_specifiedCacheDirectoryNotDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );
      FileUtil.write( "StoreMeHere", "NotADir" );

      final String output = processOptions( false, "--cache-directory", "StoreMeHere", "generate" );
      assertOutputContains( output,
                            "Error: Specified cache directory exists but is not a directory. Specified value: StoreMeHere" );
    } );
  }

  @Test
  public void processOptions_missingSpecifiedSettings()
    throws Exception
  {
    // Need to declare repositories otherwise we never even try to load settings
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n" +
                         "  - name: central" +
                         "    url: http://repo1.maven.org/maven2\n" );

      final String output = processOptions( false, "--settings-file", "some_settings.xml", "generate" );
      assertOutputContains( output,
                            "Error: Specified settings file does not exist. Specified value: some_settings.xml" );
    } );
  }

  @Test
  public void processOptions_help()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( false, "--help" );
      assertOutputContains( output, "-h, --help\n" );
      assertOutputContains( output, "-q, --quiet\n" );
      assertOutputContains( output, "-v, --verbose\n" );
      assertOutputContains( output, "-d, --dependencies-file <argument>\n" );
      assertOutputContains( output, "-s, --settings-file <argument>\n" );
      assertOutputContains( output, "-r, --cache-directory <argument>\n" );
      assertOutputContains( output, "--reset-cached-metadata\n" );
    } );
  }

  @Test
  public void processOptions_error()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( false, "--some-command-no-exist" );
      assertEquals( output, "Error: Unknown option --some-command-no-exist" );
    } );
  }

  @Test
  public void printBanner_cacheExplicitlySpecified()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Logger logger = createLogger( handler );
      final Environment environment = newEnvironment( logger );
      final Path dependenciesFile = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( dependenciesFile );
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      environment.setSettingsFile( settingsFile );
      final Path cacheDir = FileUtil.getCurrentDirectory().resolve( ".depgen-cache" );
      environment.setCacheDir( cacheDir );
      Main.printBanner( environment );
      final String output = handler.toString();
      assertOutputContains( output, "Bazel DepGen Starting...\n" );
      assertOutputContains( output, "\n  Dependencies file: " + dependenciesFile );
      assertOutputContains( output, "\n  Settings file: " + settingsFile );
      assertOutputContains( output, "\n  Cache directory: " + cacheDir );
    } );
  }

  @Test
  public void printBanner()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Logger logger = createLogger( handler );
      final Environment environment = newEnvironment( logger );
      final Path dependenciesFile = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( dependenciesFile );
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      environment.setSettingsFile( settingsFile );
      Main.printBanner( environment );
      final String output = handler.toString();
      assertOutputContains( output, "Bazel DepGen Starting...\n" );
      assertOutputContains( output, "\n  Dependencies file: " + dependenciesFile );
      assertOutputContains( output, "\n  Settings file: " + settingsFile );
      assertOutputDoesNotContain( output, "\n  Cache directory: " );
    } );
  }

  @Test
  public void getCacheDirectory_default()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final Path cacheDirectory = Main.getCacheDirectory( newEnvironment(), loadApplicationModel() );
      assertNotNull( cacheDirectory );
    } );
  }

  @Test
  public void getCacheDirectory_explicitlySpecified()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final Environment environment = newEnvironment();
      final Path cacheDir = FileUtil.createLocalTempDir();
      environment.setCacheDir( cacheDir );
      final Path cacheDirectory = Main.getCacheDirectory( environment, loadApplicationModel() );
      assertEquals( cacheDirectory, cacheDir );
    } );
  }

  @Test
  public void getCacheDirectory_outsideWorkspace()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      final TerminalStateException exception =
        expectThrows( TerminalStateException.class,
                      () -> Main.getCacheDirectory( newEnvironment(), loadApplicationModel() ) );
      assertEquals( exception.getMessage(),
                    "Error: Cache directory not specified and unable to derive default directory (Is the bazel command on the path?). Explicitly pass the cache directory as an option." );
      assertEquals( exception.getExitCode(), ExitCodes.ERROR_INVALID_DEFAULT_CACHE_DIR_CODE );
    } );
  }

  @Test
  public void cacheRepositoryFile()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final Path cacheDir = FileUtil.createLocalTempDir();
      final Path file = FileUtil.createLocalTempDir().resolve( "somefile.txt" );
      final byte[] content = { 1, 2, 3, 4 };
      Files.write( file, content );

      final String sha256 = "9F64A747E1B97F131FABB6B447296C9B6F0201E79FB3C5356E6C77E89B6A806A";

      final Path targetFile =
        cacheDir.resolve( "content_addressable" ).resolve( "sha256" ).resolve( sha256 ).resolve( "file" );

      final TestHandler handler = new TestHandler();

      assertFalse( Files.exists( targetFile ) );

      Main.cacheRepositoryFile( createLogger( handler ), cacheDir, "myartifact", file.toFile(), sha256 );
      assertEquals( handler.toString(), "Installed artifact 'myartifact' into repository cache." );

      assertTrue( Files.exists( targetFile ) );
      assertEquals( Files.readAllBytes( targetFile ), content );
    } );
  }

  @Test
  public void cacheRepositoryFile_alreadyExists()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final Path cacheDir = FileUtil.createLocalTempDir();
      final Path file = FileUtil.createLocalTempDir().resolve( "somefile.txt" );

      Files.write( file, new byte[]{ 1, 2, 3, 4 } );

      final String sha256 = "9F64A747E1B97F131FABB6B447296C9B6F0201E79FB3C5356E6C77E89B6A806A";

      final Path targetFile =
        cacheDir.resolve( "content_addressable" ).resolve( "sha256" ).resolve( sha256 ).resolve( "file" );

      // Writing cacheContent into cache that differs from actual content so we can tell if it has been updated
      final byte[] cacheContent = { 1, 2, 3, 4, 0 };
      Files.createDirectories( targetFile.getParent() );
      Files.write( targetFile, cacheContent );

      final TestHandler handler = new TestHandler();

      assertTrue( Files.exists( targetFile ) );

      Main.cacheRepositoryFile( createLogger( handler ), cacheDir, "myartifact", file.toFile(), sha256 );
      assertEquals( handler.toString(), "" );

      assertTrue( Files.exists( targetFile ) );
      assertEquals( Files.readAllBytes( targetFile ), cacheContent );
    } );
  }

  @Test
  public void cacheRepositoryFile_FailedToWrite()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final Path cacheDir = FileUtil.createLocalTempDir();
      final Path file = FileUtil.createLocalTempDir().resolve( "somefile.txt" );
      final byte[] content = { 1, 2, 3, 4 };
      Files.write( file, content );

      final String sha256 = "9F64A747E1B97F131FABB6B447296C9B6F0201E79FB3C5356E6C77E89B6A806A";

      final Path targetFile =
        cacheDir.resolve( "content_addressable" ).resolve( "sha256" ).resolve( sha256 ).resolve( "file" );

      final File dir = targetFile.getParent().toFile();
      assertTrue( dir.mkdirs() );
      assertTrue( dir.setWritable( false ) );

      final TestHandler handler = new TestHandler();

      assertFalse( Files.exists( targetFile ) );

      Main.cacheRepositoryFile( createLogger( handler ), cacheDir, "myartifact", file.toFile(), sha256 );
      assertEquals( handler.toString(), "Failed to cache artifact 'myartifact' in repository cache." );

      assertFalse( Files.exists( targetFile ) );
    } );
  }

  @Test
  public void cacheArtifactsInRepositoryCache()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path repositoryCacheDir = FileUtil.createLocalTempDir();
      writeBazelrc( repositoryCacheDir );
      FileUtil.write( "WORKSPACE", "" );
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile1 = createJarFile( ValueUtil.randomString() + ".jar", ValueUtil.randomString() );
      final Path jarFile2 = createJarFile( ValueUtil.randomString() + ".jar", ValueUtil.randomString() );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0", jarFile1 );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile2 );

      final ApplicationRecord record = loadApplicationRecord();

      final ArtifactRecord artifactRecord = record.getArtifacts().get( 0 );
      final String sha256 = artifactRecord.getSha256();
      assertNotNull( sha256 );
      final String sourceSha256 = artifactRecord.getSourceSha256();
      assertNotNull( sourceSha256 );

      final Path cacheBase = repositoryCacheDir.resolve( "content_addressable" ).resolve( "sha256" );
      final Path targetFile = cacheBase.resolve( sha256 ).resolve( "file" );
      final Path sourceTargetFile = cacheBase.resolve( sourceSha256 ).resolve( "file" );

      final TestHandler handler = new TestHandler();

      assertFalse( Files.exists( targetFile ) );
      assertFalse( Files.exists( sourceTargetFile ) );

      final Environment environment = newEnvironment( createLogger( handler ) );
      Main.cacheArtifactsInRepositoryCache( environment, record );
      assertEquals( handler.toString(),
                    "Installed artifact 'com.example:myapp:jar:1.0' into repository cache.\n" +
                    "Installed artifact 'com.example:myapp:jar:sources:1.0' into repository cache." );

      assertTrue( Files.exists( targetFile ) );
      assertTrue( Files.exists( sourceTargetFile ) );
    } );
  }

  @Test
  public void cacheArtifactsInRepositoryCache_minusSourcesClassifier()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path repositoryCacheDir = FileUtil.createLocalTempDir();
      writeBazelrc( repositoryCacheDir );
      FileUtil.write( "WORKSPACE", "" );
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile2 = createJarFile( ValueUtil.randomString() + ".jar", ValueUtil.randomString() );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile2 );

      final ApplicationRecord record = loadApplicationRecord();

      final ArtifactRecord artifactRecord = record.getArtifacts().get( 0 );
      final String sha256 = artifactRecord.getSha256();
      assertNotNull( sha256 );
      assertNull( artifactRecord.getSourceSha256() );

      final Path cacheBase = repositoryCacheDir.resolve( "content_addressable" ).resolve( "sha256" );
      final Path targetFile = cacheBase.resolve( sha256 ).resolve( "file" );

      final TestHandler handler = new TestHandler();

      assertFalse( Files.exists( targetFile ) );

      final Environment environment = newEnvironment( createLogger( handler ) );
      Main.cacheArtifactsInRepositoryCache( environment, record );
      assertEquals( handler.toString(),
                    "Installed artifact 'com.example:myapp:jar:1.0' into repository cache." );

      assertTrue( Files.exists( targetFile ) );
    } );
  }

  @Test
  public void cacheArtifactsInRepositoryCache_multipleArtifacts()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path repositoryCacheDir = FileUtil.createLocalTempDir();
      writeBazelrc( repositoryCacheDir );
      FileUtil.write( "WORKSPACE", "" );
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile1 = createJarFile( ValueUtil.randomString() + ".jar", ValueUtil.randomString() );
      final Path jarFile2 = createJarFile( ValueUtil.randomString() + ".jar", ValueUtil.randomString() );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", jarFile1 );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile2, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      final String file1Sha256 = artifacts.get( 0 ).getSha256();
      final String file2Sha256 = artifacts.get( 1 ).getSha256();
      assertNotNull( file1Sha256 );
      assertNotNull( file2Sha256 );

      final Path cacheBase = repositoryCacheDir.resolve( "content_addressable" ).resolve( "sha256" );
      final Path targetFile1 = cacheBase.resolve( file1Sha256 ).resolve( "file" );
      final Path targetFile2 = cacheBase.resolve( file2Sha256 ).resolve( "file" );

      final TestHandler handler = new TestHandler();

      assertFalse( Files.exists( targetFile1 ) );
      assertFalse( Files.exists( targetFile2 ) );

      final Environment environment = newEnvironment( createLogger( handler ) );
      Main.cacheArtifactsInRepositoryCache( environment, record );
      assertEquals( handler.toString(),
                    "Installed artifact 'com.example:myapp:jar:1.0' into repository cache.\n" +
                    "Installed artifact 'com.example:mylib:jar:1.0' into repository cache." );

      assertTrue( Files.exists( targetFile1 ) );
      assertTrue( Files.exists( targetFile2 ) );
    } );
  }

  @SuppressWarnings( "SameParameterValue" )
  @Nonnull
  private String processOptions( final boolean expectedResult, @Nonnull final String... args )
  {
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( createLogger( handler ) );
    final boolean result = Main.processOptions( environment, args );
    assertEquals( expectedResult, result, "Return value for Main.processOptions" );
    return handler.toString();
  }
}
