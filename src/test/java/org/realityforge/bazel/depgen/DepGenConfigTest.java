package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.util.Map;
import javax.annotation.Nullable;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DepGenConfigTest
  extends AbstractDepGenTest
{
  @Test
  public void parseEmpty()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      assertNull( parseDependencies() );
    } );
  }

  @Test
  public void parseRepositories()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "repositories:\n  central: http://repo1.maven.org/maven2\n  example: https://example.com/repo\n" );
      final DepGenConfig config = parseDependencies();
      assertNotNull( config );
      final Map<String, String> repositories = config.getRepositories();
      assertNotNull( repositories );
      assertNull( config.getArtifacts() );

      assertEquals( repositories.size(), 2 );
      assertEquals( repositories.get( "example" ), "https://example.com/repo" );
      assertEquals( repositories.get( "central" ), "http://repo1.maven.org/maven2" );
    } );
  }

  @Nullable
  private DepGenConfig parseDependencies()
    throws Exception
  {
    return DepGenConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
  }
}
