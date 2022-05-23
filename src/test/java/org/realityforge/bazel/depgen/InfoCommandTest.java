package org.realityforge.bazel.depgen;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class InfoCommandTest
  extends AbstractTest
{
  @Test
  public void info()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Command command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "config-file=" + environment.getConfigFile() + "\n" );
    assertOutputContains( output, "settings-file=" + environment.getSettingsFile() + "\n" );
    assertOutputContains( output, "cache-directory=" + environment.getCacheDir() + "\n" );
    assertOutputContains( output, "reset-cached-metadata=false\n" );
    assertOutputContains( output, "bazel-repository-cache=" );
  }

  @Test
  public void info_single_value()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Command command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    command.processOptions( environment, "config-file" );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertEquals( output, "config-file=" + environment.getConfigFile() );
  }

  @Test
  public void info_multiple_values()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Command command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    command.processOptions( environment, "config-file", "settings-file" );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertEquals( output, "config-file=" + environment.getConfigFile() + "\n" +
                          "settings-file=" + environment.getSettingsFile() );
  }

  @Test
  public void info_zero_values()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final Command command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    command.processOptions( environment, "XXXXX" );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertEquals( output, "" );
  }

  @Test
  public void info_badArgs()
    throws Exception
  {
    writeWorkspace();
    writeConfigFile( "" );

    final TestHandler handler = new TestHandler();
    final InfoCommand command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    final boolean parsed = command.processOptions( environment, "--something-something" );
    assertFalse( parsed );
    assertEquals( handler.toString(), "Error: Unknown option --something-something" );
  }
}
