package org.realityforge.bazel.depgen;

import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;

abstract class Command
{
  interface Context
  {
    @Nonnull
    Environment environment();

    @Nonnull
    ApplicationModel loadModel();

    @Nonnull
    ApplicationRecord loadRecord()
      throws Exception;
  }

  abstract int run( @Nonnull Context context )
    throws Exception;
}
