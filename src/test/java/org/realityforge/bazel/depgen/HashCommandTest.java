package org.realityforge.bazel.depgen;

import java.util.logging.Level;
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
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final TestHandler handler = new TestHandler();
    final HashCommand command = new HashCommand();
    final int exitCode = command.run( new CommandContextImpl( newEnvironment( handler ) ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    assertEquals( handler.toString(),
                  "Content SHA256: 0A8DBED4B09238126BA5E065EB4E392A1B631FA1A20FCA9AE1DF5AA364F59C96" );
  }

  @Test
  public void hash_verify_success()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final TestHandler handler = new TestHandler();
    final HashCommand command = new HashCommand();
    final Environment environment = newEnvironment( handler );
    final boolean parsed =
      command.processOptions( environment,
                              "--verify-sha256",
                              "0A8DBED4B09238126BA5E065EB4E392A1B631FA1A20FCA9AE1DF5AA364F59C96" );
    assertTrue( parsed );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    assertEquals( handler.toString(),
                  "Content SHA256: 0A8DBED4B09238126BA5E065EB4E392A1B631FA1A20FCA9AE1DF5AA364F59C96" );
  }

  @Test
  public void hash_verify_success_quiet()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final TestHandler handler = new TestHandler();
    final HashCommand command = new HashCommand();
    final Environment environment = newEnvironment( handler );
    environment.logger().setLevel( Level.WARNING );
    final boolean parsed =
      command.processOptions( environment,
                              "--verify-sha256",
                              "0A8DBED4B09238126BA5E065EB4E392A1B631FA1A20FCA9AE1DF5AA364F59C96" );
    assertTrue( parsed );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    assertEquals( handler.toString(), "" );
  }

  @Test
  public void hash_verify_failure()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final TestHandler handler = new TestHandler();
    final HashCommand command = new HashCommand();
    final Environment environment = newEnvironment( handler );
    final boolean parsed = command.processOptions( environment, "--verify-sha256", "XXXX" );
    assertTrue( parsed );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.ERROR_BAD_SHA256_CONFIG_CODE );
    assertEquals( handler.toString(),
                  "Content SHA256: 0A8DBED4B09238126BA5E065EB4E392A1B631FA1A20FCA9AE1DF5AA364F59C96 (Expected XXXX)" );
  }

  @Test
  public void hash_badArgs()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final TestHandler handler = new TestHandler();
    final HashCommand command = new HashCommand();
    final Environment environment = newEnvironment( handler );
    final boolean parsed = command.processOptions( environment, "--something-something" );
    assertFalse( parsed );
    assertEquals( handler.toString(), "Error: Unknown option --something-something" );
  }

  @Test
  public void hash_unknownArgs()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final TestHandler handler = new TestHandler();
    final HashCommand command = new HashCommand();
    final Environment environment = newEnvironment( handler );
    final boolean parsed = command.processOptions( environment, "blah" );
    assertFalse( parsed );
    assertEquals( handler.toString(), "Error: Invalid argument: blah" );
  }
}
