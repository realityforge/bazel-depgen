package org.realityforge.bazel.depgen;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;

final class CommandContextImpl
  implements Command.Context
{
  @Nonnull
  private final Environment _environment;

  CommandContextImpl( @Nonnull final Environment environment )
  {
    _environment = Objects.requireNonNull( environment );
  }

  @Nonnull
  @Override
  public Environment environment()
  {
    return _environment;
  }

  @Nonnull
  @Override
  public ApplicationModel loadModel()
  {
    return Main.loadModel( _environment );
  }

  @Nonnull
  @Override
  public ApplicationRecord loadRecord()
    throws Exception
  {
    return Main.loadRecord( _environment );
  }
}
