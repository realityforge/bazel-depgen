package org.realityforge.bazel.depgen.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.config.Language;
import org.realityforge.bazel.depgen.config.Nature;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArtifactModelTest
  extends AbstractTest
{
  @Test
  public void parseArtifactWith2PartCoord()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
    assertEquals( model.getNature(), Nature.Auto );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp" );
  }

  @Test
  public void parseArtifactWith3PartCoord()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp:1.0" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
    assertEquals( model.getNature(), Nature.Auto );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp:jar:1.0" );
  }

  @Test
  public void parseArtifactWith4PartCoord()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp:jszip:1.0" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jszip" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
    assertEquals( model.getNature(), Nature.Auto );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp:jszip:1.0" );
  }

  @Test
  public void parseArtifactWith5PartCoord()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp:jszip:sources:1.0" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jszip" );
    assertEquals( model.getClassifier(), "sources" );
    assertNull( model.getAlias() );
    assertEquals( model.getNature(), Nature.Auto );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp:jszip:sources:1.0" );
  }

  @Test
  public void parseArtifactWith1PartCoord()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ArtifactModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The 'coord' property on the dependency must specify 2-5 components separated by the ':' character. The 'coords' must be in one of the forms; 'group:id', 'group:id:version', 'group:id:type:version' or 'group:id:type:classifier:version'." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWith6PartCoord()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp:jszip:sources:1.0:compile" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ArtifactModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The 'coord' property on the dependency must specify 2-5 components separated by the ':' character. The 'coords' must be in one of the forms; 'group:id', 'group:id:version', 'group:id:type:version' or 'group:id:type:classifier:version'." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithIncludeOptional()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setIncludeOptional( true );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
    assertEquals( model.getNature(), Nature.Auto );
    assertTrue( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
  }

  @Test
  public void parseArtifactWithExportDeps()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setExportDeps( true );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertEquals( model.getType(), "jar" );
    assertTrue( model.exportDeps( false ) );
  }

  @Test
  public void parseArtifactWithExportDeps_overrideGlobal()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setExportDeps( false );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertEquals( model.getType(), "jar" );
    assertFalse( model.exportDeps( true ) );
  }

  @Test
  public void parseArtifactWithAlias()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setAlias( "my-app" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertEquals( model.getAlias(), "my-app" );
    assertEquals( model.getNature(), Nature.Auto );
  }

  @Test
  public void parseArtifactWithNature()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setNature( Nature.LibraryAndPlugin );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertEquals( model.getNature(), Nature.LibraryAndPlugin );
  }

  @Test
  public void parseArtifactWithNoIncludeSource()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setIncludeSource( false );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
    assertFalse( model.includeOptional() );
    assertFalse( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
  }

  @Test
  public void parseArtifactWithCoordAndExcludes()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setExcludes( Arrays.asList( "com.biz.db", "com.biz.ui:core-ui" ) );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    final List<ExcludeModel> excludes = model.getExcludes();
    assertFalse( excludes.isEmpty() );
    assertEquals( excludes.size(), 2 );
    final ExcludeModel exclude1 = excludes.get( 0 );
    assertEquals( exclude1.getGroup(), "com.biz.db" );
    assertNull( exclude1.getId() );
    final ExcludeModel exclude2 = excludes.get( 1 );
    assertEquals( exclude2.getGroup(), "com.biz.ui" );
    assertEquals( exclude2.getId(), "core-ui" );
    assertEquals( model.toCoord(), "com.example:myapp" );
  }

  @Test
  public void parseArtifactWithVisibility()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setVisibility( Arrays.asList( "//project:__subpackages__", "//other:__subpackages__" ) );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertEquals( model.getVisibility(), Arrays.asList( "//project:__subpackages__", "//other:__subpackages__" ) );
  }

  @Test
  public void parseArtifactWithLanguagesSpecified()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setLanguages( Arrays.asList( Language.Java, Language.J2cl ) );

    assertEquals( ArtifactModel.parse( source ).getLanguages( Language.Java ),
                  Arrays.asList( Language.Java, Language.J2cl ) );
  }

  @Test
  public void parseArtifactWithLanguagesNotSpecified()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setLanguages( Arrays.asList( Language.Java, Language.J2cl ) );

    assertEquals( ArtifactModel.parse( source ).getLanguages( Language.Java ),
                  Arrays.asList( Language.Java, Language.J2cl ) );
  }

  @Test
  public void parseArtifactWithSpec2Elements()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp" );
  }

  @Test
  public void parseArtifactWithSpec3Elements()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp:1.0" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp:jar:1.0" );
  }

  @Test
  public void parseArtifactWithSpec4Elements()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp:jar:1.0" );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp:jar:1.0" );
  }

  @Test
  public void parseArtifactWithAllSpecElementsAndExcludes()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp:jszip:sources:1.0" );
    source.setExcludes( Arrays.asList( "com.biz.db", "com.biz.ui:core-ui" ) );

    final ArtifactModel model = ArtifactModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jszip" );
    assertEquals( model.getClassifier(), "sources" );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    final List<ExcludeModel> excludes = model.getExcludes();
    assertFalse( excludes.isEmpty() );
    assertEquals( excludes.size(), 2 );
    final ExcludeModel exclude1 = excludes.get( 0 );
    assertEquals( exclude1.getGroup(), "com.biz.db" );
    assertNull( exclude1.getId() );
    final ExcludeModel exclude2 = excludes.get( 1 );
    assertEquals( exclude2.getGroup(), "com.biz.ui" );
    assertEquals( exclude2.getId(), "core-ui" );
    assertEquals( model.toCoord(), "com.example:myapp:jszip:sources:1.0" );
  }

  @Test
  public void parseArtifactMissingCoord()
  {
    final ArtifactConfig source = new ArtifactConfig();

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ArtifactModel.parse( source ) );
    assertEquals( exception.getMessage(), "The dependency must specify the 'coord' property." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithPluginNatureSpecifyingLanguages()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setNature( Nature.Plugin );
    source.setLanguages( Collections.singletonList( Language.Java ) );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ArtifactModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must not specify the 'languages' property if the 'nature' property is specified as `Plugin`." );
    assertEquals( exception.getModel(), source );
  }
}
