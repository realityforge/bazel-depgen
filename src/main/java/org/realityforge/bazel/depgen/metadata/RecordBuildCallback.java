package org.realityforge.bazel.depgen.metadata;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface RecordBuildCallback
{
  void onWarning( @Nonnull String message );
}
