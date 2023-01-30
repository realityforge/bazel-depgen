package org.realityforge.bazel.depgen.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
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
    // Used in Java8 when writing properties
    return new TreeSet<>( super.keySet() );
  }

  @SuppressWarnings( "UseBulkOperation" )
  @Nonnull
  @Override
  public Set<Map.Entry<Object, Object>> entrySet()
  {
    // Used in Java17+ when writing properties
    final TreeMap<Object, Object> map = new TreeMap<>();
    for ( final Map.Entry<Object, Object> entry : super.entrySet() )
    {
      map.put( entry.getKey(), entry.getValue() );
    }
    return map.entrySet();
  }
}
