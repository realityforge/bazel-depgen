package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SettingsUtilTest
  extends AbstractTest
{
  @Test
  public void loadSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      FileUtil.write( "settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );
      final Path path = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      final TestHandler handler = new TestHandler();

      final Settings settings = SettingsUtil.loadSettings( path, createLogger( handler ) );
      assertNotNull( settings );

      assertEquals( handler.getRecords().size(), 0 );

      final Server server = settings.getServer( "my-repo" );
      assertNotNull( server );
      assertEquals( server.getUsername(), "root" );
      assertEquals( server.getPassword(), "secret" );
    } );
  }

  @Test
  public void loadSettings_withNonFatalWarnings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      FileUtil.write( "settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );
      final Path path = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      final TestHandler handler = new TestHandler();
      final Logger logger = createLogger( handler );
      final Settings settings = SettingsUtil.loadSettings( path, logger );
      assertNotNull( settings );
      final ArrayList<LogRecord> records = handler.getRecords();
      assertEquals( records.size(), 1 );
      assertTrue( records.get( 0 )
                    .getMessage()
                    .contains( "'servers.server.id' must be unique but found duplicate server with id my-repo" ) );
      final Server server = settings.getServer( "my-repo" );
      assertNotNull( server );
      assertEquals( server.getUsername(), "root" );
      assertEquals( server.getPassword(), "secret" );
    } );
  }

  @Test
  public void loadSettings_malformedSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      FileUtil.write( "settings.xml", "X\n" );
      final Path path = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      final TestHandler handler = new TestHandler();
      final Logger logger = createLogger( handler );
      assertThrows( SettingsBuildingException.class, () -> SettingsUtil.loadSettings( path, logger ) );
      assertEquals( handler.getRecords().size(), 0 );
    } );
  }
}
