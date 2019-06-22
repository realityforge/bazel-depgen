package org.realityforge.bazel.depgen.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nonnull;

public final class HashUtil
{
  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  private HashUtil()
  {
  }

  @Nonnull
  public static String sha256( @Nonnull final byte[]... data )
  {
    try
    {
      final MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
      for ( final byte[] datum : data )
      {
        digest.update( datum );
      }
      return bytesToHex( digest.digest() );
    }
    catch ( final NoSuchAlgorithmException nsae )
    {
      throw new IllegalStateException( nsae.getMessage(), nsae );
    }
  }

  @Nonnull
  private static String bytesToHex( @Nonnull final byte[] bytes )
  {
    final char[] hexChars = new char[ bytes.length * 2 ];
    for ( int i = 0; i < bytes.length; i++ )
    {
      final int v = bytes[ i ] & 0xFF;
      hexChars[ i * 2 ] = HEX_ARRAY[ v >>> 4 ];
      hexChars[ i * 2 + 1 ] = HEX_ARRAY[ v & 0x0F ];
    }
    return new String( hexChars );
  }
}
