package org.realityforge.bazel.depgen.model;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.List;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.Language;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationModelTest
  extends AbstractTest
{
  @Test
  public void parse()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "excludes:\n" +
                         "  - coord: com.example:blib\n" +
                         "replacements:\n" +
                         "  - coord: com.example:alib\n" +
                         "    target: \"@com_example//:alib\"\n" );
      final Path configFile = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      final ApplicationConfig source = ApplicationConfig.parse( configFile );

      final ApplicationModel model = ApplicationModel.parse( source );
      assertEquals( model.getSource(), source );
      assertEquals( model.getConfigSha256(), "5554C636655BB43F8BC8A1E01C985419ABAF8A4102ABEDD28CB1949FA8A7DADA" );
      assertEquals( model.getConfigLocation(), configFile );
      final List<RepositoryModel> repositories = model.getRepositories();
      assertEquals( repositories.size(), 1 );
      final RepositoryModel repository = repositories.get( 0 );
      assertEquals( repository.getName(), ApplicationConfig.MAVEN_CENTRAL_NAME );
      assertEquals( repository.getUrl(), ApplicationConfig.MAVEN_CENTRAL_URL );
      assertEquals( model.getOptions().getWorkspaceDirectory(), FileUtil.getCurrentDirectory() );
      assertEquals( model.getOptions().getExtensionFile(),
                    FileUtil.getCurrentDirectory().resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) );
      assertEquals( model.getArtifacts().size(), 1 );
      final ArtifactModel artifactModel = model.getArtifacts().get( 0 );
      assertEquals( artifactModel.getGroup(), "com.example" );
      assertEquals( artifactModel.getId(), "myapp" );
      assertEquals( artifactModel.getVersion(), "1.0" );

      final List<ReplacementModel> replacements = model.getReplacements();
      assertEquals( replacements.size(), 1 );
      final ReplacementModel replacementModel = replacements.get( 0 );
      assertEquals( replacementModel.getGroup(), "com.example" );
      assertEquals( replacementModel.getId(), "alib" );
      assertEquals( replacementModel.getTarget(), "@com_example//:alib" );

      final List<GlobalExcludeModel> excludes = model.getExcludes();
      assertEquals( excludes.size(), 1 );
      final GlobalExcludeModel excludeModel = excludes.get( 0 );
      assertEquals( excludeModel.getGroup(), "com.example" );
      assertEquals( excludeModel.getId(), "blib" );
    } );
  }

  @Test
  public void isExcluded()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "excludes:\n" +
                         "  - coord: com.example:blib\n" );
      final ApplicationModel model = loadApplicationModel();

      assertFalse( model.isExcluded( "com.example", "alib" ) );
      assertTrue( model.isExcluded( "com.example", "blib" ) );
    } );
  }

  @Test
  public void findArtifact()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      final ApplicationModel model = loadApplicationModel();
      assertEquals( model.getArtifacts().size(), 1 );
      final ArtifactModel artifactModel = model.getArtifacts().get( 0 );
      assertEquals( artifactModel.toCoord(), "com.example:myapp:jar:1.0" );

      assertEquals( artifactModel, model.findArtifact( "com.example", "myapp" ) );
      assertNull( model.findArtifact( "com.example", "noexist" ) );
    } );
  }

  @Test
  public void findReplacement()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "replacements:\n" +
                         "  - coord: com.example:mylib\n" +
                         "    target: \"@com_example//:mylib\"\n" );

      final ApplicationModel model = loadApplicationModel();
      assertEquals( model.getReplacements().size(), 1 );
      final ReplacementModel replacementModel = model.getReplacements().get( 0 );
      assertEquals( replacementModel.getGroup(), "com.example" );
      assertEquals( replacementModel.getId(), "mylib" );
      assertEquals( replacementModel.getTarget(), "@com_example//:mylib" );

      assertEquals( replacementModel, model.findReplacement( "com.example", "mylib" ) );
      assertNull( model.findArtifact( "com.example", "noexist" ) );
    } );
  }

  @Test
  public void findRepository()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      final ApplicationModel model = loadApplicationModel();
      final List<RepositoryModel> repositories = model.getRepositories();
      assertEquals( repositories.size(), 1 );

      assertEquals( model.findRepository( ApplicationConfig.MAVEN_CENTRAL_NAME ), repositories.get( 0 ) );
      assertNull( model.findRepository( "other" ) );
    } );
  }

  @Test
  public void calculateConfigSha256()
  {
    // ensure sha of two separate instances equal if contents identical
    assertEquals( ApplicationModel.calculateConfigSha256( new ApplicationConfig() ),
                  ApplicationModel.calculateConfigSha256( new ApplicationConfig() ) );

    // ensure sha changes as more data added
    final ApplicationConfig config1 = new ApplicationConfig();
    final String shaA = ApplicationModel.calculateConfigSha256( config1 );
    assertEquals( shaA, "CA3D163BAB055381827226140568F3BEF7EAAC187CEBD76878E0B63E9E442356" );
    final OptionsConfig options = new OptionsConfig();
    config1.setOptions( options );
    final String shaB = ApplicationModel.calculateConfigSha256( config1 );
    assertEquals( shaB, "1473761ABB0E15C32E8D598B873750FAD3B57B737B7FF5C49A846EA921C80801" );
    options.setDefaultLanguage( Language.Java );
    final String shaC = ApplicationModel.calculateConfigSha256( config1 );
    assertEquals( shaC, "EEEABA82F5A2571624A4A9A618FABA203EF655CE4E26FE3247B0650BABC52ADB" );
  }
}
