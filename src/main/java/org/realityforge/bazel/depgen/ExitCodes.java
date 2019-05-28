package org.realityforge.bazel.depgen;

final class ExitCodes
{
  static final int SUCCESS_EXIT_CODE = 0;
  static final int ERROR_EXIT_CODE = 1;
  static final int ERROR_PARSING_ARGS_EXIT_CODE = 2;
  static final int ERROR_PARSING_DEPENDENCIES_CODE = 3;
  static final int ERROR_LOADING_SETTINGS_CODE = 4;
  static final int ERROR_CONSTRUCTING_MODEL_CODE = 5;
  static final int ERROR_INVALID_POM_CODE = 6;
  static final int ERROR_CYCLES_PRESENT_CODE = 7;
  static final int ERROR_COLLECTING_DEPENDENCIES_CODE = 8;

  private ExitCodes()
  {
  }
}
