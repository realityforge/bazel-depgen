package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ResolverUtilTest
  extends AbstractTest
{
  @Test
  public void getRemoteRepositories()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final HashMap<String, String> repositories = new HashMap<>();
      repositories.put( "central", "https://repo1.maven.org/maven2" );
      repositories.put( "example", "https://example.com/maven2" );

      FileUtil.write( "settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>example</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );

      final Settings settings =
        SettingsUtil.loadSettings( FileUtil.getCurrentDirectory().resolve( "settings.xml" ),
                                   Logger.getAnonymousLogger() );

      final ApplicationConfig source = new ApplicationConfig();
      source.setRepositories( repositories );
      final ApplicationModel model = ApplicationModel.parse( source );
      final List<RemoteRepository> remoteRepositories = ResolverUtil.getRemoteRepositories( model, settings );

      assertEquals( remoteRepositories.size(), 2 );
      final RemoteRepository central = remoteRepositories.get( 0 );
      assertEquals( central.getId(), "central" );
      assertEquals( central.getUrl(), "https://repo1.maven.org/maven2" );
      assertNull( central.getAuthentication() );

      final RemoteRepository example = remoteRepositories.get( 1 );
      assertEquals( example.getId(), "example" );
      assertEquals( example.getUrl(), "https://example.com/maven2" );
      assertNotNull( example.getAuthentication() );
    } );
  }
}
