package org.realityforge.bazel.depgen;

import gir.Gir;
import gir.Task;
import gir.io.Exec;
import gir.io.FileUtil;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
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
    return ResolverUtil.createResolver( createLogger(),
                                        cacheDir,
                                        model,
                                        SettingsUtil.loadSettings( settingsFile, createLogger() ) );
  }

  @Nonnull
  final Resolver createResolver( @Nonnull final Path localRepositoryDirectory )
    throws Exception
  {
    final RemoteRepository remoteRepository =
      new RemoteRepository.Builder( "local", "default", localRepositoryDirectory.toUri().toString() ).build();
    return ResolverUtil.createResolver( createLogger(),
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
    final ApplicationConfig applicationConfig =
      ApplicationConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );

    return ApplicationModel.parse( applicationConfig );
  }

  @Nonnull
  final String runCommand( @Nonnull final String... additionalArgs )
  {
    return runCommand( 0, additionalArgs );
  }

  @Nonnull
  final String runCommand( final int expectedExitCode, @Nonnull final String... additionalArgs )
  {
    final ArrayList<String> args = new ArrayList<>();
    args.add( "java" );
    args.add( "-Duser.home=" + FileUtil.getCurrentDirectory() );
    args.add( "-jar" );
    args.add( getApplicationJar().toString() );
    Collections.addAll( args, additionalArgs );
    return Exec.capture( b -> Exec.cmd( b, args.toArray( new String[ 0 ] ) ), expectedExitCode );
  }

  protected final void inIsolatedDirectory( @Nonnull final Task java )
    throws Exception
  {
    Gir.go( () -> FileUtil.inTempDir( java ) );
  }

  @Nonnull
  private Path getApplicationJar()
  {
    return Paths.get( System.getProperty( "depgen.jar" ) ).toAbsolutePath().normalize();
  }

  final void writeWorkspace()
    throws IOException
  {
    FileUtil.write( "WORKSPACE", "" );
  }

  protected final void writeDependencies( @Nonnull final Path dir, @Nonnull final String content )
    throws IOException
  {
    writeDependencies( "repositories:\n  local: " + dir.toUri() + "\n" + content );
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

  @SuppressWarnings( "StringConcatenationInLoop" )
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

    if ( 0 != dependencies.length )
    {
      pomContents += "  <dependencies>\n";
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
    }

    pomContents += "</project>\n";
    Files.write( pomFile, pomContents.getBytes() );
    return pomFile;
  }

  protected final void deployTempArtifactToLocalRepository( @Nonnull final Path localRepository,
                                                            @Nonnull final String coords,
                                                            @Nonnull final String... dependencies )
    throws Exception
  {
    final Resolver resolver = createResolver( localRepository );

    final Artifact jarArtifact = new DefaultArtifact( coords ).setFile( createTempJarFile().toFile() );
    final Artifact pomArtifact =
      new SubArtifact( jarArtifact, "", "pom" )
        .setFile( createTempPomFile( jarArtifact.getGroupId(),
                                     jarArtifact.getArtifactId(),
                                     jarArtifact.getVersion(),
                                     jarArtifact.getExtension(),
                                     dependencies ).toFile() );

    final DeployRequest request =
      new DeployRequest()
        .addArtifact( jarArtifact )
        .addArtifact( pomArtifact )
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
  private Path createTempJarFile()
    throws IOException
  {
    final Path jarFile = Files.createTempFile( "data", ".jar" );
    final JarOutputStream outputStream = new JarOutputStream( new FileOutputStream( jarFile.toFile() ) );
    final JarEntry entry = new JarEntry( "data.txt" );
    entry.setCreationTime( FileTime.fromMillis( 0 ) );
    entry.setTime( 0 );
    entry.setComment( null );
    outputStream.putNextEntry( entry );
    outputStream.write( "Hi".getBytes() );
    outputStream.closeEntry();
    outputStream.close();
    return jarFile;
  }

  @Nonnull
  final Logger createLogger()
  {
    // Don't capture the logs so we get some idea when issues occur
    return Logger.getAnonymousLogger();
  }

  @Nonnull
  final Logger createLogger( @Nonnull final TestHandler handler )
  {
    final Logger logger = Logger.getAnonymousLogger();
    logger.setUseParentHandlers( false );
    logger.addHandler( handler );
    return logger;
  }
}
