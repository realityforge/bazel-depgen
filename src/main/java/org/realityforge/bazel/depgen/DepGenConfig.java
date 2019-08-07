package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import javax.annotation.Nonnull;

public final class DepGenConfig
{
  @Nonnull
  static final String PROPERTY_KEY = "bazel-degen.version";
  @Nonnull
  private static final Properties c_config = loadConfig();

  @Nonnull
  public static String getVersion()
  {
    final String versionProperty = System.getProperty( PROPERTY_KEY );
    if ( null != versionProperty )
    {
      return versionProperty;
    }
    else
    {
      return Objects.requireNonNull( c_config.getProperty( "version" ) );
    }
  }

  @Nonnull
  public static String getCoord()
  {
    return getGroupId() + ":" + getArtifactId() + ":jar:" + getClassifier() + ":" + getVersion();
  }

  @Nonnull
  public static String getGroupId()
  {
    return Objects.requireNonNull( c_config.getProperty( "group" ) );
  }

  @Nonnull
  public static String getArtifactId()
  {
    return Objects.requireNonNull( c_config.getProperty( "id" ) );
  }

  @Nonnull
  public static String getClassifier()
  {
    return "all";
  }

  @Nonnull
  private static Properties loadConfig()
  {
    final InputStream inputStream = DepGenConfig.class.getResourceAsStream( "config.properties" );
    assert null != inputStream;

    try
    {
      final Properties properties = new Properties();
      properties.load( inputStream );
      return properties;
    }
    catch ( final IOException e )
    {
      throw new DepgenConfigurationException( "Failed to load config.properties", e );
    }
  }

  private DepGenConfig()
  {
  }
}
