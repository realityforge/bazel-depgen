package org.realityforge.bazel.depgen.model;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.List;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.Nature;
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
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "excludes:\n" +
                     "  - coord: com.example:blib\n" +
                     "replacements:\n" +
                     "  - coord: com.example:alib\n" +
                     "    targets:\n" +
                     "      - target: \"@com_example//:alib\"\n" );
    final Path configFile = getDefaultConfigFile();
    final ApplicationConfig source = ApplicationConfig.parse( configFile );

    final ApplicationModel model = ApplicationModel.parse( source, false );
    assertEquals( model.getSource(), source );
    assertFalse( model.shouldResetCachedMetadata() );
    assertEquals( model.getConfigSha256(), "91FA6F7CF42E0E65BAC6B0CA263E220439F61646AA95DA8D0BA085B0C141D8BC" );
    assertEquals( model.getConfigLocation(), configFile );
    final List<RepositoryModel> repositories = model.getRepositories();
    assertEquals( repositories.size(), 1 );
    final RepositoryModel repository = repositories.get( 0 );
    assertEquals( repository.getName(), ApplicationConfig.MAVEN_CENTRAL_NAME );
    assertEquals( repository.getUrl(), ApplicationConfig.MAVEN_CENTRAL_URL );
    assertTrue( repository.cacheLookups() );
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
    final List<ReplacementTargetModel> targets = replacementModel.getTargets();
    assertEquals( targets.size(), 1 );
    final ReplacementTargetModel replacementTarget = targets.get( 0 );
    assertEquals( replacementTarget.getNature(), OptionsConfig.DEFAULT_NATURE );
    assertEquals( replacementTarget.getTarget(), "@com_example//:alib" );

    final List<GlobalExcludeModel> excludes = model.getExcludes();
    assertEquals( excludes.size(), 1 );
    final GlobalExcludeModel excludeModel = excludes.get( 0 );
    assertEquals( excludeModel.getGroup(), "com.example" );
    assertEquals( excludeModel.getId(), "blib" );
  }

  @Test
  public void isExcluded()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "excludes:\n" +
                     "  - coord: com.example:blib\n" );
    final ApplicationModel model = loadApplicationModel();

    assertFalse( model.isExcluded( "com.example", "alib" ) );
    assertTrue( model.isExcluded( "com.example", "blib" ) );
  }

  @Test
  public void findArtifact()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final ApplicationModel model = loadApplicationModel();
    final ArtifactModel artifactModel = model.getArtifacts().get( 0 );
    assertEquals( artifactModel.toCoord(), "com.example:myapp:jar:1.0" );

    assertEquals( artifactModel, model.findArtifact( "com.example", "myapp" ) );
    assertNull( model.findArtifact( "com.example", "noexist" ) );
  }

  @Test
  public void findReplacement()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "replacements:\n" +
                     "  - coord: com.example:mylib\n" +
                     "    targets:\n" +
                     "      - target: \"@com_example//:mylib\"\n" );

    final ApplicationModel model = loadApplicationModel();
    assertEquals( model.getReplacements().size(), 1 );
    final ReplacementModel replacementModel = model.getReplacements().get( 0 );
    assertEquals( replacementModel.getGroup(), "com.example" );
    assertEquals( replacementModel.getId(), "mylib" );

    assertEquals( replacementModel, model.findReplacement( "com.example", "mylib" ) );
    assertNull( model.findArtifact( "com.example", "noexist" ) );
  }

  @Test
  public void findRepository()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    final ApplicationModel model = loadApplicationModel();
    final List<RepositoryModel> repositories = model.getRepositories();
    assertEquals( repositories.size(), 1 );

    assertEquals( model.findRepository( ApplicationConfig.MAVEN_CENTRAL_NAME ), repositories.get( 0 ) );
    assertNull( model.findRepository( "other" ) );
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
    assertEquals( shaA, "C63A169524CF58709A43361D23D8B9C0DBBB999461CA1500EB374D7A72C3334C" );
    final OptionsConfig options = new OptionsConfig();
    config1.setOptions( options );
    final String shaB = ApplicationModel.calculateConfigSha256( config1 );
    assertEquals( shaB, "EF5CF3CC89EAD37C90FC2CB27F3922F7D7CD7CB63180371E9F737AF7CA8E122E" );
    options.setDefaultNature( Nature.Java );
    final String shaC = ApplicationModel.calculateConfigSha256( config1 );
    assertEquals( shaC, "51547EE556C5AD9452C5F885ED9FC3772B39A66ED0A43152F77EB790127D20C3" );
  }
}
