package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class OptionsConfig
{
  public static final String DEFAULT_WORKSPACE_DIR = ".";
  public static final String DEFAULT_EXTENSION_FILE = "3rdparty/dependencies.bzl";
  public static final String DEFAULT_WORKSPACE_MACRO_NAME = "generate_workspace_rules";
  public static final String DEFAULT_TARGET_MACRO_NAME = "generate_targets";
  public static final String DEFAULT_NAME_PREFIX = "";
  @Nonnull
  private String workspaceDirectory = DEFAULT_WORKSPACE_DIR;
  @Nonnull
  private String extensionFile = DEFAULT_EXTENSION_FILE;
  @Nullable
  private String workspaceMacroName;
  @Nullable
  private String targetMacroName;
  @Nullable
  private String namePrefix;
  private boolean failOnInvalidPom = true;
  private boolean failOnMissingPom = true;
  private boolean emitDependencyGraph = true;
  private boolean includeSource = true;

  @Nonnull
  public String getWorkspaceDirectory()
  {
    return workspaceDirectory;
  }

  public void setWorkspaceDirectory( @Nonnull final String workspaceDirectory )
  {
    this.workspaceDirectory = workspaceDirectory;
  }

  @Nonnull
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

  public boolean isFailOnInvalidPom()
  {
    return failOnInvalidPom;
  }

  public void setFailOnInvalidPom( final boolean failOnInvalidPom )
  {
    this.failOnInvalidPom = failOnInvalidPom;
  }

  public boolean isFailOnMissingPom()
  {
    return failOnMissingPom;
  }

  public void setFailOnMissingPom( final boolean failOnMissingPom )
  {
    this.failOnMissingPom = failOnMissingPom;
  }

  public boolean isEmitDependencyGraph()
  {
    return emitDependencyGraph;
  }

  public void setEmitDependencyGraph( final boolean emitDependencyGraph )
  {
    this.emitDependencyGraph = emitDependencyGraph;
  }

  public boolean isIncludeSource()
  {
    return includeSource;
  }

  public void setIncludeSource( final boolean includeSource )
  {
    this.includeSource = includeSource;
  }
}
