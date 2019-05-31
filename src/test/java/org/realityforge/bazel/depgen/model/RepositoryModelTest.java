package org.realityforge.bazel.depgen.model;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.RepositoryConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RepositoryModelTest
  extends AbstractTest
{
  @Test
  public void create()
  {
    final RepositoryModel model =
      RepositoryModel.create( ApplicationConfig.MAVEN_CENTRAL_NAME, ApplicationConfig.MAVEN_CENTRAL_URL );

    assertNull( model.getSource() );
    assertEquals( model.getName(), ApplicationConfig.MAVEN_CENTRAL_NAME );
    assertEquals( model.getUrl(), ApplicationConfig.MAVEN_CENTRAL_URL );
    assertTrue( model.cacheLookups() );
  }

  @Test
  public void parse()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo" );

    final RepositoryModel model =
      RepositoryModel.parse( source );

    assertEquals( model.getSource(), source );
    assertEquals( model.getName(), "example" );
    assertEquals( model.getUrl(), "https://example.com/repo" );
    assertTrue( model.cacheLookups() );
  }

  @Test
  public void parse_nameOmitted()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setUrl( "https://example.com/repo/" );

    final RepositoryModel model =
      RepositoryModel.parse( source );

    assertEquals( model.getSource(), source );
    assertEquals( model.getName(), "https_example_com_repo" );
    assertEquals( model.getUrl(), "https://example.com/repo/" );
    assertTrue( model.cacheLookups() );
  }

  @Test
  public void parse_explicit_cacheLookups_FALSE()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo/" );
    source.setCacheLookups( Boolean.FALSE );

    final RepositoryModel model =
      RepositoryModel.parse( source );

    assertEquals( model.getSource(), source );
    assertEquals( model.getName(), "example" );
    assertEquals( model.getUrl(), "https://example.com/repo/" );
    assertFalse( model.cacheLookups() );
  }

  @Test
  public void parse_explicit_cacheLookups_TRUE()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo/" );
    source.setCacheLookups( Boolean.TRUE );

    final RepositoryModel model =
      RepositoryModel.parse( source );

    assertEquals( model.getSource(), source );
    assertEquals( model.getName(), "example" );
    assertEquals( model.getUrl(), "https://example.com/repo/" );
    assertTrue( model.cacheLookups() );
  }
}
