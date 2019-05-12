package org.realityforge.bazel.depgen.util;

import gir.io.FileUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class OrderedPropertiesTest
  extends AbstractTest
{
  @Test
  public void keySet()
  {
    final OrderedProperties properties = new OrderedProperties();
    properties.put( "1", "1" );
    properties.put( "5", "1" );
    properties.put( "2", "1" );
    properties.put( "4", "1" );
    properties.put( "3", "1" );

    assertEquals( properties.keySet(), Arrays.asList( "1", "2", "3", "4", "5" ) );
  }

  @Test
  public void store()
    throws Exception
  {
    final OrderedProperties properties = new OrderedProperties();
    properties.put( "1", "1" );
    properties.put( "5", "1" );
    properties.put( "2", "1" );
    properties.put( "4", "1" );
    properties.put( "3", "1" );

    inIsolatedDirectory( () -> {

      final Path file = FileUtil.createLocalTempDir().resolve( "myfile.properties" );

      properties.store( Files.newBufferedWriter( file ), null );

      final List<String> lines = Files.readAllLines( file );
      // Note: we are skipping over comment line
      assertEquals( String.join( "\n", lines.subList( 1, lines.size() ) ),
                    "1=1\n" +
                    "2=1\n" +
                    "3=1\n" +
                    "4=1\n" +
                    "5=1" );
    } );

  }
}
