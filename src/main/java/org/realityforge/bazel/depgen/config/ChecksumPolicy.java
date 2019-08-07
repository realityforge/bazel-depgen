package org.realityforge.bazel.depgen.config;

public enum ChecksumPolicy
{
  /**
   * Verify checksums and fail the resolution if they do not match.
   */
  fail,

  /**
   * Verify checksums and warn if they do not match.
   */
  warn,

  /**
   * Do not verify checksums.
   */
  ignore
}
