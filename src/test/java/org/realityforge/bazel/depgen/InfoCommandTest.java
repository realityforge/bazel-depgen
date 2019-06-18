package org.realityforge.bazel.depgen;

import java.util.regex.Pattern;
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
    writeDependencies( "" );

    final TestHandler handler = new TestHandler();
    final Command command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "config-file=" + environment.getDependenciesFile() + "\n" );
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
    writeDependencies( "" );

    final TestHandler handler = new TestHandler();
    final Command command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    command.processOptions( environment, "config-file" );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertEquals( output, "config-file=" + environment.getDependenciesFile() );
  }

  @Test
  public void info_multiple_values()
    throws Exception
  {
    writeWorkspace();
    writeDependencies( "" );

    final TestHandler handler = new TestHandler();
    final Command command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    command.processOptions( environment, "config-file", "settings-file" );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertEquals( output, "config-file=" + environment.getDependenciesFile() + "\n" +
                          "settings-file=" + environment.getSettingsFile() );
  }

  @Test
  public void info_zero_values()
    throws Exception
  {
    writeWorkspace();
    writeDependencies( "" );

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
  public void info_deriveCacheDirInsideWorkspace()
    throws Exception
  {
    writeDependencies( "" );
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setCacheDir( null );
    final Command command = new InfoCommand();
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output,
                          "cache-directory=Unknown: Dependency file present but either Bazel is not present or the WORKSPACE file is mis-configured.\n" );
  }

  @Test
  public void info_deriveCacheDirOutsideWorkspace()
    throws Exception
  {
    writeDependencies( "" );
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setCacheDir( null );
    final Command command = new InfoCommand();
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output,
                          "cache-directory=Unknown: Dependency file present but either Bazel is not present or the WORKSPACE file is mis-configured.\n" );
  }

  @Test
  public void info_deriveCacheDirInsideWorkspaceMissingDependencies()
    throws Exception
  {
    writeWorkspace();
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setCacheDir( null );
    final Command command = new InfoCommand();
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertTrue( Pattern.compile( "cache-directory=.*\\.depgen-cache" ).matcher( output ).find() );
  }

  @Test
  public void info_deriveCacheDirOutsideWorkspaceMissingDependencies()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( handler );
    environment.setCacheDir( null );
    final Command command = new InfoCommand();
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output,
                          "cache-directory=Unknown: Dependency file not present and either Bazel is not present or the WORKSPACE file is mis-configured.\n" );
  }

  @Test
  public void info_badArgs()
    throws Exception
  {
    writeWorkspace();
    writeDependencies( "" );

    final TestHandler handler = new TestHandler();
    final InfoCommand command = new InfoCommand();
    final Environment environment = newEnvironment( handler );
    final boolean parsed = command.processOptions( environment, "--something-something" );
    assertFalse( parsed );
    assertEquals( handler.toString(), "Error: Unknown option --something-something" );
  }
}
