package org.realityforge.bazel.depgen.record;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface RecordBuildCallback
{
  void onWarning( @Nonnull String message );
}
