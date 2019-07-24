package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class OptionsConfig
{
  public static final String DEFAULT_WORKSPACE_DIR = "..";
  public static final String DEFAULT_EXTENSION_FILE = "dependencies.bzl";
  public static final String DEFAULT_WORKSPACE_MACRO_NAME = "generate_workspace_rules";
  public static final String DEFAULT_TARGET_MACRO_NAME = "generate_targets";
  public static final String DEFAULT_NAME_PREFIX = "";
  public static final AliasStrategy DEFAULT_ALIAS_STRATEGY = AliasStrategy.GroupIdAndArtifactId;
  public static final boolean DEFAULT_FAIL_ON_INVALID_POM = true;
  public static final boolean DEFAULT_FAIL_ON_MISSING_POM = true;
  public static final boolean DEFAULT_EMIT_DEPENDENCY_GRAPH = true;
  public static final boolean DEFAULT_INCLUDE_SOURCE = true;
  public static final boolean DEFAULT_INCLUDE_EXTERNAL_ANNOTATIONS = false;
  public static final boolean DEFAULT_EXPORT_DEPS = false;
  public static final boolean DEFAULT_SUPPORT_DEPENDENCY_OMIT = false;
  public static final boolean DEFAULT_VERIFY_CONFIG_SHA256 = true;
  public static final Nature DEFAULT_NATURE = Nature.Java;
  @Nullable
  private String workspaceDirectory;
  @Nullable
  private String extensionFile;
  @Nullable
  private String workspaceMacroName;
  @Nullable
  private String targetMacroName;
  @Nullable
  private String namePrefix;
  @Nullable
  private AliasStrategy aliasStrategy;
  @Nullable
  private Nature nature;
  @Nullable
  private Boolean failOnInvalidPom;
  @Nullable
  private Boolean failOnMissingPom;
  @Nullable
  private Boolean emitDependencyGraph;
  @Nullable
  private Boolean includeSource;
  @Nullable
  private Boolean includeExternalAnnotations;
  @Nullable
  private Boolean exportDeps;
  @Nullable
  private Boolean supportDependencyOmit;
  @Nullable
  private Boolean verifyConfigSha256;

  @Nullable
  public String getWorkspaceDirectory()
  {
    return workspaceDirectory;
  }

  public void setWorkspaceDirectory( @Nonnull final String workspaceDirectory )
  {
    this.workspaceDirectory = workspaceDirectory;
  }

  @Nullable
  public String getExtensionFile()
  {
    return extensionFile;
  }

  public void setExtensionFile( @Nonnull final String extensionFile )
  {
    this.extensionFile = Objects.requireNonNull( extensionFile );
  }

  @Nullable
  public String getWorkspaceMacroName()
  {
    return workspaceMacroName;
  }

  public void setWorkspaceMacroName( @Nonnull final String workspaceMacroName )
  {
    this.workspaceMacroName = Objects.requireNonNull( workspaceMacroName );
  }

  @Nullable
  public String getTargetMacroName()
  {
    return targetMacroName;
  }

  public void setTargetMacroName( @Nonnull final String targetMacroName )
  {
    this.targetMacroName = Objects.requireNonNull( targetMacroName );
  }

  @Nullable
  public String getNamePrefix()
  {
    return namePrefix;
  }

  public void setNamePrefix( @Nonnull final String namePrefix )
  {
    this.namePrefix = Objects.requireNonNull( namePrefix );
  }

  @Nullable
  public AliasStrategy getAliasStrategy()
  {
    return aliasStrategy;
  }

  public void setAliasStrategy( @Nonnull final AliasStrategy aliasStrategy )
  {
    this.aliasStrategy = Objects.requireNonNull( aliasStrategy );
  }

  @Nullable
  public Nature getNature()
  {
    return nature;
  }

  public void setNature( @Nonnull final Nature nature )
  {
    this.nature = Objects.requireNonNull( nature );
  }

  @Nullable
  public Boolean getFailOnInvalidPom()
  {
    return failOnInvalidPom;
  }

  public void setFailOnInvalidPom( @Nonnull final Boolean failOnInvalidPom )
  {
    this.failOnInvalidPom = Objects.requireNonNull( failOnInvalidPom );
  }

  @Nullable
  public Boolean getFailOnMissingPom()
  {
    return failOnMissingPom;
  }

  public void setFailOnMissingPom( @Nonnull final Boolean failOnMissingPom )
  {
    this.failOnMissingPom = Objects.requireNonNull( failOnMissingPom );
  }

  @Nullable
  public Boolean getEmitDependencyGraph()
  {
    return emitDependencyGraph;
  }

  public void setEmitDependencyGraph( @Nonnull final Boolean emitDependencyGraph )
  {
    this.emitDependencyGraph = Objects.requireNonNull( emitDependencyGraph );
  }

  @Nullable
  public Boolean getIncludeSource()
  {
    return includeSource;
  }

  public void setIncludeSource( @Nonnull final Boolean includeSource )
  {
    this.includeSource = Objects.requireNonNull( includeSource );
  }

  @Nullable
  public Boolean getIncludeExternalAnnotations()
  {
    return includeExternalAnnotations;
  }

  public void setIncludeExternalAnnotations( @Nonnull final Boolean includeExternalAnnotations )
  {
    this.includeExternalAnnotations = Objects.requireNonNull( includeExternalAnnotations );
  }

  @Nullable
  public Boolean getExportDeps()
  {
    return exportDeps;
  }

  public void setExportDeps( @Nonnull final Boolean exportDeps )
  {
    this.exportDeps = Objects.requireNonNull( exportDeps );
  }

  @Nullable
  public Boolean getSupportDependencyOmit()
  {
    return supportDependencyOmit;
  }

  public void setSupportDependencyOmit( @Nonnull final Boolean supportDependencyOmit )
  {
    this.supportDependencyOmit = Objects.requireNonNull( supportDependencyOmit );
  }

  @Nullable
  public Boolean getVerifyConfigSha256()
  {
    return verifyConfigSha256;
  }

  public void setVerifyConfigSha256( @Nonnull final Boolean verifyConfigSha256 )
  {
    this.verifyConfigSha256 = Objects.requireNonNull( verifyConfigSha256 );
  }
}
