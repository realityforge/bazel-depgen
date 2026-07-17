package org.realityforge.bazel.depgen.model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.config.GlobalJ2clConfig;
import org.realityforge.bazel.depgen.config.GlobalJavaConfig;
import org.realityforge.bazel.depgen.config.JspecifyMode;
import org.realityforge.bazel.depgen.config.NameStrategy;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.realityforge.bazel.depgen.config.RepositoryRuleGenerationStrategy;
import org.realityforge.bazel.depgen.config.TargetGenerationStrategy;
import org.realityforge.bazel.depgen.util.BazelUtil;

public final class OptionsModel {
    private static final List<String> SUPPORTED_REPOSITORY_RULE_LOAD_SYMBOLS = List.of("http_file");

    private static final List<String> SUPPORTED_TARGET_RULE_LOAD_SYMBOLS =
            Arrays.asList("java_binary", "java_import", "java_library", "java_plugin", "java_test", "j2cl_library");

    private final OptionsConfig _source;

    private final Path _workspaceDirectory;

    private final Path _extensionFile;

    /**
     * Create the OptionsModel from config.
     * All paths are relative to baseDirectory.
     *
     * @param configDirectory the directory that paths are relative to.
     * @param source          the original configuration source.
     */
    static OptionsModel parse(final Path configDirectory, final OptionsConfig source) {
        validateNamePrefix(source);
        validateRepositoryRuleGenerationStrategy(source);
        validateTargetGenerationStrategy(source);
        validateRepositoryRuleLoadSymbols(source);
        validateTargetRuleLoadSymbols(source);
        final Path workspaceDirectory = deriveWorkspaceDirectory(configDirectory, source);
        final Path extensionFile = deriveExtensionFile(configDirectory, source);
        return new OptionsModel(source, workspaceDirectory, extensionFile);
    }

    private static void validateRepositoryRuleGenerationStrategy(final OptionsConfig source) {
        final String value = source.getRepositoryRuleGenerationStrategy();
        if (null != value && null == RepositoryRuleGenerationStrategy.findById(value)) {
            throw new InvalidModelException(
                    "The options.repositoryRuleGenerationStrategy property must be one of: extensionFile, module.",
                    source);
        }
    }

    private static void validateTargetGenerationStrategy(final OptionsConfig source) {
        final String value = source.getTargetGenerationStrategy();
        if (null != value && null == TargetGenerationStrategy.findById(value)) {
            throw new InvalidModelException(
                    "The options.targetGenerationStrategy property must be one of: extensionFile, build.", source);
        }
    }

    private static void validateRepositoryRuleLoadSymbols(final OptionsConfig source) {
        validateLoadSymbols(
                source,
                source.getRepositoryRuleLoadSymbols(),
                SUPPORTED_REPOSITORY_RULE_LOAD_SYMBOLS,
                "options.repositoryRuleLoadSymbols");
    }

    private static void validateTargetRuleLoadSymbols(final OptionsConfig source) {
        validateLoadSymbols(
                source,
                source.getTargetRuleLoadSymbols(),
                SUPPORTED_TARGET_RULE_LOAD_SYMBOLS,
                "options.targetRuleLoadSymbols");
    }

    private static void validateLoadSymbols(
            final OptionsConfig source,
            @Nullable final Map<String, Boolean> loadSymbols,
            final List<String> supportedSymbols,
            final String propertyName) {
        if (null != loadSymbols) {
            for (final Map.Entry<String, Boolean> entry : loadSymbols.entrySet()) {
                final String symbol = entry.getKey();
                if (!supportedSymbols.contains(symbol)) {
                    throw new InvalidModelException(
                            "The " + propertyName + " property contains unsupported key '" + symbol
                                    + "'. Supported keys are: " + String.join(", ", supportedSymbols) + ".",
                            source);
                }
                if (null == entry.getValue()) {
                    throw new InvalidModelException(
                            "The " + propertyName + " property contains null value for key '" + symbol
                                    + "'. Values must be true or false.",
                            source);
                }
            }
        }
    }

    private static void validateNamePrefix(final OptionsConfig source) {
        final String namePrefix = source.getNamePrefix();
        if (null != namePrefix && namePrefix.contains(BazelUtil.COMPONENT_SEPARATOR)) {
            throw new InvalidModelException(
                    "The options.namePrefix property must not contain '" + BazelUtil.COMPONENT_SEPARATOR + "'.",
                    source);
        }
    }

    private static Path deriveWorkspaceDirectory(final Path configDirectory, final OptionsConfig source) {
        final String value = source.getWorkspaceDirectory();
        final String filename = null == value ? OptionsConfig.DEFAULT_WORKSPACE_DIR : value;
        return configDirectory.resolve(filename).toAbsolutePath().normalize();
    }

    private static Path deriveExtensionFile(final Path configDirectory, final OptionsConfig source) {
        final String value = source.getExtensionFile();
        final String filename = null == value ? OptionsConfig.DEFAULT_EXTENSION_FILE : value;
        return configDirectory.resolve(filename).toAbsolutePath().normalize();
    }

    private OptionsModel(final OptionsConfig source, final Path workspaceDirectory, final Path extensionFile) {
        _source = Objects.requireNonNull(source);
        _workspaceDirectory = Objects.requireNonNull(workspaceDirectory);
        _extensionFile = Objects.requireNonNull(extensionFile);
    }

    public OptionsConfig getSource() {
        return _source;
    }

    public Path getWorkspaceDirectory() {
        return _workspaceDirectory;
    }

    public Path getExtensionFile() {
        return _extensionFile;
    }

    public Path getModuleFile() {
        return getWorkspaceDirectory().resolve("MODULE.bazel");
    }

    public String getWorkspaceMacroName() {
        final String workspaceMacroName = _source.getWorkspaceMacroName();
        return null == workspaceMacroName
                ? getNamePrefix() + OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME
                : workspaceMacroName;
    }

    public String getTargetMacroName() {
        final String targetMacroName = _source.getTargetMacroName();
        return null == targetMacroName ? getNamePrefix() + OptionsConfig.DEFAULT_TARGET_MACRO_NAME : targetMacroName;
    }

    public RepositoryRuleGenerationStrategy getRepositoryRuleGenerationStrategy() {
        final String value = _source.getRepositoryRuleGenerationStrategy();
        return null == value
                ? OptionsConfig.DEFAULT_REPOSITORY_RULE_GENERATION_STRATEGY
                : Objects.requireNonNull(RepositoryRuleGenerationStrategy.findById(value));
    }

    public TargetGenerationStrategy getTargetGenerationStrategy() {
        final String value = _source.getTargetGenerationStrategy();
        return null == value
                ? OptionsConfig.DEFAULT_TARGET_GENERATION_STRATEGY
                : Objects.requireNonNull(TargetGenerationStrategy.findById(value));
    }

    public boolean isRepositoryRuleGenerationInExtensionFile() {
        return RepositoryRuleGenerationStrategy.ExtensionFile == getRepositoryRuleGenerationStrategy();
    }

    public boolean isTargetGenerationInExtensionFile() {
        return TargetGenerationStrategy.ExtensionFile == getTargetGenerationStrategy();
    }

    public boolean isRepositoryRuleLoadSymbolsConfigured() {
        return null != _source.getRepositoryRuleLoadSymbols();
    }

    public boolean isTargetRuleLoadSymbolsConfigured() {
        return null != _source.getTargetRuleLoadSymbols();
    }

    public boolean shouldEmitRepositoryRuleLoadSymbol(final String symbol) {
        return shouldEmitLoadSymbol(_source.getRepositoryRuleLoadSymbols(), symbol);
    }

    public boolean shouldEmitTargetRuleLoadSymbol(final String symbol) {
        return shouldEmitLoadSymbol(_source.getTargetRuleLoadSymbols(), symbol);
    }

    private static boolean shouldEmitLoadSymbol(@Nullable final Map<String, Boolean> loadSymbols, final String symbol) {
        final Boolean value = null == loadSymbols ? null : loadSymbols.get(symbol);
        return null == value || value;
    }

    public boolean requiresExtensionFile() {
        return isRepositoryRuleGenerationInExtensionFile() || isTargetGenerationInExtensionFile();
    }

    public String getRepositoryRuleStartToken() {
        final String value = _source.getRepositoryRuleStartToken();
        return null == value ? OptionsConfig.DEFAULT_REPOSITORY_RULE_START_TOKEN : value;
    }

    public String getRepositoryRuleEndToken() {
        final String value = _source.getRepositoryRuleEndToken();
        return null == value ? OptionsConfig.DEFAULT_REPOSITORY_RULE_END_TOKEN : value;
    }

    public String getTargetStartToken() {
        final String value = _source.getTargetStartToken();
        return null == value ? OptionsConfig.DEFAULT_TARGET_START_TOKEN : value;
    }

    public String getTargetEndToken() {
        final String value = _source.getTargetEndToken();
        return null == value ? OptionsConfig.DEFAULT_TARGET_END_TOKEN : value;
    }

    public String getNamePrefix() {
        // Name prefix if non-null and non-empty should be suffixed with '_'
        final String namePrefix = _source.getNamePrefix();
        return null == namePrefix
                ? OptionsConfig.DEFAULT_NAME_PREFIX
                : (namePrefix.isEmpty() ? "" : (namePrefix.endsWith("_") ? namePrefix : namePrefix + "_"));
    }

    public NameStrategy getNameStrategy() {
        final NameStrategy strategy = _source.getNameStrategy();
        return null == strategy ? OptionsConfig.DEFAULT_NAME_STRATEGY : strategy;
    }

    public NameStrategy getRepositoryNameStrategy() {
        final NameStrategy strategy = _source.getRepositoryNameStrategy();
        return null == strategy ? OptionsConfig.DEFAULT_REPOSITORY_NAME_STRATEGY : strategy;
    }

    public Nature getDefaultNature() {
        final Nature nature = _source.getDefaultNature();
        return null == nature ? OptionsConfig.DEFAULT_NATURE : nature;
    }

    public boolean failOnInvalidPom() {
        final Boolean flag = _source.getFailOnInvalidPom();
        return null == flag ? OptionsConfig.DEFAULT_FAIL_ON_INVALID_POM : flag;
    }

    public boolean failOnMissingPom() {
        final Boolean flag = _source.getFailOnMissingPom();
        return null == flag ? OptionsConfig.DEFAULT_FAIL_ON_MISSING_POM : flag;
    }

    public boolean emitDependencyGraph() {
        final Boolean flag = _source.getEmitDependencyGraph();
        return null == flag ? OptionsConfig.DEFAULT_EMIT_DEPENDENCY_GRAPH : flag;
    }

    public boolean includeSource() {
        final Boolean flag = _source.getIncludeSource();
        return null == flag ? OptionsConfig.DEFAULT_INCLUDE_SOURCE : flag;
    }

    public boolean includeExternalAnnotations() {
        final Boolean flag = _source.getIncludeExternalAnnotations();
        return null == flag ? OptionsConfig.DEFAULT_INCLUDE_EXTERNAL_ANNOTATIONS : flag;
    }

    public boolean exportDeps() {
        final GlobalJavaConfig java = _source.getJava();
        final Boolean flag = null != java ? java.getExportDeps() : null;
        return null == flag ? OptionsConfig.DEFAULT_EXPORT_DEPS : flag;
    }

    public JspecifyMode jspecifyMode() {
        final GlobalJ2clConfig j2cl = _source.getJ2cl();
        final JspecifyMode mode = null != j2cl ? j2cl.getJspecifyMode() : null;
        return null == mode ? OptionsConfig.DEFAULT_JSPECIFY_MODE : mode;
    }

    public boolean supportDependencyOmit() {
        final Boolean flag = _source.getSupportDependencyOmit();
        return null == flag ? OptionsConfig.DEFAULT_SUPPORT_DEPENDENCY_OMIT : flag;
    }

    public boolean verifyConfigSha256() {
        final Boolean flag = _source.getVerifyConfigSha256();
        return null == flag ? OptionsConfig.DEFAULT_VERIFY_CONFIG_SHA256 : flag;
    }
}
