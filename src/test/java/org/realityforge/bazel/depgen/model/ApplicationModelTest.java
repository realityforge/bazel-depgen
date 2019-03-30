package org.realityforge.bazel.depgen.model;

import java.util.Collections;
import java.util.List;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.realityforge.bazel.depgen.config.ReplacementConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationModelTest
  extends AbstractTest
{
  @Test
  public void parseWithDefaults()
  {
    final ApplicationConfig source = new ApplicationConfig();
    final ArtifactConfig artifactConfig = new ArtifactConfig();
    artifactConfig.setCoord( "com.example:myapp:1.0" );
    source.setArtifacts( Collections.singletonList( artifactConfig ) );

    final ReplacementConfig replacement = new ReplacementConfig();
    replacement.setCoord( "com.example:alib" );
    replacement.setTarget( "@com_example//:alib" );
    source.setReplacements( Collections.singletonList( replacement ) );

    final ApplicationModel model = ApplicationModel.parse( source );
    assertEquals( model.getSource(), source );
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
  }
}
