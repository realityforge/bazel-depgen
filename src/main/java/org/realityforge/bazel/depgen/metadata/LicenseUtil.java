package org.realityforge.bazel.depgen.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.config.LicenseType;
import static java.util.regex.Pattern.*;

final class LicenseUtil
{
  //notice licenses
  private static final Pattern APACHE = Pattern.compile( ".*Apache.*" );
  private static final Pattern APACHE_ASF_LICENSE = Pattern.compile( ".*ASF.*License.*" );
  private static final Pattern APACHE_ASF = Pattern.compile( ".*ASF.*2.*" );
  private static final Pattern MIT = Pattern.compile( ".*MIT.*" );
  private static final Pattern BSD = Pattern.compile( ".*BSD.*" );
  private static final Pattern FACEBOOK = Pattern.compile( ".*Facebook.*License.*" );
  private static final Pattern JSON = Pattern.compile( ".*JSON.*License.*" );
  private static final Pattern BOUNCY_CASTLE = Pattern.compile( ".*Bouncy.*Castle.*" );
  private static final Pattern CDDL = Pattern.compile( ".*CDDL.*" );
  private static final Pattern COMMON_PUBLIC = Pattern.compile( ".*Common.+Public.+License.*", CASE_INSENSITIVE );
  private static final Pattern CDDL_FULL =
    Pattern.compile( ".*COMMON.+DEVELOPMENT.+AND.+DISTRIBUTION.+LICENSE.*", CASE_INSENSITIVE );
  private static final Pattern GOOGLE_CLOUD = Pattern.compile( "Google Cloud Software License" );
  private static final Pattern INDIANA_U = Pattern.compile( ".*Indiana.+University.+License.*" );
  private static final Pattern ICU = Pattern.compile( ".*ICU.+License.*" );
  private static final List<Pattern> NOTICE_LICENSES =
    Arrays.asList( APACHE,
                   APACHE_ASF,
                   APACHE_ASF_LICENSE,
                   MIT,
                   BSD,
                   FACEBOOK,
                   JSON,
                   BOUNCY_CASTLE,
                   COMMON_PUBLIC,
                   CDDL,
                   CDDL_FULL,
                   GOOGLE_CLOUD,
                   INDIANA_U,
                   ICU );
  //reciprocal licenses
  private static final Pattern ECLIPSE = Pattern.compile( ".*Eclipse\\s+Public\\s+License.*\\s+.*1.*" );
  private static final Pattern EPL = Pattern.compile( ".*EPL\\s+.*1.*" );
  private static final Pattern MOZILLA_MPL = Pattern.compile( ".*MPL.*1.1.*" );
  private static final Pattern MOZILLA = Pattern.compile( ".*Mozilla.*License.*", CASE_INSENSITIVE );
  private static final List<Pattern> RECIPROCAL_LICENSES = Arrays.asList( ECLIPSE, EPL, MOZILLA_MPL, MOZILLA );
  //restricted licenses
  private static final Pattern GNU = Pattern.compile( ".*GNU.*" );
  private static final Pattern LGPL = Pattern.compile( ".*LGPL.*" );
  private static final List<Pattern> RESTRICTED_LICENSES = Arrays.asList( GNU, LGPL );
  //unencumbered licenses
  private static final Pattern CC0 = Pattern.compile( ".*CC0.*" );
  private static final Pattern PUBLIC_DOMAIN = Pattern.compile( ".*Public.*Domain.*" );
  private static final Pattern ANDROID_SDK = Pattern.compile( ".*Android.*License.*" );
  private static final Pattern NO_WARRANTY = Pattern.compile( ".*provided.*without.*support.*or.*warranty.*" );
  private static final List<Pattern> UNENCUMBERED_LICENSES =
    Arrays.asList( CC0, PUBLIC_DOMAIN, ANDROID_SDK, NO_WARRANTY );
  //permissive
  private static final Pattern WTFPL = Pattern.compile( ".*WTFPL.*" );
  private static final List<Pattern> PERMISSIVE_LICENSES = Collections.singletonList( WTFPL );

  private LicenseUtil()
  {
  }

  /**
   * Mapping between a license and its type.
   * Data taken from https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licenses
   * Or from the licenses themselves.
   * Or from https://source.bazel.build/search?q=licenses%20f:BUILD
   */
  @Nullable
  static LicenseType classifyLicense( @Nonnull final String licenseName )
  {
    if ( anyMatch( NOTICE_LICENSES, licenseName ) )
    {
      return LicenseType.notice;
    }
    else if ( anyMatch( RECIPROCAL_LICENSES, licenseName ) )
    {
      return LicenseType.reciprocal;
    }
    else if ( anyMatch( RESTRICTED_LICENSES, licenseName ) )
    {
      return LicenseType.restricted;
    }
    else if ( anyMatch( UNENCUMBERED_LICENSES, licenseName ) )
    {
      return LicenseType.unencumbered;
    }
    else if ( anyMatch( PERMISSIVE_LICENSES, licenseName ) )
    {
      return LicenseType.permissive;
    }
    else
    {
      return null;
    }
  }

  private static boolean anyMatch( @Nonnull final List<Pattern> patterns, @Nonnull final String text )
  {
    return patterns.stream().anyMatch( pattern -> pattern.matcher( text ).find() );
  }
}
