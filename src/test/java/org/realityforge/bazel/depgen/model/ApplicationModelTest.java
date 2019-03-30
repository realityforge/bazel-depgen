package org.realityforge.bazel.depgen.model;

import java.util.Collections;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.config.OptionsConfig;
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
    assertTrue( model.getReplacements().isEmpty() );
  }
}
