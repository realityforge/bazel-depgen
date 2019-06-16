package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.logging.Level;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PrintGraphCommandTest
  extends AbstractTest
{
  @Test
  public void run_printGraph()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      final TestHandler handler = new TestHandler();
      handler.setLevel( Level.INFO );
      final PrintGraphCommand command = new PrintGraphCommand();
      final int exitCode = command.run( new CommandContextImpl( newEnvironment( createLogger( handler ) ) ) );
      assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
      assertEquals( handler.toString(),
                    "Dependency Graph:\n" +
                    "\\- com.example:myapp:jar:1.0 [compile]" );
    } );
  }
}
