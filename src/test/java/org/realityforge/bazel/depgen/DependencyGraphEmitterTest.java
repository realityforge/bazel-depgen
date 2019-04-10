package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.eclipse.aether.resolution.DependencyResult;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
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
      final Path dir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtB:jar::2.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtB:2.0" );

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger(), dir, Collections.emptyList(), true, true );

      writeDependencies( "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      final String output = collectOutput( resolver );
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
      final Path dir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::32.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:32.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );

      final Resolver resolver =
        ResolverUtil.createResolver( createLogger(), dir, Collections.emptyList(), true, true );

      writeDependencies( "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      final String output = collectOutput( resolver );
      assertEquals( output,
                    "\\- com.example:myapp:jar:1.0 [compile]\n" +
                    "   +- com.example:mylib:jar:1.0 [compile]\n" +
                    "   |  \\- com.example:rtA:jar:32.0 [runtime] (conflicts with 33.0)\n" +
                    "   \\- com.example:rtA:jar:33.0 [runtime]\n" );
    } );
  }

  @Nonnull
  private String collectOutput( @Nonnull final Resolver resolver )
    throws Exception
  {
    final ApplicationConfig applicationConfig =
      ApplicationConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );

    final ApplicationModel model = ApplicationModel.parse( applicationConfig );

    final AtomicBoolean hasFailed = new AtomicBoolean( false );

    final DependencyResult result = resolver.resolveDependencies( model, ( m, e ) -> hasFailed.set( true ) );

    assertFalse( hasFailed.get() );

    assertTrue( result.getCycles().isEmpty() );
    assertTrue( result.getCollectExceptions().isEmpty() );
    assertNotNull( result.getRoot() );

    final StringBuilder sb = new StringBuilder();
    result.getRoot().accept( new DependencyGraphEmitter( line -> {
      sb.append( line );
      sb.append( "\n" );
    } ) );

    return sb.toString();
  }
}
