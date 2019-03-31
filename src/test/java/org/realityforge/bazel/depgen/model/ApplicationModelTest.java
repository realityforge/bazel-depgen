package org.realityforge.bazel.depgen.model;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.List;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
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
                         "replacements:\n" +
                         "  - coord: com.example:alib\n" +
                         "    target: \"@com_example//:alib\"\n" );
      final Path configFile = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      final ApplicationConfig source = ApplicationConfig.parse( configFile );

      final ApplicationModel model = ApplicationModel.parse( source );
      assertEquals( model.getSource(), source );
      assertEquals( model.getConfigLocation(), configFile );
      assertEquals( model.getRepositories().get( ApplicationConfig.MAVEN_CENTRAL_ID ),
                    ApplicationConfig.MAVEN_CENTRAL_URL );
      assertEquals( model.getOptions().getWorkspaceDirectory(), OptionsConfig.DEFAULT_WORKSPACE_DIR );
      assertEquals( model.getOptions().getExtensionFile(), OptionsConfig.DEFAULT_EXTENSION_FILE );
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
    } );
  }
}
