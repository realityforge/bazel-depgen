package org.realityforge.bazel.depgen.model;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.ChecksumPolicy;
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

  @Test
  public void parse_implicit_searchByDefault()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo/" );

    assertTrue( RepositoryModel.parse( source ).searchByDefault() );
  }

  @Test
  public void parse_explicit_searchByDefault_FALSE()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo/" );
    source.setSearchByDefault( Boolean.FALSE );

    assertFalse( RepositoryModel.parse( source ).searchByDefault() );
  }

  @Test
  public void parse_explicit_searchByDefault_TRUE()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo/" );
    source.setSearchByDefault( Boolean.TRUE );

    assertTrue( RepositoryModel.parse( source ).searchByDefault() );
  }

  @Test
  public void parse_implicit_checksumPolicy()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo/" );

    assertEquals( RepositoryModel.parse( source ).checksumPolicy(), ChecksumPolicy.fail );
  }

  @Test
  public void parse_explicit_checksumPolicy()
  {
    final RepositoryConfig source = new RepositoryConfig();
    source.setName( "example" );
    source.setUrl( "https://example.com/repo/" );
    source.setChecksumPolicy( ChecksumPolicy.ignore );

    assertEquals( RepositoryModel.parse( source ).checksumPolicy(), ChecksumPolicy.ignore );
  }

  @Test
  public void parse_missing_url()
  {
    final RepositoryConfig config = new RepositoryConfig();
    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> RepositoryModel.parse( config ) );

    assertEquals( exception.getMessage(), "The repository must specify the 'url' property." );
    assertEquals( exception.getModel(), config );
  }
}
