package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
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
    throws Exception
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
    throws Exception
  {
    assertEquals( failToProcessOptions(), "Error: No command specified. Please specify a command." );
  }

  @Test
  public void processOptions_defaultDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = failToProcessOptions( "generate" );
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

      final String output = failToProcessOptions( "generate", "Bleep" );
      assertOutputContains( output, "Error: Unknown command: Bleep" );
    } );
  }

  @Test
  public void processOptions_specifiedDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = failToProcessOptions( "--dependencies-file", "deps.txt", "generate" );
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

      final String output = failToProcessOptions( "--cache-directory", "StoreMeHere", "generate" );
      assertOutputContains( output,
                            "Error: Specified cache directory exists but is not a directory. Specified value: StoreMeHere" );
    } );
  }

  @Test
  public void processOptions_duplicateCommand()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = failToProcessOptions( "generate", "generate" );
      assertOutputContains( output, "Error: Duplicate command specified: generate" );
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

      final String output = failToProcessOptions( "--settings-file", "some_settings.xml", "generate" );
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

      final String output = failToProcessOptions( "--help" );
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

      final String output = failToProcessOptions( "--some-command-no-exist" );
      assertEquals( output, "Error: Unknown option --some-command-no-exist" );
    } );
  }

  @Test
  public void processOptions_default()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );
      environment.setCacheDir( null );
      assertTrue( Main.processOptions( environment, "generate" ) );
      assertTrue( environment.hasCommand() );
      assertEquals( environment.getCommand(), "generate" );
      assertEquals( environment.getDependenciesFile(), FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
      assertEquals( environment.getSettingsFile(),
                    Paths.get( System.getProperty( "user.home" ), ".m2", "settings.xml" )
                      .toAbsolutePath()
                      .normalize() );
      assertFalse( environment.hasCacheDir() );
      assertOutputContains( handler.toString(), "Bazel DepGen Starting..." );
    } );
  }

  @Test
  public void processOptions_specifyDependenciesFile()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      FileUtil.write( "dependencies2.yml", "" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );
      assertTrue( Main.processOptions( environment, "--dependencies-file", "dependencies2.yml", "generate" ) );
      assertEquals( environment.getDependenciesFile(), FileUtil.getCurrentDirectory().resolve( "dependencies2.yml" ) );
    } );
  }

  @Test
  public void processOptions_specifyCacheDir()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );
      final Path dir = FileUtil.createLocalTempDir();

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );
      assertTrue( Main.processOptions( environment, "--cache-directory", dir.toString(), "generate" ) );
      assertTrue( environment.hasCacheDir() );
      assertEquals( environment.getCacheDir(), dir );
    } );
  }

  @Test
  public void processOptions_specifySettingsFile()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n" +
                         "  - name: central" +
                         "    url: http://repo1.maven.org/maven2\n" );

      FileUtil.write( "settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );
      final Path path = FileUtil.getCurrentDirectory().resolve( "settings.xml" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );
      assertTrue( Main.processOptions( environment, "--settings-file", path.toString(), "generate" ) );
      assertTrue( environment.hasSettingsFile() );
      assertEquals( environment.getSettingsFile(), path );
    } );
  }

  @Test
  public void processOptions_verbose()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );
      environment.logger().setLevel( Level.OFF );
      assertTrue( Main.processOptions( environment, "--verbose", "generate" ) );
      assertEquals( environment.logger().getLevel(), Level.ALL );
    } );
  }

  @Test
  public void processOptions_quiet()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );
      environment.logger().setLevel( Level.OFF );
      assertTrue( Main.processOptions( environment, "--quiet", "generate" ) );
      assertEquals( environment.logger().getLevel(), Level.WARNING );
    } );
  }

  @Test
  public void processOptions_reset_cached_metadata()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );
      assertFalse( environment.shouldResetCachedMetadata() );
      assertTrue( Main.processOptions( environment, "--reset-cached-metadata", "generate" ) );
      assertTrue( environment.shouldResetCachedMetadata() );
    } );
  }

  @Test
  public void loadDependenciesYaml()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:zeapp:2.0\n" );

      final Environment environment = newEnvironment();
      final Path file = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( file );
      final ApplicationConfig config = Main.loadDependenciesYaml( environment );
      assertEquals( config.getConfigLocation(), file );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );
      assertEquals( artifacts.size(), 1 );
    } );
  }

  @Test
  public void loadDependenciesYaml_error()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final Environment environment = newEnvironment();
      final Path file = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( file );
      final TerminalStateException exception =
        expectThrows( TerminalStateException.class, () -> Main.loadDependenciesYaml( environment ) );
      assertEquals( exception.getMessage(), "Error: Failed to read dependencies file " + file );
      assertEquals( exception.getExitCode(), ExitCodes.ERROR_PARSING_DEPENDENCIES_CODE );
    } );
  }

  @Test
  public void loadModel()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:zeapp:2.0\n" );

      final Environment environment = newEnvironment();

      final Path file = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( file );

      final ApplicationModel model = Main.loadModel( environment );
      assertEquals( model.getConfigLocation(), file );
      assertFalse( model.shouldResetCachedMetadata() );
      final List<ArtifactModel> artifacts = model.getArtifacts();
      assertNotNull( artifacts );
      assertEquals( artifacts.size(), 1 );
    } );
  }

  @Test
  public void loadModel_resetCachedMetadata()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:zeapp:2.0\n" );

      final Environment environment = newEnvironment();
      environment.markResetCachedMetadata();

      final Path file = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( file );

      final ApplicationModel model = Main.loadModel( environment );
      assertEquals( model.getConfigLocation(), file );
      assertTrue( model.shouldResetCachedMetadata() );
      final List<ArtifactModel> artifacts = model.getArtifacts();
      assertNotNull( artifacts );
      assertEquals( artifacts.size(), 1 );
    } );
  }

  @Test
  public void loadRecord_noDependencies()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir, "" );

      final Environment environment = newEnvironment();

      final Path file = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( file );
      environment.setSettingsFile( FileUtil.getCurrentDirectory().resolve( "settings.xml" ) );
      environment.setCacheDir( FileUtil.createLocalTempDir() );

      final ApplicationRecord record = Main.loadRecord( environment );
      assertEquals( record.getSource().getConfigLocation(), file );
      assertEquals( record.getArtifacts().size(), 0 );
      assertEquals( record.getAuthenticationContexts().size(), 0 );
    } );
  }

  @Test
  public void loadRecord_singleDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final Environment environment = newEnvironment();

      environment.setDependenciesFile( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
      environment.setSettingsFile( FileUtil.getCurrentDirectory().resolve( "settings.xml" ) );
      environment.setCacheDir( FileUtil.createLocalTempDir() );

      final ApplicationRecord record = Main.loadRecord( environment );
      assertEquals( record.getArtifacts().size(), 1 );
    } );
  }

  @Test
  public void loadRecord_invalidSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir, "" );

      final Environment environment = newEnvironment();

      environment.setDependenciesFile( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      environment.setSettingsFile( settingsFile );
      environment.setCacheDir( FileUtil.createLocalTempDir() );

      FileUtil.write( settingsFile.toString(), "JHSGDJHDS()*&(&Y*&" );

      final TerminalStateException exception =
        expectThrows( TerminalStateException.class, () -> Main.loadRecord( environment ) );
      assertEquals( exception.getMessage(), "Error: Problem loading settings from " + settingsFile );
      assertEquals( exception.getExitCode(), ExitCodes.ERROR_LOADING_SETTINGS_CODE );
    } );
  }

  @Test
  public void loadRecord_validSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final Environment environment = newEnvironment();

      environment.setDependenciesFile( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      environment.setSettingsFile( settingsFile );
      FileUtil.write( settingsFile.toString(),
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>local</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );

      environment.setCacheDir( FileUtil.createLocalTempDir() );

      final ApplicationRecord record = Main.loadRecord( environment );
      assertEquals( record.getAuthenticationContexts().size(), 1 );
    } );
  }

  @Test
  public void loadRecord_ensureInvalidCacheMessagePropagated()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );

      environment.setDependenciesFile( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
      environment.setSettingsFile( FileUtil.getCurrentDirectory().resolve( "settings.xml" ) );
      final Path cacheDir = FileUtil.createLocalTempDir();
      environment.setCacheDir( cacheDir );

      final Path metadataCache = cacheDir.resolve( "com/example/myapp/1.0" ).resolve( DepgenMetadata.FILENAME );
      Files.createDirectories( metadataCache.getParent() );
      Files.write( metadataCache, ( "<default>.local.url=badUrl\n" ).getBytes( StandardCharsets.ISO_8859_1 ) );

      Main.loadRecord( environment );
      assertOutputContains( handler.toString(),
                            "Cache entry '<default>.local.url' for artifact 'com.example:myapp:jar:1.0' contains a url 'badUrl' that does not match the repository url '" );
    } );
  }

  @Test
  public void loadRecord_circularDependencies()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:myapp:1.0" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );

      environment.setDependenciesFile( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
      environment.setSettingsFile( FileUtil.getCurrentDirectory().resolve( "settings.xml" ) );
      final Path cacheDir = FileUtil.createLocalTempDir();
      environment.setCacheDir( cacheDir );

      final TerminalStateException exception =
        expectThrows( TerminalStateException.class, () -> Main.loadRecord( environment ) );
      assertNull( exception.getMessage() );
      assertEquals( exception.getExitCode(), ExitCodes.ERROR_CYCLES_PRESENT_CODE );
      assertOutputContains( handler.toString(),
                            "1 dependency cycles detected when collecting dependencies:\n" +
                            "com.example:myapp:jar -> com.example:mylib:jar -> com.example:myapp:jar" );
    } );
  }

  @Test
  public void loadRecord_errorResolving()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );

      final TestHandler handler = new TestHandler();
      final Environment environment = newEnvironment( createLogger( handler ) );

      environment.setDependenciesFile( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
      environment.setSettingsFile( FileUtil.getCurrentDirectory().resolve( "settings.xml" ) );
      final Path cacheDir = FileUtil.createLocalTempDir();
      environment.setCacheDir( cacheDir );
      final TerminalStateException exception;
      try
      {
        //Make the cache directory un-writeable and thus error saving pom
        Files.setPosixFilePermissions( cacheDir, new HashSet<>() );

        exception = expectThrows( TerminalStateException.class, () -> Main.loadRecord( environment ) );
      }
      finally
      {
        final HashSet<PosixFilePermission> perms = new HashSet<>();
        perms.add( PosixFilePermission.OWNER_READ );
        perms.add( PosixFilePermission.OWNER_WRITE );
        Files.setPosixFilePermissions( cacheDir, perms );
      }
      assertNull( exception.getMessage() );
      assertEquals( exception.getExitCode(), ExitCodes.ERROR_INVALID_POM_CODE );
      assertOutputContains( handler.toString(), "Transfer Failed: com/example/myapp/1.0/myapp-1.0.jar" );
      assertOutputContains( handler.toString(),
                            "Could not transfer artifact com.example:myapp:jar:1.0 from/to local (" );
    } );
  }

  @Test
  public void setupLogger()
    throws Exception
  {
    final Logger logger = Logger.getAnonymousLogger();
    final Environment environment = newEnvironment( logger );
    assertEquals( logger.getHandlers().length, 0 );
    assertTrue( logger.getUseParentHandlers() );
    logger.setLevel( Level.OFF );
    Main.setupLogger( environment );
    assertEquals( logger.getHandlers().length, 1 );
    assertEquals( logger.getLevel(), Level.INFO );
    assertFalse( logger.getUseParentHandlers() );
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
      environment.setCacheDir( null );
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
      final Environment environment = newEnvironment();
      environment.setCacheDir( null );
      final TerminalStateException exception =
        expectThrows( TerminalStateException.class,
                      () -> Main.getCacheDirectory( environment, loadApplicationModel() ) );
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
                         "  - coord: com.example:myapp:1.0\n" +
                         "    includeSource: false\n" );
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
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", jarFile1 );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
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
                    "Installed artifact 'com.example:myapp:jar:sources:1.0' into repository cache.\n" +
                    "Installed artifact 'com.example:mylib:jar:1.0' into repository cache." );

      assertTrue( Files.exists( targetFile1 ) );
      assertTrue( Files.exists( targetFile2 ) );
    } );
  }

  @Test
  public void printGraph()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final TestHandler handler = new TestHandler();
      Main.printGraph( newEnvironment( createLogger( handler ) ), loadApplicationRecord() );
      final String output = handler.toString();
      assertEquals( output,
                    "Dependency Graph:\n" +
                    "\\- com.example:myapp:jar:1.0 [compile]" );
    } );
  }

  @Test
  public void generate()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      final String url = dir.toUri().toString();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final TestHandler handler = new TestHandler();
      Main.generate( loadApplicationRecord() );
      assertEquals( handler.toString(), "" );

      assertEquals( loadAsString( FileUtil.getCurrentDirectory().resolve( "thirdparty/BUILD.bazel" ) ),
                    "# File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen\n" +
                    "# Contents can be edited and will not be overridden.\n" +
                    "package(default_visibility = [\"//visibility:public\"])\n" +
                    "\n" +
                    "load(\"//thirdparty:dependencies.bzl\", \"generate_targets\")\n" +
                    "\n" +
                    "generate_targets()\n" );
      assertEquals( loadAsString( FileUtil.getCurrentDirectory().resolve( "thirdparty/dependencies.bzl" ) ),
                    "# DO NOT EDIT: File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen\n" +
                    "\n" +
                    "\"\"\"\n" +
                    "    Macro rules to load dependencies defined in '../dependencies.yml'.\n" +
                    "\n" +
                    "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                    "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                    "\"\"\"\n" +
                    "# Dependency Graph Generated from the input data\n" +
                    "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                    "\n" +
                    "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n" +
                    "\n" +
                    "def generate_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + url + "com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                    "    )\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0__sources\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + url + "com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                    "    )\n" +
                    "\n" +
                    "def generate_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp\",\n" +
                    "        actual = \":com_example__myapp__1_0\",\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                    "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void generate_directoryIsAFile()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      final String url = dir.toUri().toString();

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      FileUtil.write( FileUtil.getCurrentDirectory().resolve( "thirdparty" ).toString(), "" );

      assertThrows( FileNotFoundException.class, () -> Main.generate( loadApplicationRecord() ) );
    } );
  }

  @Test
  public void hash()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      final TestHandler handler = new TestHandler();
      Main.hash( newEnvironment( createLogger( handler ) ), loadApplicationModel() );
      assertEquals( handler.toString(),
                    "Content SHA256: 68814747A184F6E9A415AC0B97061A8ED1A79E487364555F3BAE5E0B0785DA39" );
    } );
  }

  @Nonnull
  private String failToProcessOptions( @Nonnull final String... args )
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( createLogger( handler ) );
    assertFalse( Main.processOptions( environment, args ) );
    return handler.toString();
  }
}
