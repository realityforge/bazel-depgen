package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;

public final class Version
{
  @Nonnull
  private static final String VERSION = loadVersion();
  @Nonnull
  static final String PROPERTY_KEY = "bazel-degen.version";

  @Nonnull
  public static String get()
  {
    return VERSION;
  }

  @Nonnull
  static String loadVersion()
  {
    final String versionProperty = System.getProperty( PROPERTY_KEY );
    if ( null != versionProperty )
    {
      return versionProperty;
    }
    else
    {
      final InputStream inputStream = Version.class.getResourceAsStream( "version.txt" );
      assert null != inputStream;
      try
      {
        final byte[] bytes = new byte[ inputStream.available() ];
        final int count = inputStream.read( bytes );
        assert bytes.length == count;
        return new String( bytes, StandardCharsets.UTF_8 );
      }
      catch ( final IOException e )
      {
        throw new IllegalStateException( "Failed to read version.txt resource", e );
      }
    }
  }

  private Version()
  {
  }
}
