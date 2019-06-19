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
    assertEquals( model.getConfigSha256(), "121141B72422F4A4487D9D13E43F920BA5DE1A8837DDDA78204AD3D74B5DD147" );
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
    assertEquals( model.getArtifacts().size(), 1 );
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
    assertEquals( shaA, "CA3D163BAB055381827226140568F3BEF7EAAC187CEBD76878E0B63E9E442356" );
    final OptionsConfig options = new OptionsConfig();
    config1.setOptions( options );
    final String shaB = ApplicationModel.calculateConfigSha256( config1 );
    assertEquals( shaB, "1473761ABB0E15C32E8D598B873750FAD3B57B737B7FF5C49A846EA921C80801" );
    options.setDefaultNature( Nature.Java );
    final String shaC = ApplicationModel.calculateConfigSha256( config1 );
    assertEquals( shaC, "C60262623878C465934DD29FBEF0EA8C7C0FD4CA29ED6F87177D2672BE75AC59" );
  }
}
