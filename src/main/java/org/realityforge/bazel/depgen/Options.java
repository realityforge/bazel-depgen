package org.realityforge.bazel.depgen;

import org.realityforge.getopt4j.CLOptionDescriptor;

final class Options
{
  static final String DEFAULT_DEPENDENCIES_FILE = "dependencies.yml";
  static final String DEFAULT_CACHE_DIR = ".repository";
  static final int DEPENDENCIES_FILE_OPT = 'd';
  static final CLOptionDescriptor DEPENDENCIES_DESCRIPTOR =
    new CLOptionDescriptor( "dependencies-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            DEPENDENCIES_FILE_OPT,
                            "The path to the yaml file containing the dependencies. Defaults to '" +
                            DEFAULT_DEPENDENCIES_FILE + "' in the workspace directory." );
  static final int SETTINGS_FILE_OPT = 's';
  static final CLOptionDescriptor SETTINGS_DESCRIPTOR =
    new CLOptionDescriptor( "settings-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            SETTINGS_FILE_OPT,
                            "The path to the settings.xml used by Maven to extract repository credentials. " +
                            "Defaults to '~/.m2/settings.xml'." );
  static final int CACHE_DIR_OPT = 'r';
  static final CLOptionDescriptor CACHE_DESCRIPTOR =
    new CLOptionDescriptor( "cache-dir",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            CACHE_DIR_OPT,
                            "The path to the directory in which to cache downloads from remote " +
                            "repositories. Defaults to '" + DEFAULT_CACHE_DIR + "' in the workspace directory." );
  static final int RESET_CACHED_METADATA_OPT = 1;
  static final CLOptionDescriptor RESET_CACHED_METADATA_DESCRIPTOR =
    new CLOptionDescriptor( "reset-cached-metadata",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            RESET_CACHED_METADATA_OPT,
                            "Recalculate metadata about an artifact." );

  private Options()
  {
  }
}
