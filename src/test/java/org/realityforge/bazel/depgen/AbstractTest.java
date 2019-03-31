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
        assert components.length >= 3 && components.length <= 6;
        final String dependencyGroup = components[ 0 ];
        final String dependencyId = components[ 1 ];
        final String dependencyType = components.length > 3 ? components[ 2 ] : null;
        final String dependencyClassifier = components.length > 4 ? components[ 3 ] : null;
        final String dependencyVersion =
          components.length == 3 ? components[ 2 ] : components.length == 4 ? components[ 3 ] : components[ 4 ];
        final String dependencyScope = components.length == 6 ? components[ 5 ] : null;

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
        }
        pomContents += "    </dependency>\n";
      }
      pomContents += "  </dependencies>\n";
    }

    pomContents += "</project>\n";
    Files.write( pomFile, pomContents.getBytes() );
    return pomFile;
  }

  final void deployTempArtifactToLocalRepository( @Nonnull final Path localRepository,
                                                  @Nonnull final String coords,
                                                  @Nonnull final String... dependencies )
    throws IOException, DeploymentException
  {
    final Resolver resolver =
      ResolverUtil.createResolver( createLogger(), localRepository, Collections.emptyList(), true, true );

    final Artifact jarArtifact = new DefaultArtifact( coords ).setFile( createTempJarFile().toFile() );
    final Artifact pomArtifact =
      new SubArtifact( jarArtifact, "", "pom" )
        .setFile( createTempPomFile( jarArtifact.getGroupId(),
                                     jarArtifact.getArtifactId(),
                                     jarArtifact.getVersion(),
                                     jarArtifact.getExtension(),
                                     dependencies ).toFile() );

    final DeployRequest request =
      new DeployRequest().addArtifact( jarArtifact )
        .addArtifact( pomArtifact )
        .setRepository( new RemoteRepository.Builder( "local",
                                                      "default",
                                                      localRepository.toUri().toString() ).build() );
    resolver.getSystem().deploy( resolver.getSession(), request );
  }

  @Nonnull
  private Path createTempJarFile()
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
