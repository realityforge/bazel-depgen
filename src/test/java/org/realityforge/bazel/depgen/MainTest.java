package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.io.File;
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
    final Environment environment = newEnvironment( handler );
    // We set the level to warning which is equivalent to --quiet so that we know that output
    // occurs even if the --quiet argument is passed.
    environment.logger().setLevel( Level.WARNING );
    Main.printUsage( environment );
    assertEquals( handler.toString(),
                  "java org.realityforge.bazel.depgen.Main [options] [command]\n" +
                  "\tPossible Commands:\n" +
                  "\t\tgenerate: Generate the bazel extension from the dependency configuration.\n" +
                  "\t\tprint-graph: Compute and print the dependency graph for the dependency configuration.\n" +
                  "\t\thash: Generate a hash of the content of the dependency configuration.\n" +
                  "\t\tinit: Initialize an empty dependency configuration and workspace infrastructure.\n" +
                  "\t\tinfo: Print runtime info about the tool.\n" +
                  "\tOptions:\n" +
                  "\t--version\n" +
                  "\t\tprint the version and exit\n" +
                  "\t-h, --help\n" +
                  "\t\tprint this message and exit\n" +
                  "\t-q, --quiet\n" +
                  "\t\tDo not output unless an error occurs.\n" +
                  "\t-v, --verbose\n" +
                  "\t\tVerbose output of differences.\n" +
                  "\t-d, --directory <argument>\n" +
                  "\t\tThe directory to run the tool from.\n" +
                  "\t-c, --config-file <argument>\n" +
                  "\t\tThe path to the yaml file containing the dependency configur\n" +
                  "\t\tation. Defaults to 'thirdparty/dependencies.yml'.\n" +
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
  public void processOptions_defaultConfigFileMissing()
    throws Exception
  {
    writeWorkspace();

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setConfigFile( null );
    environment.setSettingsFile( null );
    assertFalse( Main.processOptions( environment, "generate" ) );
    final String output = handler.toString();
    assertOutputContains( output, "Error: Default config file does not exist: " );
  }

  @Test
  public void processOptions_unexpectedArgument()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final String output = failToProcessOptions( "Bleep" );
    assertOutputContains( output, "Error: Unknown command: Bleep" );
  }

  @Test
  public void processOptions_specifiedConfigFileMissing()
    throws Exception
  {
    writeWorkspace();

    final String output = failToProcessOptions( "--config-file", "deps.txt", "generate" );
    assertOutputContains( output, "Error: Specified config file does not exist. Specified value: " +
                                  FileUtil.getCurrentDirectory().resolve( "deps.txt" ) );
  }

  @Test
  public void processOptions_configFileMissing_initSubCommand()
    throws Exception
  {
    writeWorkspace();

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    assertFalse( environment.shouldResetCachedMetadata() );
    assertTrue( Main.processOptions( environment, "init" ) );
    assertFalse( environment.getConfigFile().toFile().exists() );
    assertEquals( handler.toString(), "" );
  }

  @Test
  public void processOptions_specifiedCacheDirectoryNotDirectory()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );
    FileUtil.write( "StoreMeHere", "NotADir" );

    final String output = failToProcessOptions( "--cache-directory", "StoreMeHere", "generate" );
    assertOutputContains( output,
                          "Error: Specified cache directory exists but is not a directory. Specified value: StoreMeHere" );
  }

  @Test
  public void processOptions_specifiedDirectoryNotDirectory()
    throws Exception
  {
    writeWorkspace();
    FileUtil.write( "RunMeHere", "NotADir" );

    final String output = failToProcessOptions( "--directory", "RunMeHere", "generate" );
    assertOutputContains( output,
                          "Error: Specified directory is not a directory. Specified value: RunMeHere" );
  }

  @Test
  public void processOptions_specifiedDirectoryNoExist()
    throws Exception
  {
    writeWorkspace();

    final String output = failToProcessOptions( "--directory", "RunMeHere", "generate" );
    assertOutputContains( output,
                          "Error: Specified directory does not exist. Specified value: RunMeHere" );
  }

  @Test
  public void processOptions_specifyDirectory()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    FileUtil.write( dir.resolve( ".bazelrc" ), "build --repository_cache " + FileUtil.createLocalTempDir() + "\n" );
    FileUtil.write( dir.resolve( "WORKSPACE" ), "" );
    final Path configFile = dir.resolve( "thirdparty" ).resolve( ApplicationConfig.FILENAME );
    FileUtil.write( configFile, "" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setConfigFile( null );
    environment.logger().setLevel( Level.INFO );
    final int exitCode = Main.run( environment, "--directory", dir.toString(), "info", "config-file" );
    assertEquals( ExitCodes.SUCCESS_EXIT_CODE, exitCode );
    assertEquals( environment.currentDirectory(), dir );
    assertEquals( handler.toString(), "config-file=" + configFile );
  }

  @Test
  public void processOptions_unknownArgumentTCommand()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final String output = failToProcessOptions( "generate", "generate" );
    assertOutputContains( output, "Error: Unknown arguments to generate command. Arguments: [generate]" );
  }

  @Test
  public void processOptions_missingSpecifiedSettings()
    throws Exception
  {
    // Need to declare repositories otherwise we never even try to load settings
    writeWorkspace();
    // Need to declare repositories otherwise we never even try to load settings
    writeConfigFile( "repositories:\n" +
                     "  - name: central" +
                     "    url: http://repo1.maven.org/maven2\n" );

    final String output = failToProcessOptions( "--settings-file", "some_settings.xml", "generate" );
    assertOutputContains( output,
                          "Error: Specified settings file does not exist. Specified value: some_settings.xml" );
  }

  @Test
  public void processOptions_help()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final String output = failToProcessOptions( "--help" );
    assertOutputContains( output, "-h, --help\n" );
    assertOutputContains( output, "-q, --quiet\n" );
    assertOutputContains( output, "-v, --verbose\n" );
    assertOutputContains( output, "-c, --config-file <argument>\n" );
    assertOutputContains( output, "-s, --settings-file <argument>\n" );
    assertOutputContains( output, "-r, --cache-directory <argument>\n" );
    assertOutputContains( output, "--reset-cached-metadata\n" );
  }

  @Test
  public void processOptions_version()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final String output = failToProcessOptions( "--version" );
    assertEquals( output, "DepGen Version: 1" );
  }

  @Test
  public void processOptions_error()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final String output = failToProcessOptions( "--some-command-no-exist" );
    assertEquals( output, "Error: Unknown option --some-command-no-exist" );
  }

  @Test
  public void processOptions_default()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setConfigFile( null );
    environment.setSettingsFile( null );
    assertTrue( Main.processOptions( environment, "generate" ) );
    assertTrue( environment.hasCommand() );
    assertTrue( environment.getCommand() instanceof GenerateCommand );
    assertEquals( environment.getConfigFile(), getDefaultConfigFile() );
    assertEquals( environment.getSettingsFile(),
                  Paths
                    .get( System.getProperty( "user.home" ), ".m2", "settings.xml" )
                    .toAbsolutePath()
                    .normalize() );
  }

  @Test
  public void processOptions_specifyConfigFile()
    throws Exception
  {
    writeWorkspace();
    FileUtil.write( "dependencies2.yml", "" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    assertTrue( Main.processOptions( environment, "--config-file", "dependencies2.yml", "generate" ) );
    assertEquals( environment.getConfigFile(), FileUtil.getCurrentDirectory().resolve( "dependencies2.yml" ) );
  }

  @Test
  public void processOptions_specifyCacheDir()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );
    final Path dir = FileUtil.createLocalTempDir();

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    assertTrue( Main.processOptions( environment, "--cache-directory", dir.toString(), "generate" ) );
    assertTrue( environment.hasCacheDir() );
    assertEquals( environment.getCacheDir(), dir );
  }

  @Test
  public void processOptions_whenCacheDirectoryUnspecifiedAndNotInvokedWIthinWorkspace()
    throws Exception
  {
    FileUtil.write( "dependencies2.yml", "" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setCacheDir( null );
    assertFalse( Main.processOptions( environment, "--config-file", "dependencies2.yml", "generate" ) );
    assertOutputContains( handler.toString(),
                          "Error: Cache directory not specified and unable to derive default directory (Is the bazel command on the path?). Explicitly pass the cache directory as an option." );

    assertFalse( environment.hasCacheDir() );
  }

  @Test
  public void processOptions_specifySettingsFile()
    throws Exception
  {
    // Need to declare repositories otherwise we never even try to load settings
    writeWorkspace();
    // Need to declare repositories otherwise we never even try to load settings
    writeConfigFile( "repositories:\n" +
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
    final Environment environment = newEnvironment( handler );
    assertTrue( Main.processOptions( environment, "--settings-file", path.toString(), "generate" ) );
    assertTrue( environment.hasSettingsFile() );
    assertEquals( environment.getSettingsFile(), path );
  }

  @Test
  public void processOptions_verbose()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.logger().setLevel( Level.OFF );
    assertTrue( Main.processOptions( environment, "--verbose", "generate" ) );
    assertEquals( environment.logger().getLevel(), Level.ALL );
  }

  @Test
  public void processOptions_quiet()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.logger().setLevel( Level.OFF );
    assertTrue( Main.processOptions( environment, "--quiet", "generate" ) );
    assertEquals( environment.logger().getLevel(), Level.WARNING );
  }

  @Test
  public void processOptions_reset_cached_metadata()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    assertFalse( environment.shouldResetCachedMetadata() );
    assertTrue( Main.processOptions( environment, "--reset-cached-metadata", "generate" ) );
    assertTrue( environment.shouldResetCachedMetadata() );
  }

  @Test
  public void loadConfigFile()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:zeapp:2.0\n" );

    final Environment environment = newEnvironment();
    final Path file = getDefaultConfigFile();
    environment.setConfigFile( file );
    final ApplicationConfig config = Main.loadConfigFile( environment );
    assertEquals( config.getConfigLocation(), file );
    final List<ArtifactConfig> artifacts = config.getArtifacts();
    assertNotNull( artifacts );
    assertEquals( artifacts.size(), 1 );
  }

  @Test
  public void loadConfigFile_error()
    throws Exception
  {
    writeWorkspace();

    final Environment environment = newEnvironment();
    final Path file = getDefaultConfigFile();
    environment.setConfigFile( file );
    final TerminalStateException exception =
      expectThrows( TerminalStateException.class, () -> Main.loadConfigFile( environment ) );
    assertEquals( exception.getMessage(), "Error: Failed to load config file " + file );
    assertEquals( exception.getExitCode(), ExitCodes.ERROR_LOADING_CONFIG_CODE );
  }

  @Test
  public void loadModel()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:zeapp:2.0\n" );

    final Environment environment = newEnvironment();

    final Path file = getDefaultConfigFile();
    environment.setConfigFile( file );

    final ApplicationModel model = Main.loadModel( environment );
    assertEquals( model.getConfigLocation(), file );
    assertFalse( model.shouldResetCachedMetadata() );
    final List<ArtifactModel> artifacts = model.getArtifacts();
    assertNotNull( artifacts );
    assertEquals( artifacts.size(), 1 );
  }

  @Test
  public void loadModel_resetCachedMetadata()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:zeapp:2.0\n" );

    final Environment environment = newEnvironment();
    environment.markResetCachedMetadata();

    final ApplicationModel model = Main.loadModel( environment );
    assertTrue( model.shouldResetCachedMetadata() );
    final List<ArtifactModel> artifacts = model.getArtifacts();
    assertNotNull( artifacts );
    assertEquals( artifacts.size(), 1 );
  }

  @Test
  public void loadRecord_noDependencies()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir, "" );

    final ApplicationRecord record = Main.loadRecord( newEnvironment() );
    assertNonSystemArtifactCount( record, 0 );
    assertEquals( record.getAuthenticationContexts().size(), 0 );
  }

  @Test
  public void loadRecord_singleDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = Main.loadRecord( newEnvironment() );
    assertNonSystemArtifactCount( record, 1 );
  }

  @Test
  public void loadRecord_invalidSettings()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir, "" );

    final Environment environment = newEnvironment();

    final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
    environment.setSettingsFile( settingsFile );

    FileUtil.write( settingsFile.toString(), "JHSGDJHDS()*&(&Y*&" );

    final TerminalStateException exception =
      expectThrows( TerminalStateException.class, () -> Main.loadRecord( environment ) );
    assertEquals( exception.getMessage(), "Error: Problem loading settings from " + settingsFile );
    assertEquals( exception.getExitCode(), ExitCodes.ERROR_LOADING_SETTINGS_CODE );
  }

  @Test
  public void loadRecord_validSettings()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final Environment environment = newEnvironment();

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

    final ApplicationRecord record = Main.loadRecord( environment );
    assertEquals( record.getAuthenticationContexts().size(), 1 );
  }

  @Test
  public void loadRecord_ensureInvalidCacheMessagePropagated()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );

    final Path cacheDir = FileUtil.createLocalTempDir();
    environment.setCacheDir( cacheDir );

    final Path metadataCache = cacheDir.resolve( "com/example/myapp/1.0" ).resolve( DepgenMetadata.FILENAME );
    Files.createDirectories( metadataCache.getParent() );
    Files.write( metadataCache, ( "<default>.local.url=badUrl\n" ).getBytes( StandardCharsets.ISO_8859_1 ) );

    Main.loadRecord( environment );
    assertOutputContains( handler.toString(),
                          "Cache entry '<default>.local.url' for artifact 'com.example:myapp:jar:1.0' contains a url 'badUrl' that does not match the repository url '" );
  }

  @Test
  public void loadRecord_circularDependencies()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:myapp:1.0" );

    final TestHandler handler = new TestHandler();
    final TerminalStateException exception =
      expectThrows( TerminalStateException.class,
                    () -> Main.loadRecord( newEnvironment( handler ) ) );
    assertNull( exception.getMessage() );
    assertEquals( exception.getExitCode(), ExitCodes.ERROR_CYCLES_PRESENT_CODE );
    assertOutputContains( handler.toString(),
                          "1 dependency cycles detected when collecting dependencies:\n" +
                          "com.example:myapp:jar -> com.example:mylib:jar -> com.example:myapp:jar" );
  }

  @Test
  public void loadRecord_errorResolving()
    throws Exception
  {
    //Make the cache directory un-writeable and thus error saving pom
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );

    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );

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
  public void cacheRepositoryFile()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

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
  }

  @Test
  public void cacheRepositoryFile_alreadyExists()
    throws Exception
  {
    // Writing cacheContent into cache that differs from actual content so we can tell if it has been updated
    writeWorkspace();
    writeConfigFile( "" );

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
  }

  @Test
  public void cacheRepositoryFile_FailedToWrite()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

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
  }

  @Test
  public void cacheArtifactsInRepositoryCache()
    throws Exception
  {
    final Path repositoryCacheDir = FileUtil.createLocalTempDir();
    writeBazelrc( repositoryCacheDir );
    FileUtil.write( "WORKSPACE", "" );
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
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

    final Environment environment = newEnvironment( handler );
    environment.setRepositoryCacheDir( repositoryCacheDir );
    Main.cacheArtifactsInRepositoryCache( environment, record );
    assertOutputContains( handler.toString(),
                          "Installed artifact 'com.example:myapp:jar:1.0' into repository cache.\n" +
                          "Installed artifact 'com.example:myapp:jar:sources:1.0' into repository cache." );

    assertTrue( Files.exists( targetFile ) );
    assertTrue( Files.exists( sourceTargetFile ) );
  }

  @Test
  public void cacheArtifactsInRepositoryCache_minusSourcesClassifier()
    throws Exception
  {
    final Path repositoryCacheDir = FileUtil.createLocalTempDir();
    writeBazelrc( repositoryCacheDir );
    FileUtil.write( "WORKSPACE", "" );
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
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

    final Environment environment = newEnvironment( handler );
    environment.setRepositoryCacheDir( repositoryCacheDir );
    Main.cacheArtifactsInRepositoryCache( environment, record );
    assertOutputContains( handler.toString(), "Installed artifact 'com.example:myapp:jar:1.0' into repository cache." );

    assertTrue( Files.exists( targetFile ) );
  }

  @Test
  public void cacheArtifactsInRepositoryCache_multipleArtifacts()
    throws Exception
  {
    final Path repositoryCacheDir = FileUtil.createLocalTempDir();
    writeBazelrc( repositoryCacheDir );
    FileUtil.write( "WORKSPACE", "" );
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
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

    final Environment environment = newEnvironment( handler );
    environment.setRepositoryCacheDir( repositoryCacheDir );
    Main.cacheArtifactsInRepositoryCache( environment, record );
    assertEquals( handler.toString(),
                  "Installed artifact 'com.example:myapp:jar:1.0' into repository cache.\n" +
                  "Installed artifact 'com.example:myapp:jar:sources:1.0' into repository cache.\n" +
                  "Installed artifact 'com.example:mylib:jar:1.0' into repository cache." );

    assertTrue( Files.exists( targetFile1 ) );
    assertTrue( Files.exists( targetFile2 ) );
  }

  @Test
  public void run_validDependencySpec()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:jar:1.0\n" +
                     "    excludes: ['org.realityforge.javax.annotation:javax.annotation']\n" );
    final String output = runCommand( "generate" );
    assertEquals( output, "" );
  }

  @Test
  public void run_hash()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    excludes: ['org.realityforge.javax.annotation:javax.annotation']\n" );
    final String output = runCommand( "hash" );
    assertEquals( output, "Content SHA256: 2DDCEE0CE8D16EE57C89A175877115495555796D3C1598EB32DC7652CA37204A" );
  }

  @Test
  public void run_invalidDependencySpec()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.gir\n" );

    final String output = runCommand( ExitCodes.ERROR_CONSTRUCTING_MODEL_CODE, "generate" );
    assertOutputContains( output,
                          "The 'coord' property on the dependency must specify 2-5 components separated by the ':' character. The 'coords' must be in one of the forms; 'group:id', 'group:id:version', 'group:id:type:version' or 'group:id:type:classifier:version'.\n" +
                          "--- Invalid Config ---\n" +
                          "coord: org.realityforge.gir\n" +
                          "--- End Config ---" );
  }

  @Test
  public void run_invalidYaml()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts: 's\n" +
                     "  - group: org.realityforge.gir\n" );

    final String output = runCommand( ExitCodes.ERROR_LOADING_CONFIG_CODE, "generate" );
    assertOutputContains( output, "Error: Failed to load config file " );
    assertOutputContains( output, "Cause: while scanning a quoted scalar" );
    assertOutputDoesNotContain( output, "\tat org.yaml.snakeyaml.Yaml.load(" );
  }

  @Test
  public void run_invalidYamlInVerboseMode()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts: 's\n" +
                     "  - group: org.realityforge.gir\n" );

    final String output = runCommand( ExitCodes.ERROR_LOADING_CONFIG_CODE, "--verbose", "generate" );
    assertOutputContains( output, "Error: Failed to load config file " );
    assertOutputContains( output, "Cause: while scanning a quoted scalar" );
    assertOutputContains( output, "found unexpected end of stream" );
  }

  @Test
  public void run_invalidConfig()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:jar:1.0\n" +
                     "    repositories:\n" +
                     "      - NotExist\n" );

    final String output = runCommand( ExitCodes.ERROR_CONFIG_VALIDATION_CODE, "--verbose", "generate" );
    assertEquals( output,
                  "Artifact 'com.example:myapp' declared a repository named 'NotExist' but no such repository is declared in the repository section. Known repositories include: local" );
  }

  @Test
  public void run_badArg()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final String output = runCommand( ExitCodes.ERROR_PARSING_ARGS_EXIT_CODE, "Bleep" );
    assertEquals( output, "Error: Unknown command: Bleep" );
  }

  @Test
  public void run_noOutputInNormalCase()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( FileUtil.createLocalTempDir(), "" );

    final String output = runCommand( "generate" );
    assertEquals( output, "" );
  }

  @Test
  public void run_printGraph()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final String output = runCommand( "print-graph" );
    assertEquals( output,
                  "Dependency Graph:\n" +
                  "\\- com.example:myapp:jar:1.0 [compile]" );
  }

  @Test
  public void run_missingDefaultSettingsIsFine()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( FileUtil.createLocalTempDir(), "" );

    runCommand( "generate" );
  }

  @Test
  public void run_validDefaultSettings()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( FileUtil.createLocalTempDir(), "" );

    assertTrue( FileUtil.getCurrentDirectory().resolve( ".m2" ).toFile().mkdir() );
    FileUtil.write( ".m2/settings.xml",
                    "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                    "  <servers>\n" +
                    "    <server>\n" +
                    "      <id>my-repo</id>\n" +
                    "      <username>root</username>\n" +
                    "      <password>secret</password>\n" +
                    "    </server>\n" +
                    "  </servers>\n" +
                    "</settings>\n" );

    final String output = runCommand( "generate" );
    assertOutputContains( output, "" );
  }

  @Test
  public void run_validSpecifiedSettings()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( FileUtil.createLocalTempDir(), "" );

    FileUtil.write( "some_settings.xml",
                    "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                    "  <servers>\n" +
                    "    <server>\n" +
                    "      <id>my-repo</id>\n" +
                    "      <username>root</username>\n" +
                    "      <password>secret</password>\n" +
                    "    </server>\n" +
                    "  </servers>\n" +
                    "</settings>\n" );

    final String output = runCommand( "--settings-file", "some_settings.xml", "generate" );
    assertOutputContains( output, "" );
  }

  @Nonnull
  private String runCommand( @Nonnull final String... args )
    throws Exception
  {
    return runCommand( ExitCodes.SUCCESS_EXIT_CODE, args );
  }

  @Nonnull
  private String runCommand( final int expectedExitCode, @Nonnull final String... args )
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.logger().setLevel( Level.INFO );
    final int exitCode = Main.run( environment, args );
    assertEquals( expectedExitCode, exitCode );
    return handler.toString();
  }

  @Nonnull
  private String failToProcessOptions( @Nonnull final String... args )
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    assertFalse( Main.processOptions( environment, args ) );
    return handler.toString();
  }
}
