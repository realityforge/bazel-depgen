package org.realityforge.bazel.depgen.metadata;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.LicenseType;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class LicenseUtilTest
  extends AbstractTest
{
  @Test
  public void classifyLicense()
  {
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "Apache 2.0" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "Apache License" ) );
    assertEquals( LicenseType.notice,
                  LicenseUtil.classifyLicense( "Similar to Apache License but with the acknowledgment clause removed" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "ASF 2.0" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "Apache License, Version 2.0" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "The Apache Software License, Version 2.0" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "The MIT License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "MIT License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "MIT" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "Bouncy Castle Licence" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "New BSD License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "BSD 2-Clause License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "BSD License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "Facebook Platform License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "The JSON License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "Common Public License Version 1.0" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "CDDL + GPLv2 with classpath exception" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "CDDL/GPLv2+CE" ) );
    assertEquals( LicenseType.notice,
                  LicenseUtil.classifyLicense( "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "Google Cloud Software License" ) );
    assertEquals( LicenseType.notice,
                  LicenseUtil.classifyLicense( "Indiana University Extreme! Lab Software License, version 1.1.1" ) );
    assertEquals( LicenseType.notice,
                  LicenseUtil.classifyLicense( "Indiana University Extreme! Lab Software License, version 1.2" ) );
    assertEquals( LicenseType.notice,
                  LicenseUtil.classifyLicense( "Indiana University Extreme! Lab Software License" ) );
    assertEquals( LicenseType.notice, LicenseUtil.classifyLicense( "ICU License" ) );

    assertEquals( LicenseType.reciprocal, LicenseUtil.classifyLicense( "Eclipse Public License 1.0" ) );
    assertEquals( LicenseType.reciprocal, LicenseUtil.classifyLicense( "Eclipse Public License v 1.0" ) );
    assertEquals( LicenseType.reciprocal, LicenseUtil.classifyLicense( "Eclipse Public License, Version 1.0" ) );
    assertEquals( LicenseType.reciprocal, LicenseUtil.classifyLicense( "EPL 1" ) );
    assertEquals( LicenseType.reciprocal, LicenseUtil.classifyLicense( "MPL 1.1" ) );
    assertEquals( LicenseType.reciprocal, LicenseUtil.classifyLicense( "Mozilla License" ) );

    assertEquals( LicenseType.restricted, LicenseUtil.classifyLicense( "GNU GPL v2" ) );
    assertEquals( LicenseType.restricted,
                  LicenseUtil.classifyLicense( "GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1" ) );
    assertEquals( LicenseType.restricted, LicenseUtil.classifyLicense( "GNU Lesser Public License" ) );
    assertEquals( LicenseType.restricted, LicenseUtil.classifyLicense( "LGPL" ) );

    assertEquals( LicenseType.unencumbered, LicenseUtil.classifyLicense( "CC0 1.0 Universal License" ) );
    assertEquals( LicenseType.unencumbered, LicenseUtil.classifyLicense( "Public Domain" ) );
    assertEquals( LicenseType.unencumbered, LicenseUtil.classifyLicense( "Android Software Development Kit License" ) );
    assertEquals( LicenseType.unencumbered, LicenseUtil.classifyLicense( "provided without support or warranty" ) );

    assertEquals( LicenseType.permissive, LicenseUtil.classifyLicense( "WTFPL" ) );
  }
}
