package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.eclipse.aether.graph.DependencyNode;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DependencyGraphEmitterTest
  extends AbstractTest
{
  @Test
  public void emitBasicGraph()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtB:jar::2.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtB:2.0" );

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      final String output = collectOutput( createResolver( dir ) );
      assertEquals( output,
                    "\\- com.example:myapp:jar:1.0 [compile]\n" +
                    "   +- com.example:mylib:jar:1.0 [compile]\n" +
                    "   |  \\- com.example:rtB:jar:2.0 [runtime]\n" +
                    "   \\- com.example:rtA:jar:33.0 [runtime]\n" );
    } );
  }

  @Test
  public void emitGraphWithConflicts()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::32.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:32.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      final String output = collectOutput( createResolver( dir ) );
      assertEquals( output,
                    "\\- com.example:myapp:jar:1.0 [compile]\n" +
                    "   +- com.example:mylib:jar:1.0 [compile]\n" +
                    "   |  \\- com.example:rtA:jar:32.0 [runtime] (conflicts with 33.0)\n" +
                    "   \\- com.example:rtA:jar:33.0 [runtime]\n" );
    } );
  }

  @Test
  public void emitGraphWithReplacement()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtB:jar::2.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtB:2.0" );

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "replacements:\n" +
                              "  - coord: com.example:rtA\n" +
                              "    target: //foo/rta" );
      final String output = collectOutput( createResolver( dir ) );
      assertEquals( output,
                    "\\- com.example:myapp:jar:1.0 [compile]\n" +
                    "   +- com.example:mylib:jar:1.0 [compile]\n" +
                    "   |  \\- com.example:rtB:jar:2.0 [runtime]\n" +
                    "   \\- com.example:rtA:jar:33.0 [runtime] REPLACED BY //foo/rta\n" );
    } );
  }

  @Test
  public void emitGraphWithExclude()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtB:jar::2.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtB:2.0" );

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "excludes:\n" +
                              "  - coord: com.example:rtB\n" );
      final String output = collectOutput( createResolver( dir ) );
      assertEquals( output,
                    "\\- com.example:myapp:jar:1.0 [compile]\n" +
                    "   +- com.example:mylib:jar:1.0 [compile]\n" +
                    "   \\- com.example:rtA:jar:33.0 [runtime]\n" );
    } );
  }
  @Nonnull
  private String collectOutput( @Nonnull final Resolver resolver )
    throws Exception
  {
    final ApplicationModel model = loadApplicationModel();
    final DependencyNode root = resolveDependencies( resolver, model );

    final StringBuilder sb = new StringBuilder();
    root.accept( new DependencyGraphEmitter( model, line -> {
      sb.append( line );
      sb.append( "\n" );
    } ) );

    return sb.toString();
  }
}
