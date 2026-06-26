package org.realityforge.bazel.depgen.record;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.AuthenticationContext;
import org.realityforge.bazel.depgen.DepGenConfig;
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
import org.realityforge.bazel.depgen.DepgenValidationException;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.metadata.RecordBuildCallback;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.OptionsModel;
import org.realityforge.bazel.depgen.model.ReplacementModel;
import org.realityforge.bazel.depgen.util.StarlarkOutput;

public final class ApplicationRecord
{
  @Nonnull
  private final ApplicationModel _source;
  @Nonnull
  private final DependencyNode _node;
  @Nonnull
  private final Map<String, ArtifactRecord> _artifacts = new HashMap<>();
  @Nonnull
  private final Map<String, AuthenticationContext> _authenticationContexts;

  @Nonnull
  public static ApplicationRecord build( @Nonnull final ApplicationModel model,
                                         @Nonnull final DependencyNode node,
                                         @Nonnull final List<AuthenticationContext> authenticationContexts,
                                         @Nonnull final RecordBuildCallback callback )
  {
    final ApplicationRecord record = new ApplicationRecord( model, node, authenticationContexts );
    node.accept( new DependencyCollector( record, callback ) );
    propagateNature( record, Nature.J2cl, Nature.J2cl );
    propagateNature( record, Nature.Plugin, Nature.Java );
    propagateNature( record, Nature.Java, Nature.Java );
    record.getArtifacts().forEach( ArtifactRecord::validate );
    record.validate();
    return record;
  }

  private void validate()
  {
    ensureEmittedTargetNamesAreUnique();
    ensureEmittedRepositoryNamesAreUnique();
    ensureDeclaredDepgenArtifactIsValid();
  }

  private void ensureDeclaredDepgenArtifactIsValid()
  {
    final ApplicationModel model = getSource();
    final OptionsModel options = model.getOptions();
    if ( options.verifyConfigSha256() )
    {
      final ArtifactModel artifact =
        model.findApplicationArtifact( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
      final ReplacementModel replacement =
        model.findReplacement( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
      final boolean javaProvidedByArtifact =
        null == artifact || artifact.getNatures( options.getDefaultNature() ).contains( Nature.Java );
      final boolean javaProvidedByReplacement =
        null != replacement && null != replacement.findTarget( Nature.Java );
      if ( !javaProvidedByArtifact && !javaProvidedByReplacement )
      {
        final String message =
          "Artifact '" + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "' declared as a " +
          "dependency but does not declare the Java nature which is required if the verifyConfigSha256 option is " +
          "set to true.";
        throw new DepgenValidationException( message );
      }
      if ( null != artifact &&
           !javaProvidedByReplacement &&
           !DepGenConfig.getClassifier().equals( artifact.getClassifier() ) )
      {
        final String message =
          "Artifact '" + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "' declared as a " +
          "dependency but does not specify the classifier '" + DepGenConfig.getClassifier() + "' which is " +
          "required if the verifyConfigSha256 option is set to true.";
        throw new DepgenValidationException( message );
      }
    }
  }

  private static void propagateNature( @Nonnull final ApplicationRecord record,
                                       @Nonnull final Nature rootNature,
                                       @Nonnull final Nature targetNature )
  {
    for ( final ArtifactRecord artifact : record.getArtifacts() )
    {
      if ( null != artifact.getArtifactModel() &&
           artifact.getNatures().contains( rootNature ) &&
           !artifact.isNatureReplaced( rootNature ) )
      {
        checkTransitiveNature( artifact, artifact, rootNature, targetNature );
      }
    }
  }

  private static void checkTransitiveNature( @Nonnull final ArtifactRecord root,
                                             @Nonnull final ArtifactRecord artifact,
                                             @Nonnull final Nature rootNature,
                                             @Nonnull final Nature targetNature )
  {
    for ( final ArtifactRecord dependency : artifact.getDeps() )
    {
      if ( null == dependency.getArtifactModel() )
      {
        if ( dependency.addNature( targetNature ) )
        {
          if ( !dependency.isNatureReplaced( targetNature ) )
          {
            checkTransitiveNature( root, dependency, rootNature, targetNature );
          }
        }
      }
      else if ( !dependency.getNatures().contains( targetNature ) )
      {
        //Must be a declared dependency
        final String message =
          "Artifact '" + dependency.getArtifact() + "' does not specify the " +
          targetNature + " nature but is a " + ( root == artifact ? "direct" : "transitive" ) +
          " dependency of '" + root.getArtifact() + "' which has the " +
          rootNature + " nature. This is not a supported scenario.";
        throw new DepgenValidationException( message );
      }
    }
  }

  private void ensureEmittedTargetNamesAreUnique()
  {
    final HashMap<String, String> names = new HashMap<>();
    if ( getSource().getOptions().verifyConfigSha256() )
    {
      final String verifyTargetName = getSource().verifyTargetName();
      ensureUniqueEmittedTargetName( names,
                                     verifyTargetName,
                                     "built-in helper target '" + verifyTargetName + "'" );
      final String regenerateExtensionTargetName = getUpdateGeneratedOutputsTargetName();
      ensureUniqueEmittedTargetName( names,
                                     regenerateExtensionTargetName,
                                     "built-in helper target '" + regenerateExtensionTargetName + "'" );
    }
    for ( final ArtifactRecord artifact : getArtifacts() )
    {
      if ( artifact.emitsTargets() )
      {
        for ( final Map.Entry<String, String> entry : artifact.getEmittedPublicTargetNames().entrySet() )
        {
          ensureUniqueEmittedTargetName( names, entry.getKey(), entry.getValue() );
        }
        for ( final Map.Entry<String, String> entry : artifact.getEmittedPrivateTargetNames().entrySet() )
        {
          ensureUniqueEmittedTargetName( names, entry.getKey(), entry.getValue() );
        }
      }
    }
  }

  private void ensureEmittedRepositoryNamesAreUnique()
  {
    final HashMap<String, String> names = new HashMap<>();
    for ( final ArtifactRecord artifact : getArtifacts() )
    {
      if ( artifact.emitsRepositoryRules() )
      {
        for ( final Map.Entry<String, String> entry : artifact.getEmittedRepositoryNames().entrySet() )
        {
          ensureUniqueEmittedRepositoryName( names, entry.getKey(), entry.getValue() );
        }
      }
    }
  }

  private void ensureUniqueEmittedTargetName( @Nonnull final Map<String, String> names,
                                              @Nonnull final String name,
                                              @Nonnull final String description )
  {
    final String existing = names.get( name );
    if ( null != existing )
    {
      throw new DepgenValidationException( "Multiple emitted targets have the same name '" + name + "' which is " +
                                           "not supported. Adjust naming configuration or explicit names for " +
                                           existing + " and " + description + "." );
    }
    else
    {
      names.put( name, description );
    }
  }

  private void ensureUniqueEmittedRepositoryName( @Nonnull final Map<String, String> names,
                                                  @Nonnull final String name,
                                                  @Nonnull final String description )
  {
    final String existing = names.get( name );
    if ( null != existing )
    {
      throw new DepgenValidationException( "Multiple emitted repositories have the same name '" + name + "' which is " +
                                           "not supported. Adjust repository naming configuration for " +
                                           existing + " and " + description + "." );
    }
    else
    {
      names.put( name, description );
    }
  }

  private ApplicationRecord( @Nonnull final ApplicationModel source,
                             @Nonnull final DependencyNode node,
                             @Nonnull final List<AuthenticationContext> authenticationContexts )
  {
    _source = Objects.requireNonNull( source );
    _node = Objects.requireNonNull( node );
    final Map<String, AuthenticationContext> contexts = new HashMap<>();
    authenticationContexts.forEach( c -> contexts.put( c.getRepository().getId(), c ) );
    _authenticationContexts = Collections.unmodifiableMap( contexts );
  }

  @Nonnull
  public ApplicationModel getSource()
  {
    return _source;
  }

  @Nonnull
  public DependencyNode getNode()
  {
    return _node;
  }

  @Nonnull
  public Map<String, AuthenticationContext> getAuthenticationContexts()
  {
    return _authenticationContexts;
  }

  @Nonnull
  public List<ArtifactRecord> getArtifacts()
  {
    return _artifacts
      .values()
      .stream()
      .sorted( Comparator.comparing( ArtifactRecord::getKey ) )
      .collect( Collectors.toList() );
  }

  /**
   * Return the relative path from the extension file to the source dependency file.
   *
   * @return the relative path from the  extension file to the source dependency file.
   */
  @Nonnull
  Path getPathFromExtensionToConfig()
  {
    final Path configLocation = _source.getConfigLocation();
    final Path extensionFile = _source.getOptions().getExtensionFile();
    return extensionFile.getParent()
      .toAbsolutePath()
      .normalize()
      .relativize( configLocation.toAbsolutePath().normalize() );
  }

  public void writeBazelExtension( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final OptionsModel options = getSource().getOptions();
    final boolean includeWorkspaceMacro = options.isRepositoryRuleGenerationInExtensionFile();
    final boolean includeTargetMacro = options.isTargetGenerationInExtensionFile();

    emitAutoGeneratedComment( output, true );
    output.newLine();
    writeExtensionDescription( output, includeWorkspaceMacro, includeTargetMacro );
    if ( includeTargetMacro )
    {
      writeDependencyGraphIfRequired( output );
    }
    writeRepositoryRuleLoadsIfRequired( output, includeWorkspaceMacro );
    writeTargetLoadsIfRequired( output, includeTargetMacro );
    if ( includeTargetMacro && getSource().getOptions().verifyConfigSha256() )
    {
      output.write( "# SHA256 of the configuration content that generated this file" );
      output.write( "_CONFIG_SHA256 = \"" + getSource().getConfigSha256() + "\"" );
      output.newLine();
    }
    if ( includeWorkspaceMacro )
    {
      writeWorkspaceMacro( output );
    }
    if ( includeWorkspaceMacro && includeTargetMacro )
    {
      output.newLine();
    }
    if ( includeTargetMacro )
    {
      writeTargetMacro( output );
    }
  }

  @Nonnull
  private Set<String> getJavaRules()
  {
    final Set<String> javaRules = new HashSet<>();
    if ( getSource().getOptions().verifyConfigSha256() )
    {
      javaRules.add( "java_binary" );
      javaRules.add( "java_test" );
    }
    for ( final ArtifactRecord artifact : getArtifacts() )
    {
      if ( artifact.shouldEmitNatureTarget( Nature.Java ) )
      {
        javaRules.add( "java_import" );
      }
      if ( artifact.shouldEmitNatureTarget( Nature.Plugin ) )
      {
        javaRules.add( "java_import" );
        javaRules.add( "java_library" );
        javaRules.add( "java_plugin" );
      }
    }
    return javaRules;
  }

  public void writeDefaultExtensionBuild( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    writeDefaultExtensionBuild( output, true );
  }

  public void writeDefaultExtensionBuild( @Nonnull final StarlarkOutput output, final boolean includeTargetMacro )
    throws IOException
  {
    emitAutoGeneratedComment( output, false );
    output.write( "# Contents can be edited and will not be overridden." );

    output.write( "package(default_visibility = [\"//visibility:public\"])" );
    output.newLine();

    final OptionsModel options = getSource().getOptions();
    final Path extensionFile = options.getExtensionFile();
    final Path workspaceDirectory = options.getWorkspaceDirectory();

    if ( includeTargetMacro )
    {
      final String targetMacroName = options.getTargetMacroName();
      output.write( "load(\"//" + workspaceDirectory.relativize( extensionFile.getParent() ) +
                    ":" + extensionFile.getName( extensionFile.getNameCount() - 1 ) +
                    "\", \"" + targetMacroName + "\")" );
      output.newLine();

      output.write( targetMacroName + "()" );
    }

    if ( includeTargetMacro && getRelativeConfigDirFromExtension().toString().isEmpty() )
    {
      output.newLine();
      output.write( "exports_files([\"" + getSource().getConfigLocation().getFileName() + "\"])" );
    }
  }

  private void emitAutoGeneratedComment( @Nonnull final StarlarkOutput output, final boolean doNotEditWarning )
    throws IOException
  {
    output.write( "# " + ( doNotEditWarning ? "DO NOT EDIT: " : "" ) +
                  "File is auto-generated from " + getPathFromExtensionToConfig() +
                  " by https://github.com/realityforge/bazel-depgen version " + DepGenConfig.getVersion() );
  }

  public void writeDefaultConfigBuild( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    emitAutoGeneratedComment( output, false );
    output.write( "# Contents can be edited and will not be overridden." );

    output.write( "package(default_visibility = [\"//visibility:public\"])" );
    output.newLine();

    output.write( "exports_files([\"" + getSource().getConfigLocation().getFileName() + "\"])" );
  }

  public void writeBazelModuleSection( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    emitGeneratedSectionComment( output );
    output.newLine();
    writeRepositoryRuleUseRepoBindingsIfRequired( output );
    writeDirectRepositoryRules( output );
  }

  public void writeBazelBuildSection( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    emitGeneratedSectionComment( output );
    output.newLine();
    writeTargetLoadsIfRequired( output, true );
    if ( getSource().getOptions().verifyConfigSha256() )
    {
      output.write( "# SHA256 of the configuration content that generated this content" );
      output.write( "_CONFIG_SHA256 = \"" + getSource().getConfigSha256() + "\"" );
      output.newLine();
    }
    output.write( "exports_files([\"" + getSource().getConfigLocation().getFileName() + "\"])" );
    output.newLine();
    writeDependencyGraphIfRequired( output );
    writeDirectTargets( output );
  }

  void artifact( @Nonnull final DependencyNode node,
                 @Nonnull final String sha256,
                 @Nonnull final List<String> urls,
                 @Nullable final String sourceSha256,
                 @Nullable final List<String> sourceUrls,
                 @Nullable final String externalAnnotationSha256,
                 @Nullable final List<String> externalAnnotationUrls,
                 @Nullable final List<String> processors,
                 @Nullable final List<String> jsAssets )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ArtifactModel model = _source.findArtifact( groupId, artifactId );
    final ReplacementModel replacementModel = _source.findReplacement( groupId, artifactId );
    final ArtifactRecord record =
      new ArtifactRecord( this,
                          node,
                          sha256,
                          urls,
                          sourceSha256,
                          sourceUrls,
                          externalAnnotationSha256,
                          externalAnnotationUrls,
                          processors,
                          jsAssets,
                          model,
                          replacementModel );
    final String key = record.getKey();
    final ArtifactRecord existing = _artifacts.get( key );
    if ( null == existing )
    {
      _artifacts.put( key, record );
    }
    else
    {
      if ( !"".equals( existing.getArtifact().getClassifier() ) && "".equals( node.getArtifact().getClassifier() ) )
      {
        _artifacts.put( key, record );
      }
    }
  }

  @Nonnull
  ArtifactRecord getArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return Objects.requireNonNull( findArtifact( groupId, artifactId ) );
  }

  @Nullable
  ArtifactRecord findArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return findArtifact( m -> m.shouldMatch( groupId, artifactId ) );
  }

  @Nullable
  private ArtifactRecord findArtifact( @Nonnull final Predicate<ArtifactRecord> predicate )
  {
    return _artifacts
      .values()
      .stream()
      .filter( predicate )
      .findAny()
      .orElse( null );
  }

  void writeUpdateGeneratedOutputsTarget( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final String configLabel = getConfigFileLabel();
    final String depgenArtifactLabel = getDepgenArtifactLabel();
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + getUpdateGeneratedOutputsTargetName() + "\"" );
    arguments.put( "args",
                   Arrays.asList( "\"--config-file\"",
                                  "\"$(rootpath " + configLabel + ")\"",
                                  "\"--verbose\"",
                                  "\"generate\"" ) );
    arguments.put( "data", Collections.singletonList( "\"" + configLabel + "\"" ) );
    arguments.put( "main_class", "\"org.realityforge.bazel.depgen.Main\"" );
    arguments.put( "tags",
                   Arrays.asList( "\"local\"", "\"manual\"", "\"no-cache\"", "\"no-remote\"", "\"no-sandbox\"" ) );
    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    arguments.put( "runtime_deps", Collections.singletonList( "\"" + depgenArtifactLabel + "\"" ) );
    output.writeCall( "_java_binary", arguments );
  }

  void writeRegenerateExtensionTarget( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    writeUpdateGeneratedOutputsTarget( output );
  }

  void writeVerifyTarget( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + _source.verifyTargetName() + "\"" );
    arguments.put( "size", "\"small\"" );
    arguments.put( "runtime_deps", Collections.singletonList( "\"" + getDepgenArtifactLabel() + "\"" ) );
    arguments.put( "main_class", "\"org.realityforge.bazel.depgen.Main\"" );
    arguments.put( "use_testrunner", Boolean.FALSE );

    final String configLabel = getConfigFileLabel();
    arguments.put( "args",
                   Arrays.asList( "\"--config-file\"",
                                  "\"$(rootpath " + configLabel + ")\"",
                                  "\"--verbose\"",
                                  "\"hash\"",
                                  "\"--verify-sha256\"",
                                  "_CONFIG_SHA256" ) );

    arguments.put( "data", Collections.singletonList( "\"" + configLabel + "\"" ) );
    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );

    output.writeCall( "_java_test", arguments );
  }

  @Nonnull
  public String getUpdateGeneratedOutputsTargetName()
  {
    return _source.getOptions().getNamePrefix() + "update_depgen_generated_outputs";
  }

  @Nonnull
  public String getConfigFileLabel()
  {
    return "//" + getRelativeConfigPath() + ":" + _source.getConfigLocation().getFileName();
  }

  @Nonnull
  private String getDepgenArtifactLabel()
  {
    final ArtifactRecord artifact = findArtifact( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
    if ( null != artifact )
    {
      return artifact.getLabel( Nature.Java );
    }
    else
    {
      return getSource()
        .getReplacement( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() )
        .getTarget( Nature.Java );
    }
  }

  @Nonnull
  private Path getRelativeConfigDirFromExtension()
  {
    final Path configLocation = _source.getConfigLocation();
    final Path extensionFile = _source.getOptions().getExtensionFile();
    return extensionFile.getParent()
      .toAbsolutePath()
      .normalize()
      .relativize( configLocation.getParent().toAbsolutePath().normalize() );
  }

  @Nonnull
  private Path getRelativeConfigPath()
  {
    return _source.getOptions().getWorkspaceDirectory().relativize( _source.getConfigLocation().getParent() );
  }

  void writeTargetMacro( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final OptionsModel options = getSource().getOptions();
    final boolean supportDependencyOmit = options.supportDependencyOmit();
    output.writeMacro( options.getTargetMacroName(),
                       supportDependencyOmit ?
                       getArtifacts()
                         .stream()
                         .filter( ArtifactRecord::emitsTargets )
                         .sorted( Comparator.comparing( ArtifactRecord::getSymbol ) )
                         .map( a -> "omit_" + a.getSymbol() + " = False" )
                         .collect( Collectors.toList() ) :
                       Collections.emptyList(), macro -> {
        macro.writeMultilineComment( o -> o.write( "Macro to define targets for dependencies." ) );
        if ( getSource().getOptions().verifyConfigSha256() )
        {
          macro.newLine();
          writeVerifyTarget( output );
          macro.newLine();
          writeUpdateGeneratedOutputsTarget( output );
        }
        for ( final ArtifactRecord artifact : getArtifacts() )
        {
          if ( artifact.emitsTargets() )
          {
            macro.newLine();
            if ( supportDependencyOmit )
            {
              macro.writeIfCondition( "not omit_" + artifact.getSymbol(), artifact::writeArtifactTargets );
            }
            else
            {
              artifact.writeArtifactTargets( macro );
            }
          }
        }
      } );
  }

  void writeWorkspaceMacro( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final OptionsModel options = getSource().getOptions();
    final boolean supportDependencyOmit = options.supportDependencyOmit();
    output.writeMacro( options.getWorkspaceMacroName(),
                       supportDependencyOmit ?
                       getArtifacts()
                         .stream()
                         .filter( ArtifactRecord::emitsRepositoryRules )
                         .sorted( Comparator.comparing( ArtifactRecord::getSymbol ) )
                         .map( a -> "omit_" + a.getSymbol() + " = False" )
                         .collect( Collectors.toList() ) :
                       Collections.emptyList(), macro -> {
        macro.writeMultilineComment( o -> {
          o.write( "Repository rules macro to load dependencies." );
          o.newLine();
          o.write( "Must be run from a WORKSPACE file." );
        } );

        for ( final ArtifactRecord artifact : getArtifacts() )
        {
          if ( artifact.emitsRepositoryRules() )
          {
            macro.newLine();
            if ( supportDependencyOmit )
            {
              macro.writeIfCondition( "not omit_" + artifact.getSymbol(),
                                      o -> writeArtifactHttpRules( artifact, o, false ) );
            }
            else
            {
              writeArtifactHttpRules( artifact, macro, false );
            }
          }
        }
      } );
  }

  private void writeArtifactHttpRules( @Nonnull final ArtifactRecord artifact,
                                       @Nonnull final StarlarkOutput output,
                                       final boolean useRepoRuleBindingStyle )
    throws IOException
  {
    boolean needsNewLine = false;
    if ( artifact.emitsBinaryRepositoryRule() )
    {
      needsNewLine = true;
      artifact.writeArtifactHttpFileRule( output );
    }

    if ( null != artifact.getSourceSha256() && artifact.emitsSourceRepositoryRule() )
    {
      if ( needsNewLine )
      {
        output.newLine();
      }
      needsNewLine = true;
      artifact.writeArtifactSourcesHttpFileRule( output );
    }
    if ( null != artifact.getExternalAnnotationSha256() && artifact.emitsAnnotationsRepositoryRule() )
    {
      if ( needsNewLine )
      {
        output.newLine();
      }
      needsNewLine = true;
      artifact.writeArtifactAnnotationsHttpFileRule( output );
    }
    if ( artifact.emitsJsSourceRepositoryRule() )
    {
      if ( needsNewLine )
      {
        output.newLine();
      }
      artifact.writeArtifactJsSourcesHttpArchiveRule( output, useRepoRuleBindingStyle );
    }
  }

  void writeDependencyGraphIfRequired( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final ApplicationModel source = getSource();
    if ( source.getOptions().emitDependencyGraph() )
    {
      output.write( "# Dependency Graph Generated from the input data" );
      getNode().accept( new DependencyGraphEmitter( source, line -> {

        try
        {
          output.write( "# " + line );
        }
        catch ( final IOException ioe )
        {
          throw new IllegalStateException( ioe );
        }
      } ) );
      output.newLine();
    }
  }

  private void writeExtensionDescription( @Nonnull final StarlarkOutput output,
                                          final boolean includeWorkspaceMacro,
                                          final boolean includeTargetMacro )
    throws IOException
  {
    output.writeMultilineComment( o -> {
      if ( includeWorkspaceMacro && includeTargetMacro )
      {
        o.write( "Macro rules to load dependencies." );
        o.newLine();
        o.write( "Invoke '" + getSource().getOptions().getWorkspaceMacroName() + "' from a WORKSPACE file." );
        o.write( "Invoke '" + getSource().getOptions().getTargetMacroName() + "' from a BUILD.bazel file." );
      }
      else
      {
        o.write( "Generated dependency helpers." );
        if ( includeWorkspaceMacro )
        {
          o.newLine();
          o.write( "Invoke '" + getSource().getOptions().getWorkspaceMacroName() + "' from a WORKSPACE file." );
        }
        if ( includeTargetMacro )
        {
          o.newLine();
          o.write( "Invoke '" + getSource().getOptions().getTargetMacroName() + "' from a BUILD.bazel file." );
        }
      }
    } );
  }

  private void writeRepositoryRuleLoadsIfRequired( @Nonnull final StarlarkOutput output, final boolean includeLoads )
    throws IOException
  {
    if ( includeLoads && getArtifacts().stream().anyMatch( ArtifactRecord::emitsRepositoryRules ) )
    {
      output.write( "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", " +
                    "_http_file = \"http_file\"" +
                    ( requiresHttpArchive() ? ", _http_archive = \"http_archive\"" : "" ) +
                    ")" );
    }
  }

  private void writeRepositoryRuleUseRepoBindingsIfRequired( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    if ( getArtifacts().stream().anyMatch( ArtifactRecord::emitsRepositoryRules ) )
    {
      output.write( "_http_file = use_repo_rule(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")" );
      if ( requiresHttpArchive() )
      {
        output.write(
          "_http_archive = use_repo_rule(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_archive\")" );
      }
      output.newLine();
    }
  }

  private void writeTargetLoadsIfRequired( @Nonnull final StarlarkOutput output, final boolean includeLoads )
    throws IOException
  {
    if ( includeLoads )
    {
      boolean emittedLoad = false;
      final Set<String> javaRules = getJavaRules();
      if ( !javaRules.isEmpty() )
      {
        emittedLoad = true;
        final String rules =
          javaRules.stream().sorted().map( r -> "_" + r + " = \"" + r + "\"" ).collect( Collectors.joining( ", " ) );
        output.write( "load(\"@rules_java//java:defs.bzl\", " + rules + ")" );
      }
      if ( getArtifacts().stream().anyMatch( a -> a.shouldEmitNatureTarget( Nature.J2cl ) ) )
      {
        emittedLoad = true;
        output.write( "load(\"@j2cl//build_defs:rules.bzl\", _j2cl_library = \"j2cl_library\")" );
      }
      if ( emittedLoad )
      {
        output.newLine();
      }
    }
  }

  private boolean requiresHttpArchive()
  {
    return getArtifacts().stream().anyMatch( a -> null != a.getJsAssets() && a.shouldEmitNatureTarget( Nature.J2cl ) );
  }

  private void emitGeneratedSectionComment( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.write( "# DO NOT EDIT: Content is auto-generated from " +
                  getConfigFileLabel() +
                  " by https://github.com/realityforge/bazel-depgen version " +
                  DepGenConfig.getVersion() );
  }

  private void writeDirectTargets( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    if ( getSource().getOptions().verifyConfigSha256() )
    {
      writeVerifyTarget( output );
      output.newLine();
      writeUpdateGeneratedOutputsTarget( output );
    }

    for ( final ArtifactRecord artifact : getArtifacts() )
    {
      if ( artifact.emitsTargets() )
      {
        output.newLine();
        artifact.writeArtifactTargets( output );
      }
    }
  }

  private void writeDirectRepositoryRules( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    int count = 0;
    for ( final ArtifactRecord artifact : getArtifacts() )
    {
      if ( artifact.emitsRepositoryRules() )
      {
        if ( 0 != count++ )
        {
          output.newLine();
        }
        writeArtifactHttpRules( artifact, output, true );
      }
    }
  }
}
