package org.realityforge.bazel.depgen.model;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.nio.file.Path;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.GlobalJavaConfig;
import org.realityforge.bazel.depgen.config.NameStrategy;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.realityforge.bazel.depgen.config.RepositoryRuleGenerationStrategy;
import org.realityforge.bazel.depgen.config.TargetGenerationStrategy;
import org.testng.annotations.Test;

public class OptionsModelTest extends AbstractTest {
    @Test
    public void parseWithDefaults() {
        final OptionsConfig source = new OptionsConfig();

        final OptionsModel model = OptionsModel.parse(FileUtil.getCurrentDirectory(), source);
        assertEquals(model.getSource(), source);
        assertEquals(
                model.getWorkspaceDirectory(),
                FileUtil.getCurrentDirectory().resolve("..").toAbsolutePath().normalize());
        assertEquals(
                model.getExtensionFile(), FileUtil.getCurrentDirectory().resolve(OptionsConfig.DEFAULT_EXTENSION_FILE));
        assertEquals(model.getWorkspaceMacroName(), OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME);
        assertEquals(model.getTargetMacroName(), OptionsConfig.DEFAULT_TARGET_MACRO_NAME);
        assertEquals(
                model.getRepositoryRuleGenerationStrategy(), OptionsConfig.DEFAULT_REPOSITORY_RULE_GENERATION_STRATEGY);
        assertEquals(model.getTargetGenerationStrategy(), OptionsConfig.DEFAULT_TARGET_GENERATION_STRATEGY);
        assertEquals(model.getRepositoryRuleStartToken(), OptionsConfig.DEFAULT_REPOSITORY_RULE_START_TOKEN);
        assertEquals(model.getRepositoryRuleEndToken(), OptionsConfig.DEFAULT_REPOSITORY_RULE_END_TOKEN);
        assertEquals(model.getTargetStartToken(), OptionsConfig.DEFAULT_TARGET_START_TOKEN);
        assertEquals(model.getTargetEndToken(), OptionsConfig.DEFAULT_TARGET_END_TOKEN);
        assertEquals(model.getNamePrefix(), OptionsConfig.DEFAULT_NAME_PREFIX);
        assertEquals(model.getNameStrategy(), OptionsConfig.DEFAULT_NAME_STRATEGY);
        assertEquals(model.getRepositoryNameStrategy(), OptionsConfig.DEFAULT_REPOSITORY_NAME_STRATEGY);
        assertEquals(model.getDefaultNature(), OptionsConfig.DEFAULT_NATURE);
        assertTrue(model.failOnMissingPom());
        assertTrue(model.failOnInvalidPom());
        assertTrue(model.emitDependencyGraph());
        assertTrue(model.includeSource());
        assertFalse(model.includeExternalAnnotations());
        assertFalse(model.exportDeps());
        assertFalse(model.supportDependencyOmit());
        assertTrue(model.verifyConfigSha256());
    }

    @Test
    public void parseWithNamePrefixSpecified() {
        final OptionsConfig source = new OptionsConfig();
        source.setNamePrefix("myprj");

        final OptionsModel model = OptionsModel.parse(FileUtil.getCurrentDirectory(), source);
        assertEquals(model.getWorkspaceMacroName(), "myprj_" + OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME);
        assertEquals(model.getTargetMacroName(), "myprj_" + OptionsConfig.DEFAULT_TARGET_MACRO_NAME);
        assertEquals(model.getNamePrefix(), "myprj_");
    }

    @Test
    public void parseWithInvalidNamePrefix() {
        final OptionsConfig source = new OptionsConfig();
        source.setNamePrefix("my__prj");

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> OptionsModel.parse(FileUtil.getCurrentDirectory(), source));
        assertEquals(exception.getMessage(), "The options.namePrefix property must not contain '__'.");
        assertEquals(exception.getModel(), source);
    }

    @Test
    public void parse() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final Path thirdpartyDir = dir.resolve("thirdparty");

        final OptionsConfig source = new OptionsConfig();
        source.setWorkspaceDirectory("..");
        source.setExtensionFile("dependencies.bzl");
        source.setWorkspaceMacroName("gen_myprj_dependency_rules");
        source.setTargetMacroName("gen_myprj_targets");
        source.setRepositoryRuleGenerationStrategy("module");
        source.setTargetGenerationStrategy("build");
        source.setRepositoryRuleStartToken("# rs");
        source.setRepositoryRuleEndToken("# re");
        source.setTargetStartToken("# ts");
        source.setTargetEndToken("# te");
        source.setNamePrefix("myprj_");
        source.setNameStrategy(NameStrategy.ArtifactId);
        source.setRepositoryNameStrategy(NameStrategy.ArtifactId);
        source.setDefaultNature(Nature.J2cl);
        source.setFailOnMissingPom(false);
        source.setFailOnInvalidPom(false);
        source.setEmitDependencyGraph(false);
        source.setIncludeSource(false);
        source.setIncludeExternalAnnotations(true);
        source.setSupportDependencyOmit(true);
        source.setVerifyConfigSha256(false);
        final GlobalJavaConfig java = new GlobalJavaConfig();
        java.setExportDeps(true);
        source.setJava(java);

        final OptionsModel model = OptionsModel.parse(thirdpartyDir, source);
        assertEquals(model.getSource(), source);
        assertEquals(model.getWorkspaceDirectory(), dir.normalize());
        assertEquals(model.getExtensionFile(), thirdpartyDir.resolve("dependencies.bzl"));
        assertEquals(model.getWorkspaceMacroName(), "gen_myprj_dependency_rules");
        assertEquals(model.getTargetMacroName(), "gen_myprj_targets");
        assertEquals(model.getRepositoryRuleGenerationStrategy(), RepositoryRuleGenerationStrategy.Module);
        assertEquals(model.getTargetGenerationStrategy(), TargetGenerationStrategy.Build);
        assertEquals(model.getRepositoryRuleStartToken(), "# rs");
        assertEquals(model.getRepositoryRuleEndToken(), "# re");
        assertEquals(model.getTargetStartToken(), "# ts");
        assertEquals(model.getTargetEndToken(), "# te");
        assertEquals(model.getNamePrefix(), "myprj_");
        assertEquals(model.getNameStrategy(), NameStrategy.ArtifactId);
        assertEquals(model.getRepositoryNameStrategy(), NameStrategy.ArtifactId);
        assertEquals(model.getDefaultNature(), Nature.J2cl);
        assertFalse(model.failOnMissingPom());
        assertFalse(model.failOnInvalidPom());
        assertFalse(model.includeSource());
        assertTrue(model.includeExternalAnnotations());
        assertTrue(model.exportDeps());
        assertTrue(model.supportDependencyOmit());
        assertFalse(model.verifyConfigSha256());
    }

    @Test
    public void parseWithInvalidRepositoryRuleGenerationStrategy() {
        final OptionsConfig source = new OptionsConfig();
        source.setRepositoryRuleGenerationStrategy("bogus");

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> OptionsModel.parse(FileUtil.getCurrentDirectory(), source));
        assertEquals(
                exception.getMessage(),
                "The options.repositoryRuleGenerationStrategy property must be one of: extensionFile, module.");
        assertEquals(exception.getModel(), source);
    }

    @Test
    public void parseWithInvalidTargetGenerationStrategy() {
        final OptionsConfig source = new OptionsConfig();
        source.setTargetGenerationStrategy("bogus");

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> OptionsModel.parse(FileUtil.getCurrentDirectory(), source));
        assertEquals(
                exception.getMessage(),
                "The options.targetGenerationStrategy property must be one of: extensionFile, build.");
        assertEquals(exception.getModel(), source);
    }
}
