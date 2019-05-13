package org.realityforge.bazel.depgen.model;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
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

    final ArtifactModel model = parseModel( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
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

    final ArtifactModel model = parseModel( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
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

    final ArtifactModel model = parseModel( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jszip" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
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

    final ArtifactModel model = parseModel( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jszip" );
    assertEquals( model.getClassifier(), "sources" );
    assertNull( model.getAlias() );
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
      expectThrows( InvalidModelException.class, () -> parseModel( source ) );
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
      expectThrows( InvalidModelException.class, () -> parseModel( source ) );
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

    final ArtifactModel model = parseModel( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertFalse( model.isVersioned() );
    assertNull( model.getVersion() );
    assertEquals( model.getType(), "jar" );
    assertNull( model.getClassifier() );
    assertNull( model.getAlias() );
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

    final ArtifactModel model = parseModel( source );
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

    final ArtifactModel model = parseModel( source );
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

    final ArtifactModel model = parseModel( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertEquals( model.getAlias(), "my-app" );
  }

  @Test
  public void parseArtifactWithNoIncludeSource()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setIncludeSource( false );

    final ArtifactModel model = parseModel( source );
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

    final ArtifactModel model = parseModel( source );
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

    final ArtifactModel model = parseModel( source );
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
  public void parseArtifactWithSpec2Elements()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );

    final ArtifactModel model = parseModel( source );
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
  public void parseArtifactWithIdsProperty()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );
    source.setIds( Arrays.asList( "core", "util", "ui" ) );

    final List<ArtifactModel> models = ArtifactModel.parse( source );
    assertEquals( models.size(), 3 );
    assertEquals( models.get( 0 ).getGroup(), "com.example" );
    assertEquals( models.get( 0 ).getId(), "core" );
    assertEquals( models.get( 0 ).toCoord(), "com.example:core" );

    assertEquals( models.get( 1 ).getGroup(), "com.example" );
    assertEquals( models.get( 1 ).getId(), "util" );
    assertEquals( models.get( 1 ).toCoord(), "com.example:util" );

    assertEquals( models.get( 2 ).getGroup(), "com.example" );
    assertEquals( models.get( 2 ).getId(), "ui" );
    assertEquals( models.get( 2 ).toCoord(), "com.example:ui" );
  }

  @Test
  public void parseArtifactWithSpec3Elements()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );
    source.setVersion( "1.0" );

    final ArtifactModel model = parseModel( source );
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
    source.setGroup( "com.example" );
    source.setId( "myapp" );
    source.setVersion( "1.0" );
    source.setClassifier( "sources" );

    final ArtifactModel model = parseModel( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertTrue( model.isVersioned() );
    assertEquals( model.getVersion(), "1.0" );
    assertEquals( model.getType(), "jar" );
    assertEquals( model.getClassifier(), "sources" );
    assertFalse( model.includeOptional() );
    assertTrue( model.includeSource( true ) );
    assertFalse( model.exportDeps( false ) );
    assertTrue( model.getExcludes().isEmpty() );
    assertTrue( model.getVisibility().isEmpty() );
    assertEquals( model.toCoord(), "com.example:myapp:jar:sources:1.0" );
  }

  @Test
  public void parseArtifactWithAllSpecElementsAndExcludes()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );
    source.setVersion( "1.0" );
    source.setClassifier( "sources" );
    source.setType( "jszip" );
    source.setExcludes( Arrays.asList( "com.biz.db", "com.biz.ui:core-ui" ) );

    final ArtifactModel model = parseModel( source );
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
  public void parseArtifactMissingGroup()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setId( "myapp" );

    final InvalidModelException exception = expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must specify the 'group' property unless the 'coord' shorthand property is used." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactMissingVersionWhenTypeSpecified()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );
    source.setType( "jszip" );

    final InvalidModelException exception = expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must specify either the 'version' property if the 'type' property is specified." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactMissingVersionWhenClassifierSpecified()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );
    source.setClassifier( "sources" );

    final InvalidModelException exception = expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must specify either the 'version' property if the 'classifier' property is specified." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithIdAndIds()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );
    source.setIds( Arrays.asList( "core", "ui" ) );

    final InvalidModelException exception = expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must not specify both the 'id' property and the 'ids' property." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithNeitherIdNorIds()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setGroup( "com.example" );

    final InvalidModelException exception = expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must specify either the 'id' property or the 'ids' property." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithCoordAndGroup()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setGroup( "com.example" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group, id, version, classifier, type or ids." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithCoordAndId()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setId( "myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group, id, version, classifier, type or ids." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithCoordAndIds()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setIds( Arrays.asList( "core", "util" ) );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group, id, version, classifier, type or ids." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithCoordAndClassifier()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setClassifier( "sources" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group, id, version, classifier, type or ids." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseArtifactWithCoordAndType()
  {
    final ArtifactConfig source = new ArtifactConfig();
    source.setCoord( "com.example:myapp" );
    source.setClassifier( "jar" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> parseModel( source ) );
    assertEquals( exception.getMessage(),
                  "The dependency must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group, id, version, classifier, type or ids." );
    assertEquals( exception.getModel(), source );
  }

  @Nonnull
  private ArtifactModel parseModel( @Nonnull final ArtifactConfig source )
  {
    final List<ArtifactModel> models = ArtifactModel.parse( source );
    assertEquals( models.size(), 1 );
    return models.get( 0 );
  }
}
