package org.realityforge.bazel.depgen;

import java.util.Objects;
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

  @Nonnull
  private final String _option;

  Command( @Nonnull final String option )
  {
    _option = Objects.requireNonNull( option );
  }
  abstract int run( @Nonnull Context context )
    throws Exception;
}
