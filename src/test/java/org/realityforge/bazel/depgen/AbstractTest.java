package org.realityforge.bazel.depgen;

import gir.Gir;
import gir.Task;
import gir.io.Exec;
import gir.io.FileUtil;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.SubArtifact;
import static org.testng.Assert.*;

public abstract class AbstractTest
{
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

  @Nonnull
  final Path createTempPomFile( @Nonnull final String group,
                                @Nonnull final String id,
                                @Nonnull final String version,
                                @Nonnull final String type )
    throws IOException
  {
    final Path pomFile = Files.createTempFile( "data", ".pom" );
    createPomFile( pomFile, group, id, version, type );
    return pomFile;
  }

  final void createPomFile( @Nonnull final Path pomFile,
                            @Nonnull final String group,
                            @Nonnull final String id,
                            @Nonnull final String version,
                            @Nonnull final String type )
    throws IOException
  {
    final String pomContents =
      "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
      "  <modelVersion>4.0.0</modelVersion>\n" +
      "  <groupId>" + group + "</groupId>\n" +
      "  <artifactId>" + id + "</artifactId>\n" +
      "  <version>" + version + "</version>\n" +
      "  <packaging>" + type + "</packaging>\n" +
      "</project>\n";

    Files.write( pomFile, pomContents.getBytes() );
  }

  final void deployTempArtifactToLocalRepository( @Nonnull final Path localRepository, @Nonnull final String coords )
    throws IOException, DeploymentException
  {
    final Resolver resolver =
      ResolverUtil.createResolver( createLogger(), localRepository, Collections.emptyList(), true, true );

    final DefaultArtifact artifact = new DefaultArtifact( coords );
    final Artifact jarArtifact = artifact.setFile( createTempJarFile().toFile() );
    final Artifact pomArtifact =
      new SubArtifact( jarArtifact, "", "pom" )
        .setFile( createTempPomFile( jarArtifact.getGroupId(),
                                     jarArtifact.getArtifactId(),
                                     jarArtifact.getVersion(),
                                     jarArtifact.getExtension() ).toFile() );

    final DeployRequest request =
      new DeployRequest().addArtifact( jarArtifact )
        .addArtifact( pomArtifact )
        .setRepository( new RemoteRepository.Builder( "local",
                                                      "default",
                                                      localRepository.toUri().toString() ).build() );
    resolver.getSystem().deploy( resolver.getSession(), request );
  }

  @Nonnull
  final Path createTempJarFile()
    throws IOException
  {
    final Path jarFile = Files.createTempFile( "data", ".jar" );
    final JarOutputStream outputStream = new JarOutputStream( new FileOutputStream( jarFile.toFile() ) );
    final JarEntry entry = new JarEntry( "data.txt" );
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
