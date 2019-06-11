package org.realityforge.bazel.depgen;

import gir.Gir;
import gir.Task;
import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.testng.Assert;
import static org.testng.Assert.*;

public abstract class AbstractTest
{
  @Nonnull
  protected final ApplicationRecord loadApplicationRecord()
    throws Exception
  {
    return loadApplicationRecord( FileUtil.createLocalTempDir() );
  }

  @Nonnull
  protected final ApplicationRecord loadApplicationRecord( @Nonnull final Path cacheDir )
    throws Exception
  {
    final ApplicationModel model = loadApplicationModel();
    final Resolver resolver = createResolver( model, cacheDir );
    final DependencyNode root = resolveDependencies( resolver, model );
    return ApplicationRecord.build( model,
                                    root,
                                    resolver.getAuthenticationContexts(),
                                    Assert::fail );
  }

  @Nonnull
  private Resolver createResolver( @Nonnull final ApplicationModel model,
                                   @Nonnull final Path cacheDir )
    throws Exception
  {
    final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
    return ResolverUtil.createResolver( newEnvironment(),
                                        cacheDir,
                                        model,
                                        SettingsUtil.loadSettings( settingsFile, Logger.getAnonymousLogger() ) );
  }

  final Environment newEnvironment()
  {
    return newEnvironment( Logger.getAnonymousLogger() );
  }

  final Environment newEnvironment( @Nonnull final Logger logger )
  {
    return new Environment( null, FileUtil.getCurrentDirectory(), logger );
  }

  @Nonnull
  final Resolver createResolver( @Nonnull final Path localRepositoryDirectory )
    throws Exception
  {
    final RemoteRepository remoteRepository =
      new RemoteRepository.Builder( "local", "default", localRepositoryDirectory.toUri().toString() ).build();
    return ResolverUtil.createResolver( newEnvironment(),
                                        FileUtil.createLocalTempDir(),
                                        Collections.singletonList( remoteRepository ),
                                        true,
                                        true );
  }

  @Nonnull
  final DependencyNode resolveDependencies( @Nonnull final Resolver resolver,
                                            @Nonnull final ApplicationModel model )
    throws DependencyResolutionException
  {
    final DependencyResult result = resolver.resolveDependencies( model, ( m, e ) -> fail() );

    assertTrue( result.getCycles().isEmpty() );
    assertTrue( result.getCollectExceptions().isEmpty() );
    final DependencyNode root = result.getRoot();
    assertNotNull( root );
    return root;
  }

  @Nonnull
  protected final ApplicationModel loadApplicationModel()
    throws Exception
  {
    return ApplicationModel.parse( loadApplicationConfig(), false );
  }

  @Nonnull
  protected final ApplicationConfig loadApplicationConfig()
    throws Exception
  {
    return ApplicationConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
  }

  protected final void inIsolatedDirectory( @Nonnull final Task task )
    throws Exception
  {
    Gir.go( () -> FileUtil.inTempDir( task ) );
  }

  private void writeBazelrc()
    throws IOException
  {
    writeBazelrc( FileUtil.createLocalTempDir() );
  }

  protected final void writeBazelrc( @Nonnull final Path repositoryCache )
    throws IOException
  {
    FileUtil.write( ".bazelrc", "build --repository_cache " + repositoryCache + "\n" );
  }

  final void writeWorkspace()
    throws IOException
  {
    writeBazelrc();
    FileUtil.write( "WORKSPACE", "" );
  }

  protected final void writeDependencies( @Nonnull final Path dir, @Nonnull final String content )
    throws IOException
  {
    writeDependencies( "repositories:\n" +
                       "  - name: local\n" +
                       "    url: " + dir.toUri() + "\n" +
                       content );
  }

  protected final void writeDependencies( @Nonnull final String content )
    throws IOException
  {
    FileUtil.write( "dependencies.yml", content );
  }

  final void assertOutputContains( @Nonnull final String output, @Nonnull final String text )
  {
    assertTrue( output.contains( text ),
                "Expected output\n---\n" + output + "\n---\nto contain text\n---\n" + text + "\n---\n" );
  }

  final void assertOutputDoesNotContain( @Nonnull final String output, @Nonnull final String text )
  {
    assertFalse( output.contains( text ),
                 "Expected output\n---\n" + output + "\n---\nto not contain text\n---\n" + text + "\n---\n" );
  }

  @Nonnull
  private Path createTempPomFile( @Nonnull final String group,
                                  @Nonnull final String id,
                                  @Nonnull final String version,
                                  @Nonnull final String type,
                                  @Nonnull final String... dependencies )
    throws IOException
  {
    final Path pomFile = Files.createTempFile( "data", ".pom" );
    String pomContents =
      "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
      "  <modelVersion>4.0.0</modelVersion>\n" +
      "  <groupId>" + group + "</groupId>\n" +
      "  <artifactId>" + id + "</artifactId>\n" +
      "  <version>" + version + "</version>\n" +
      "  <packaging>" + type + "</packaging>\n";

    pomContents += buildDependenciesSection( dependencies );

    pomContents += "</project>\n";
    Files.write( pomFile, pomContents.getBytes() );
    return pomFile;
  }

  @SuppressWarnings( "StringConcatenationInLoop" )
  @Nonnull
  private String buildDependenciesSection( @Nonnull final String[] dependencies )
    throws IOException
  {
    if ( 0 != dependencies.length )
    {
      String pomContents = "  <dependencies>\n";
      for ( final String dependency : dependencies )
      {
        final String[] components = dependency.split( ":" );
        assert components.length >= 3 && components.length <= 7;
        final String dependencyGroup = components[ 0 ];
        final String dependencyId = components[ 1 ];
        final String dependencyType = components.length > 3 ? components[ 2 ] : null;
        final String dependencyClassifier = components.length > 4 ? components[ 3 ] : null;
        final String dependencyVersion =
          components.length == 3 ? components[ 2 ] : components.length == 4 ? components[ 3 ] : components[ 4 ];
        final String dependencyScope = components.length == 6 ? components[ 5 ] : null;
        final boolean optional = components.length == 7 && "optional".equals( components[ 6 ] );

        pomContents += "    <dependency>\n";
        pomContents += "      <groupId>" + dependencyGroup + "</groupId>\n";
        pomContents += "      <artifactId>" + dependencyId + "</artifactId>\n";
        pomContents += "      <version>" + dependencyVersion + "</version>\n";
        if ( null != dependencyType )
        {
          pomContents += "      <type>" + dependencyType + "</type>\n";
        }
        if ( null != dependencyClassifier && !"".equals( dependencyClassifier ) )
        {
          pomContents += "      <classifier>" + dependencyClassifier + "</classifier>\n";
        }
        if ( null != dependencyScope )
        {
          pomContents += "      <scope>" + dependencyScope + "</scope>\n";
          if ( org.apache.maven.artifact.Artifact.SCOPE_SYSTEM.equals( dependencyScope ) )
          {
            pomContents += "      <systemPath>" + createTempJarFile() + "</systemPath>\n";
          }
        }
        if ( optional )
        {
          pomContents += "      <optional>true</optional>\n";
        }
        pomContents += "    </dependency>\n";
      }
      pomContents += "  </dependencies>\n";
      return pomContents;
    }
    else
    {
      return "";
    }
  }

  protected final void deployArtifactToLocalRepository( @Nonnull final Path localRepository,
                                                        @Nonnull final String coords,
                                                        @Nonnull final String... dependencies )
    throws Exception
  {
    final SubArtifact sourcesArtifact = new SubArtifact( new DefaultArtifact( coords ), "sources", "jar" );
    deployTempArtifactToLocalRepository( localRepository, sourcesArtifact.toString() );
    deployTempArtifactToLocalRepository( localRepository, coords, dependencies );
  }

  protected final void deployTempArtifactToLocalRepository( @Nonnull final Path localRepository,
                                                            @Nonnull final String coords,
                                                            @Nonnull final String... dependencies )
    throws Exception
  {
    deployTempArtifactToLocalRepository( localRepository, coords, createTempJarFile(), dependencies );
  }

  protected final void deployTempArtifactToLocalRepository( @Nonnull final Path localRepository,
                                                            @Nonnull final String coords,
                                                            @Nonnull final Path file,
                                                            @Nonnull final String... dependencies )
    throws Exception
  {
    final Artifact artifact = new DefaultArtifact( coords );
    final Path pomFile =
      createTempPomFile( artifact.getGroupId(),
                         artifact.getArtifactId(),
                         artifact.getVersion(),
                         artifact.getExtension(),
                         dependencies );
    deployTempArtifactToLocalRepository( localRepository, coords, file, pomFile );
  }

  private void deployTempArtifactToLocalRepository( @Nonnull final Path localRepository,
                                                    @Nonnull final String coords,
                                                    @Nonnull final Path file,
                                                    @Nonnull final Path pomFile )
    throws Exception
  {
    final Artifact pomArtifact =
      new SubArtifact( new DefaultArtifact( coords ), "", "pom" );

    final Resolver resolver = createResolver( localRepository );

    final DeployRequest request =
      new DeployRequest()
        .addArtifact( new DefaultArtifact( coords ).setFile( file.toFile() ) )
        .addArtifact( pomArtifact.setFile( pomFile.toFile() ) )
        .setRepository( new RemoteRepository.Builder( "local",
                                                      "default",
                                                      localRepository.toUri().toString() ).build() );
    resolver.getSystem().deploy( resolver.getSession(), request );
  }

  @Nonnull
  protected final String loadPropertiesContent( @Nonnull final Path file )
    throws IOException
  {
    return new String( Files.readAllBytes( file ), StandardCharsets.ISO_8859_1 ).replaceAll( "^#[^\n]*\n", "" );
  }

  @Nonnull
  protected final Path createTempJarFile()
    throws IOException
  {
    return createJarFile( "data.txt", "Hi" );
  }

  @Nonnull
  protected final Path createJarFile( @Nonnull final String filename, @Nonnull final String contents )
    throws IOException
  {
    final Path jarFile = Files.createTempFile( FileUtil.getCurrentDirectory(), "data", ".jar" );
    final JarOutputStream outputStream = new JarOutputStream( new FileOutputStream( jarFile.toFile() ) );
    final JarEntry entry = new JarEntry( filename );
    entry.setCreationTime( FileTime.fromMillis( 0 ) );
    entry.setTime( 0 );
    entry.setComment( null );
    outputStream.putNextEntry( entry );
    outputStream.write( contents.getBytes( StandardCharsets.UTF_8 ) );
    outputStream.closeEntry();
    outputStream.close();
    return jarFile;
  }

  @Nonnull
  final Logger createLogger( @Nonnull final TestHandler handler )
  {
    final Logger logger = Logger.getAnonymousLogger();
    logger.setUseParentHandlers( false );
    logger.addHandler( handler );
    logger.setLevel( Level.ALL );
    return logger;
  }

  @Nonnull
  protected final String asString( @Nonnull final ByteArrayOutputStream outputStream )
  {
    return new String( outputStream.toByteArray(), StandardCharsets.US_ASCII );
  }
}
