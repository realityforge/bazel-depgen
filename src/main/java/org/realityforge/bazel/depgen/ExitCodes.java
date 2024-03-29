package org.realityforge.bazel.depgen;

final class ExitCodes
{
  static final int SUCCESS_EXIT_CODE = 0;
  static final int ERROR_EXIT_CODE = 1;
  static final int ERROR_PARSING_ARGS_EXIT_CODE = 2;
  static final int ERROR_LOADING_CONFIG_CODE = 3;
  static final int ERROR_LOADING_SETTINGS_CODE = 4;
  static final int ERROR_CONSTRUCTING_MODEL_CODE = 5;
  static final int ERROR_INVALID_POM_CODE = 6;
  static final int ERROR_CYCLES_PRESENT_CODE = 7;
  static final int ERROR_COLLECTING_DEPENDENCIES_CODE = 8;
  static final int ERROR_BAD_SHA256_CONFIG_CODE = 10;
  static final int ERROR_DEPENDENCY_CONFIG_PRESENT_CODE = 11;
  static final int ERROR_INIT_WRITE_FAILED_CODE = 12;
  static final int ERROR_SYSTEM_CONFIGURATION_CODE = 13;
  static final int ERROR_CONFIG_VALIDATION_CODE = 14;
  static final int ERROR_RUNTIME_CODE = 15;

  private ExitCodes()
  {
  }
}
