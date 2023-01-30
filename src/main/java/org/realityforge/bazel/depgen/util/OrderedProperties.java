package org.realityforge.bazel.depgen.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;

/**
 * A simple customization of Properties that has a stable output order based on alphabetic ordering of keys.
 */
public final class OrderedProperties
  extends Properties
{
  @Override
  @Nonnull
  public synchronized Enumeration<Object> keys()
  {
    return Collections.enumeration( keySet() );
  }

  @Override
  @Nonnull
  public Set<Object> keySet()
  {
    return new TreeSet<>( super.keySet() );
  }
}
