package org.realityforge.bazel.depgen.model;

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.AliasStrategy;
import org.realityforge.bazel.depgen.config.GlobalJavaConfig;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.OptionsConfig;

public final class OptionsModel
{
  @Nonnull
  private final OptionsConfig _source;
  @Nonnull
  private final Path _workspaceDirectory;
  @Nonnull
  private final Path _extensionFile;

  /**
   * Create the OptionsModel from config.
   * All paths are relative to baseDirectory.
   *
   * @param configDirectory the directory that paths are relative to.
   * @param source          the original configuration source.
   */
  @Nonnull
  static OptionsModel parse( @Nonnull final Path configDirectory, @Nonnull final OptionsConfig source )
  {
    final Path workspaceDirectory = deriveWorkspaceDirectory( configDirectory, source );
    final Path extensionFile = deriveExtensionFile( configDirectory, source );
    return new OptionsModel( source, workspaceDirectory, extensionFile );
  }

  @Nonnull
  private static Path deriveWorkspaceDirectory( @Nonnull final Path configDirectory,
                                                @Nonnull final OptionsConfig source )
  {
    final String value = source.getWorkspaceDirectory();
    final String filename = null == value ? OptionsConfig.DEFAULT_WORKSPACE_DIR : value;
    return configDirectory.resolve( filename ).toAbsolutePath().normalize();
  }

  @Nonnull
  private static Path deriveExtensionFile( @Nonnull final Path configDirectory, @Nonnull final OptionsConfig source )
  {
    final String value = source.getExtensionFile();
    final String filename = null == value ? OptionsConfig.DEFAULT_EXTENSION_FILE : value;
    return configDirectory.resolve( filename ).toAbsolutePath().normalize();
  }

  private OptionsModel( @Nonnull final OptionsConfig source,
                        @Nonnull final Path workspaceDirectory,
                        @Nonnull final Path extensionFile )
  {
    _source = Objects.requireNonNull( source );
    _workspaceDirectory = Objects.requireNonNull( workspaceDirectory );
    _extensionFile = Objects.requireNonNull( extensionFile );
  }

  @Nonnull
  public OptionsConfig getSource()
  {
    return _source;
  }

  @Nonnull
  public Path getWorkspaceDirectory()
  {
    return _workspaceDirectory;
  }

  @Nonnull
  public Path getExtensionFile()
  {
    return _extensionFile;
  }

  @Nonnull
  public String getWorkspaceMacroName()
  {
    final String workspaceMacroName = _source.getWorkspaceMacroName();
    return null == workspaceMacroName ?
           getNamePrefix() + OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME :
           workspaceMacroName;
  }

  @Nonnull
  public String getTargetMacroName()
  {
    final String targetMacroName = _source.getTargetMacroName();
    return null == targetMacroName ? getNamePrefix() + OptionsConfig.DEFAULT_TARGET_MACRO_NAME : targetMacroName;
  }

  @Nonnull
  public String getNamePrefix()
  {
    //Name prefix if non-null and non-empty should be suffixed with '_'
    final String namePrefix = _source.getNamePrefix();
    return null == namePrefix ? OptionsConfig.DEFAULT_NAME_PREFIX :
           ( namePrefix.isEmpty() ? "" :
             ( namePrefix.endsWith( "_" ) ? namePrefix : namePrefix + "_" ) );
  }

  @Nonnull
  public AliasStrategy getAliasStrategy()
  {
    final AliasStrategy strategy = _source.getAliasStrategy();
    return null == strategy ? OptionsConfig.DEFAULT_ALIAS_STRATEGY : strategy;
  }

  @Nonnull
  public Nature getDefaultNature()
  {
    final Nature nature = _source.getDefaultNature();
    return null == nature ? OptionsConfig.DEFAULT_NATURE : nature;
  }

  public boolean failOnInvalidPom()
  {
    final Boolean flag = _source.getFailOnInvalidPom();
    return null == flag ? OptionsConfig.DEFAULT_FAIL_ON_INVALID_POM : flag;
  }

  public boolean failOnMissingPom()
  {
    final Boolean flag = _source.getFailOnMissingPom();
    return null == flag ? OptionsConfig.DEFAULT_FAIL_ON_MISSING_POM : flag;
  }

  public boolean emitDependencyGraph()
  {
    final Boolean flag = _source.getEmitDependencyGraph();
    return null == flag ? OptionsConfig.DEFAULT_EMIT_DEPENDENCY_GRAPH : flag;
  }

  public boolean includeSource()
  {
    final Boolean flag = _source.getIncludeSource();
    return null == flag ? OptionsConfig.DEFAULT_INCLUDE_SOURCE : flag;
  }

  public boolean includeExternalAnnotations()
  {
    final Boolean flag = _source.getIncludeExternalAnnotations();
    return null == flag ? OptionsConfig.DEFAULT_INCLUDE_EXTERNAL_ANNOTATIONS : flag;
  }

  public boolean exportDeps()
  {
    final GlobalJavaConfig java = _source.getJava();
    final Boolean flag = null != java ? java.getExportDeps() : null;
    return null == flag ? OptionsConfig.DEFAULT_EXPORT_DEPS : flag;
  }

  public boolean supportDependencyOmit()
  {
    final Boolean flag = _source.getSupportDependencyOmit();
    return null == flag ? OptionsConfig.DEFAULT_SUPPORT_DEPENDENCY_OMIT : flag;
  }

  public boolean verifyConfigSha256()
  {
    final Boolean flag = _source.getVerifyConfigSha256();
    return null == flag ? OptionsConfig.DEFAULT_VERIFY_CONFIG_SHA256 : flag;
  }
}
