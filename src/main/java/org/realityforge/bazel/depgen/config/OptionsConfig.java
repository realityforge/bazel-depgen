package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;

public class OptionsConfig
{
  public static final String DEFAULT_WORKSPACE_DIR = ".";
  public static final String DEFAULT_EXTENSION_FILE = "3rdparty/dependencies.bzl";
  public static final String DEFAULT_GENERATE_RULES_MACRO_NAME = "generate_workspace_rules";
  @Nonnull
  private String workspaceDirectory = DEFAULT_WORKSPACE_DIR;
  @Nonnull
  private String extensionFile = DEFAULT_EXTENSION_FILE;
  @Nonnull
  private String generateRulesMacroName = DEFAULT_GENERATE_RULES_MACRO_NAME;
  private boolean failOnInvalidPom = true;
  private boolean failOnMissingPom = true;
  private boolean emitDependencyGraph = true;

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
  public String getGenerateRulesMacroName()
  {
    return generateRulesMacroName;
  }

  public void setGenerateRulesMacroName( @Nonnull final String generateRulesMacroName )
  {
    this.generateRulesMacroName = Objects.requireNonNull( generateRulesMacroName );
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
}
