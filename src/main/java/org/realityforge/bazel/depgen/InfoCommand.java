package org.realityforge.bazel.depgen;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.util.BazelUtil;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;

final class InfoCommand
  extends ConfigurableCommand
{
  @Nonnull
  static final String COMMAND = "info";
  @Nonnull
  private final Set<String> _outputKeys = new HashSet<>();

  InfoCommand()
  {
    super( COMMAND, "Print runtime info about the tool.", new CLOptionDescriptor[ 0 ] );
  }

  @Override
  boolean processArguments( @Nonnull final Environment environment, @Nonnull final List<CLOption> arguments )
  {
    // Get a list of parsed options
    for ( final CLOption option : arguments )
    {
      assert CLOption.TEXT_ARGUMENT == option.getId();
      _outputKeys.add( option.getArgument() );
    }

    return true;
  }

  @Override
  int run( @Nonnull final Context context )
  {
    printInfo( context, "config-file", () -> context.environment().getConfigFile() );
    printInfo( context, "settings-file", () -> context.environment().getSettingsFile() );
    printInfo( context, "cache-directory", () -> getCacheDir( context ) );
    printInfo( context, "reset-cached-metadata", () -> context.environment().shouldResetCachedMetadata() );
    printInfo( context,
               "bazel-repository-cache",
               () -> BazelUtil.getRepositoryCache( context.environment().currentDirectory().toFile() ) );
    return ExitCodes.SUCCESS_EXIT_CODE;
  }

  @Nonnull
  private String getCacheDir( @Nonnull final Context context )
  {
    if ( context.environment().hasCacheDir() )
    {
      return context.environment().getCacheDir().toString();
    }
    else if ( Files.exists( context.environment().getConfigFile() ) )
    {
      try
      {
        return Main.getCacheDirectory( context.environment(), context.loadModel() ).toString();
      }
      catch ( final TerminalStateException tse )
      {
        return "Unknown: Dependency file present but either Bazel is not present or the WORKSPACE file is mis-configured.";
      }
    }
    else
    {
      final File repositoryCache = BazelUtil.getOutputBase( context.environment().currentDirectory().toFile() );
      if ( null == repositoryCache )
      {
        return "Unknown: Dependency file not present and either Bazel is not present or the WORKSPACE file is mis-configured.";
      }
      else
      {
        return repositoryCache.toPath().resolve( ".depgen-cache" ).toString();
      }
    }
  }

  private void printInfo( @Nonnull final Context context,
                          @Nonnull final String key,
                          @Nonnull final Supplier<Object> accessor )
  {
    if ( _outputKeys.isEmpty() || _outputKeys.contains( key ) )
    {
      final Logger logger = context.environment().logger();
      if ( logger.isLoggable( Level.WARNING ) )
      {
        logger.log( Level.WARNING, key + "=" + accessor.get() );
      }
    }
  }
}
