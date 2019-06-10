package org.realityforge.bazel.depgen.config;

public enum J2clMode
{
  /**
   * The artifact is a binary dependency such as annotations that need not be transpiled to javascript.
   * The code will be included using the <code>j2cl_import</code> macro.
   */
  Import,
  /**
   * The artifact is a "library" dependency and must be transpiled to javascript.
   * The code will be included using the <code>j2cl_library</code> macro.
   */
  Library
}
