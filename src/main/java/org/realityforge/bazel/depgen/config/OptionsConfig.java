package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;

public class OptionsConfig
{
  public static final String DEFAULT_WORKSPACE_DIR = ".";
  public static final String DEFAULT_EXTENSION_FILE = "3rdparty/dependencies.bzl";
  public static final String DEFAULT_WORKSPACE_MACRO_NAME = "generate_workspace_rules";
  public static final String DEFAULT_NAME_PREFIX = "";
  @Nonnull
  private String workspaceDirectory = DEFAULT_WORKSPACE_DIR;
  @Nonnull
  private String extensionFile = DEFAULT_EXTENSION_FILE;
  @Nonnull
  private String workspaceMacroName = DEFAULT_WORKSPACE_MACRO_NAME;
  @Nonnull
  private String namePrefix = DEFAULT_NAME_PREFIX;
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

  @Nonnull
  public String getWorkspaceMacroName()
  {
    return workspaceMacroName;
  }

  public void setWorkspaceMacroName( @Nonnull final String workspaceMacroName )
  {
    this.workspaceMacroName = Objects.requireNonNull( workspaceMacroName );
  }

  @Nonnull
  public String getNamePrefix()
  {
    return namePrefix;
  }

  public void setNamePrefix( @Nonnull final String namePrefix )
  {
    this.namePrefix = namePrefix;
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
