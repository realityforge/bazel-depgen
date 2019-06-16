package org.realityforge.bazel.depgen;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class HashCommandTest
  extends AbstractTest
{
  @Test
  public void hash()
    throws Exception
  {
    writeWorkspace();
    writeDependencies( "artifacts:\n" +
                       "  - coord: com.example:myapp:1.0\n" );

    final TestHandler handler = new TestHandler();
    final HashCommand command = new HashCommand();
    final int exitCode = command.run( new CommandContextImpl( newEnvironment( createLogger( handler ) ) ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    assertEquals( handler.toString(),
                  "Content SHA256: 68814747A184F6E9A415AC0B97061A8ED1A79E487364555F3BAE5E0B0785DA39" );
  }
}
