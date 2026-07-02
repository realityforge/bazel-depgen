package org.realityforge.bazel.depgen.config;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;

public class ApplicationConfigTest extends AbstractTest {
    @Test
    public void parseEmpty() throws Exception {
        writeConfigFile("");
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        assertEquals(config.getConfigLocation(), getDefaultConfigFile());
    }

    @Test
    public void parseRepositories() throws Exception {
        writeConfigFile("""
            repositories:
              - name: central
                url: http://repo1.maven.org/maven2
              - url: https://example.com/repo
                cacheLookups: false
                searchByDefault: false
                checksumPolicy: warn
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        assertEquals(config.getConfigLocation(), getDefaultConfigFile());
        final List<RepositoryConfig> repositories = requireNonNull(config.getRepositories());

        assertEquals(repositories.size(), 2);
        final RepositoryConfig repository1 = repositories.get(0);
        assertEquals(repository1.getName(), "central");
        assertEquals(repository1.getUrl(), "http://repo1.maven.org/maven2");
        assertNull(repository1.getCacheLookups());
        assertNull(repository1.getSearchByDefault());
        assertNull(repository1.getChecksumPolicy());
        final RepositoryConfig repository2 = repositories.get(1);
        assertNull(repository2.getName());
        assertEquals(repository2.getUrl(), "https://example.com/repo");
        assertEquals(repository2.getCacheLookups(), Boolean.FALSE);
        assertEquals(repository2.getSearchByDefault(), Boolean.FALSE);
        assertEquals(repository2.getChecksumPolicy(), ChecksumPolicy.warn);
    }

    @Test
    public void parseConfigWithComments() throws Exception {
        writeConfigFile("""
            # Configuration comment
            repositories:
              # Repository comment
              - name: central
                url: http://repo1.maven.org/maven2
            """);
        final ApplicationConfig config = loadApplicationConfig();
        final List<RepositoryConfig> repositories = requireNonNull(config.getRepositories());

        assertEquals(repositories.size(), 1);
        final RepositoryConfig repository = repositories.get(0);
        assertEquals(repository.getName(), "central");
        assertEquals(repository.getUrl(), "http://repo1.maven.org/maven2");
    }

    @Test
    public void parseDependencyWithCoords() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
    }

    @Test
    public void parseDependencyWithExcludes() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
                excludes: ['org.realityforge.javax.annotation:javax.annotation',\
             'org.realityforge.braincheck']
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
        final List<String> excludes = requireNonNull(artifact.getExcludes());
        assertEquals(excludes.size(), 2);

        assertTrue(excludes.contains("org.realityforge.javax.annotation:javax.annotation"));
        assertTrue(excludes.contains("org.realityforge.braincheck"));
    }

    @Test
    public void parseDependencyWithRepositories() throws Exception {
        writeConfigFile("""
            repositories:
              - name: central
                url: https://repo1.maven.org/maven2
              - name: example
                url: https://repo1.example.com/maven2
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
                repositories: ['example']
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
        assertEquals(artifact.getRepositories(), Collections.singletonList("example"));
    }

    @Test
    public void parseDependencyWithVisibility() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
                visibility: ['//some/package:__pkg__', '//other/package:__subpackages__']
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
        final List<String> visibility = requireNonNull(artifact.getVisibility());
        assertEquals(visibility.size(), 2);

        assertTrue(visibility.contains("//some/package:__pkg__"));
        assertTrue(visibility.contains("//other/package:__subpackages__"));
    }

    @Test
    public void parseDependencyWithNatures() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
                natures: [J2cl]
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
        final List<Nature> natures = artifact.getNatures();
        assertNotNull(natures);
        assertEquals(natures, Collections.singletonList(Nature.J2cl));
    }

    @Test
    public void parseWithJ2clConfig() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.arez:arez-core:0.138
                natures: [J2cl]
                j2cl:
                  suppress: ['checkDebuggerStatement','other']
                  mode: Library
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.arez:arez-core:0.138");
        final List<Nature> natures = artifact.getNatures();
        assertNotNull(natures);
        assertEquals(natures, Collections.singletonList(Nature.J2cl));
        final J2clConfig j2cl = requireNonNull(artifact.getJ2cl());
        assertEquals(j2cl.getSuppress(), Arrays.asList("checkDebuggerStatement", "other"));
        assertEquals(j2cl.getMode(), J2clMode.Library);
    }

    @Test
    public void parseWithoutJ2clConfig() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.arez:arez-core:0.138
                natures: [J2cl]
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.arez:arez-core:0.138");
        final List<Nature> natures = artifact.getNatures();
        assertNotNull(natures);
        assertEquals(natures, Collections.singletonList(Nature.J2cl));
        assertNull(artifact.getJ2cl());
    }

    @Test
    public void artifactWithNameStrategy() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.arez:arez-core:0.138
                nameStrategy: ArtifactId
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.arez:arez-core:0.138");
        assertEquals(artifact.getNameStrategy(), NameStrategy.ArtifactId);
    }

    @Test
    public void artifactWithoutNameStrategy() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.arez:arez-core:0.138
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.arez:arez-core:0.138");
        assertNull(artifact.getNameStrategy());
    }

    @Test
    public void artifactWithRepositoryNamingConfig() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.arez:arez-core:0.138
                repositoryNameStrategy: ArtifactId
                repositoryName: arez_core
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.arez:arez-core:0.138");
        assertEquals(artifact.getRepositoryNameStrategy(), NameStrategy.ArtifactId);
        assertEquals(artifact.getRepositoryName(), "arez_core");
    }

    @Test
    public void parseDependencyWithIncludeOptional() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
                includeOptional: true
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
        assertTrue(requireNonNull(artifact.getIncludeOptional()));
    }

    @Test
    public void parseDependencyWithExportDeps() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
                java:
                  exportDeps: true
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
        final JavaConfig java = requireNonNull(artifact.getJava());
        assertTrue(requireNonNull(java.getExportDeps()));
    }

    @Test
    public void generatesApi() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.realityforge.gir:gir-core:jar:sources:0.08
                plugin:
                  generatesApi: false
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);
        final ArtifactConfig artifact = ensureSingleArtifact(config);
        assertEquals(artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08");
        final PluginConfig plugin = requireNonNull(artifact.getPlugin());
        final Boolean generatesApi = requireNonNull(plugin.getGeneratesApi());
        assertFalse(generatesApi);
    }

    @Test
    public void parseOptions() throws Exception {
        writeConfigFile("""
            options:
              workspaceDirectory: workspaceDir
              failOnMissingPom: false
              failOnInvalidPom: false
              includeSource: false
              includeExternalAnnotations: true
              verifyConfigSha256: false
              supportDependencyOmit: true
              emitDependencyGraph: false
              workspaceMacroName: workspace_rules
              targetMacroName: gen_targets
              repositoryRuleGenerationStrategy: module
              targetGenerationStrategy: build
              repositoryRuleLoadSymbols:
                http_file: false
                http_archive: true
              targetRuleLoadSymbols:
                java_binary: true
                java_import: false
                j2cl_library: false
              repositoryRuleStartToken: '# rs'
              repositoryRuleEndToken: '# re'
              targetStartToken: '# ts'
              targetEndToken: '# te'
              namePrefix: magic_
              nameStrategy: ArtifactId
              repositoryNameStrategy: ArtifactId
              defaultNature: J2cl
              extensionFile: workspaceDir/vendor/workspace.bzl
              java:
                exportDeps: true
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);

        final OptionsConfig options = requireNonNull(config.getOptions());

        assertEquals(options.getWorkspaceDirectory(), "workspaceDir");
        assertEquals(options.getExtensionFile(), "workspaceDir/vendor/workspace.bzl");
        assertEquals(options.getWorkspaceMacroName(), "workspace_rules");
        assertEquals(options.getTargetMacroName(), "gen_targets");
        assertEquals(options.getRepositoryRuleGenerationStrategy(), "module");
        assertEquals(options.getTargetGenerationStrategy(), "build");
        assertEquals(
                options.getRepositoryRuleLoadSymbols(),
                Map.of("http_file", Boolean.FALSE, "http_archive", Boolean.TRUE));
        assertEquals(
                options.getTargetRuleLoadSymbols(),
                Map.of("java_binary", Boolean.TRUE, "java_import", Boolean.FALSE, "j2cl_library", Boolean.FALSE));
        assertEquals(options.getRepositoryRuleStartToken(), "# rs");
        assertEquals(options.getRepositoryRuleEndToken(), "# re");
        assertEquals(options.getTargetStartToken(), "# ts");
        assertEquals(options.getTargetEndToken(), "# te");
        assertEquals(options.getNamePrefix(), "magic_");
        assertEquals(options.getNameStrategy(), NameStrategy.ArtifactId);
        assertEquals(options.getRepositoryNameStrategy(), NameStrategy.ArtifactId);
        assertEquals(options.getDefaultNature(), Nature.J2cl);
        assertEquals(options.getFailOnMissingPom(), Boolean.FALSE);
        assertEquals(options.getFailOnInvalidPom(), Boolean.FALSE);
        assertEquals(options.getEmitDependencyGraph(), Boolean.FALSE);
        assertEquals(options.getIncludeSource(), Boolean.FALSE);
        assertEquals(options.getIncludeExternalAnnotations(), Boolean.TRUE);
        assertEquals(options.getSupportDependencyOmit(), Boolean.TRUE);
        assertEquals(options.getVerifyConfigSha256(), Boolean.FALSE);
        final GlobalJavaConfig java = requireNonNull(options.getJava());
        assertEquals(java.getExportDeps(), Boolean.TRUE);
    }

    @Test
    public void parseDefaultOptions() throws Exception {
        writeConfigFile("options: {}\n");
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);

        final OptionsConfig options = requireNonNull(config.getOptions());

        assertNull(options.getWorkspaceDirectory());
        assertNull(options.getExtensionFile());
        assertNull(options.getWorkspaceMacroName());
        assertNull(options.getTargetMacroName());
        assertNull(options.getRepositoryRuleGenerationStrategy());
        assertNull(options.getTargetGenerationStrategy());
        assertNull(options.getRepositoryRuleLoadSymbols());
        assertNull(options.getTargetRuleLoadSymbols());
        assertNull(options.getRepositoryRuleStartToken());
        assertNull(options.getRepositoryRuleEndToken());
        assertNull(options.getTargetStartToken());
        assertNull(options.getTargetEndToken());
        assertNull(options.getNamePrefix());
        assertNull(options.getNameStrategy());
        assertNull(options.getRepositoryNameStrategy());
        assertNull(options.getDefaultNature());
        assertNull(options.getFailOnMissingPom());
        assertNull(options.getFailOnInvalidPom());
        assertNull(options.getEmitDependencyGraph());
        assertNull(options.getIncludeSource());
        assertNull(options.getIncludeExternalAnnotations());
        assertNull(options.getSupportDependencyOmit());
        assertNull(options.getVerifyConfigSha256());
        assertNull(options.getJava());
    }

    @Test
    public void parseReplacementsDefined() throws Exception {
        writeConfigFile("""
            replacements:
              - coord: com.example:myapp
                targets:
                  - target: "@com_example//:myapp"
                    nature: Java
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);

        final List<ReplacementConfig> replacements = requireNonNull(config.getReplacements());

        assertEquals(replacements.size(), 1);
        final ReplacementConfig replacement = replacements.get(0);
        assertEquals(replacement.getCoord(), "com.example:myapp");
        final List<ReplacementTargetConfig> targets = requireNonNull(replacement.getTargets());
        final ReplacementTargetConfig target = targets.get(0);
        assertEquals(target.getTarget(), "@com_example//:myapp");
        assertEquals(target.getNature(), Nature.Java);
    }

    @Test
    public void parseWithNoConfiguration() throws Exception {
        writeConfigFile("");
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);

        assertNull(config.getOptions());
        assertNull(config.getRepositories());
        assertNull(config.getArtifacts());
        assertNull(config.getReplacements());
        assertNull(config.getExcludes());
    }

    @Test
    public void parseExcludesDefinedUsingCoord() throws Exception {
        writeConfigFile("""
            excludes:
              - coord: com.example:myapp
            """);
        final ApplicationConfig config = loadApplicationConfig();
        assertNotNull(config);

        final List<ExcludeConfig> excludes = requireNonNull(config.getExcludes());

        assertEquals(excludes.size(), 1);
        final ExcludeConfig exclude = excludes.get(0);
        assertEquals(exclude.getCoord(), "com.example:myapp");
    }

    private ArtifactConfig ensureSingleArtifact(final ApplicationConfig config) {
        final List<ArtifactConfig> artifacts = requireNonNull(config.getArtifacts());

        assertEquals(artifacts.size(), 1);
        final ArtifactConfig artifact = artifacts.get(0);
        assertNotNull(artifact);
        return artifact;
    }
}
