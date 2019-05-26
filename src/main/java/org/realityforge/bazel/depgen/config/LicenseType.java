package org.realityforge.bazel.depgen.config;

/**
 * Types of licenses. Taken from https://docs.bazel.build/versions/master/be/functions.html#licenses
 */
public enum LicenseType
{
  /**
   * Requires mandatory source distribution.
   */
  restricted,

  /**
   * Allows usage of software freely in unmodified form. Any modifications must be made freely available.
   */
  reciprocal,

  /**
   * Original or modified third-party software may be shipped without danger nor encumbering other sources. All of the licenses in this category do, however, have an "original Copyright notice" or "advertising clause", wherein any external distributions must include the notice or clause specified in the license.
   */
  notice,

  /**
   * Code that is under a license but does not require a notice.
   */
  permissive,

  /**
   * Public domain, free for any use.
   */
  unencumbered
}
