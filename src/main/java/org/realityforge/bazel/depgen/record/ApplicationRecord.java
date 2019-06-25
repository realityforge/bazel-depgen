package org.realityforge.bazel.depgen.record;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.AuthenticationContext;
import org.realityforge.bazel.depgen.DepGenConfig;
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
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
    ensureAliasesAreUnique();
    ensureDepgenArtifactHasJavaNature();
  }

  private void ensureDepgenArtifactHasJavaNature()
  {
    final ApplicationModel model = getSource();
    final OptionsModel options = model.getOptions();
    if ( options.verifyConfigSha256() )
    {
      final ArtifactModel artifact =
        model.findApplicationArtifact( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
      if ( null != artifact && !artifact.getNatures( options.getDefaultNature() ).contains( Nature.Java ) )
      {
        final String message =
          "Artifact '" + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "' declared as a " +
          "dependency but does not declare the Java nature which is required if verifyConfigSha256 option is " +
          "set to true.";
        throw new IllegalStateException( message );
      }

      final ReplacementModel replacement =
        model.findReplacement( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
      if ( null != replacement && replacement.getTargets().stream().noneMatch( t -> t.getNature() == Nature.Java ) )
      {
        final String message =
          "Artifact '" + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "' declared as a replace " +
          "but does not declare the Java nature which is required if verifyConfigSha256 option is set to true.";
        throw new IllegalStateException( message );
      }
    }
  }

  private static void propagateNature( @Nonnull final ApplicationRecord record,
                                       @Nonnull final Nature rootNature,
                                       @Nonnull final Nature targetNature )
  {
    for ( final ArtifactRecord artifact : record.getArtifacts() )
    {
      if ( null != artifact.getArtifactModel() && artifact.getNatures().contains( rootNature ) )
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
          if ( null == dependency.getReplacementModel() )
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
        throw new IllegalStateException( message );
      }
    }
  }

  private void ensureAliasesAreUnique()
  {
    final HashMap<String, ArtifactRecord> aliases = new HashMap<>();
    for ( final ArtifactRecord artifact : getArtifacts() )
    {
      for ( final Nature nature : artifact.getNatures() )
      {
        final String alias = artifact.getAlias( nature );
        final ArtifactRecord existing = aliases.get( alias );
        if ( null != existing )
        {
          throw new IllegalStateException( "Multiple artifacts have the same alias '" + alias + "' which is " +
                                           "not supported. Change the aliasStrategy option globally or for " +
                                           "one of the artifacts '" + existing.getArtifact() + "' and '" +
                                           artifact.getArtifact() + "'." );
        }
        else
        {
          aliases.put( alias, artifact );
        }
      }
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
    emitAutoGeneratedComment( output, true );
    output.newLine();

    output.writeMultilineComment( o -> {
      o.write( "Macro rules to load dependencies defined in '" + getPathFromExtensionToConfig() + "'." );
      o.newLine();
      final OptionsModel options = getSource().getOptions();
      o.write( "Invoke '" + options.getWorkspaceMacroName() + "' from a WORKSPACE file." );
      o.write( "Invoke '" + options.getTargetMacroName() + "' from a BUILD.bazel file." );
    } );

    writeDependencyGraphIfRequired( output );

    boolean emittedLoad = false;
    if ( !getArtifacts().isEmpty() )
    {
      emittedLoad = true;
      output.write( "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")" );
    }
    if ( getArtifacts().stream().anyMatch( a -> a.getNatures().contains( Nature.J2cl ) ) )
    {
      emittedLoad = true;
      output.write( "load(\"@com_google_j2cl//build_defs:rules.bzl\", \"j2cl_library\")" );
    }
    if ( emittedLoad )
    {
      output.newLine();
    }

    if ( getSource().getOptions().verifyConfigSha256() )
    {
      output.write( "# SHA256 of the configuration content that generated this file" );
      output.write( "_CONFIG_SHA256 = \"" + getSource().getConfigSha256() + "\"" );
      output.newLine();
    }

    writeWorkspaceMacro( output );

    output.newLine();

    writeTargetMacro( output );
  }

  public void writeDefaultExtensionBuild( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    emitAutoGeneratedComment( output, false );
    output.write( "# Contents can be edited and will not be overridden." );

    output.write( "package(default_visibility = [\"//visibility:public\"])" );
    output.newLine();

    final OptionsModel options = getSource().getOptions();
    final Path extensionFile = options.getExtensionFile();
    final Path workspaceDirectory = options.getWorkspaceDirectory();

    final String targetMacroName = options.getTargetMacroName();
    output.write( "load(\"//" + workspaceDirectory.relativize( extensionFile.getParent() ) +
                  ":" + extensionFile.getName( extensionFile.getNameCount() - 1 ) +
                  "\", \"" + targetMacroName + "\")" );
    output.newLine();

    output.write( targetMacroName + "()" );

    if ( getRelativeConfigDirFromExtension().toString().isEmpty() )
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

  void replacement( @Nonnull final DependencyNode node )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ReplacementModel model = _source.findReplacement( groupId, artifactId );
    assert null != model;
    final ArtifactRecord record = new ArtifactRecord( this, node, null, null, null, null, null, null, model );
    final String key = record.getKey();
    assert !_artifacts.containsKey( key );
    _artifacts.put( key, record );
  }

  void artifact( @Nonnull final DependencyNode node,
                 @Nonnull final String sha256,
                 @Nonnull final List<String> urls,
                 @Nullable final String sourceSha256,
                 @Nullable final List<String> sourceUrls,
                 @Nullable final List<String> processors )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ArtifactModel model = _source.findArtifact( groupId, artifactId );
    final ArtifactRecord record =
      new ArtifactRecord( this, node, sha256, urls, sourceSha256, sourceUrls, processors, model, null );
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

  void writeVerifyTarget( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + _source.getOptions().getNamePrefix() + "verify_config_sha256\"" );
    final String configLabel = "//" + getRelativeConfigPath() + ":" + _source.getConfigLocation().getFileName();

    final ArtifactRecord artifact = findArtifact( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
    final String depgenArtifactLabel;
    if ( null != artifact )
    {
      depgenArtifactLabel = ":" + artifact.getLabel( Nature.Java );
    }
    else
    {
      final ReplacementModel replacement =
        getSource().findReplacement( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
      assert null != replacement;
      depgenArtifactLabel = replacement.getTarget( Nature.Java );
    }

    arguments.put( "srcs", Arrays.asList( "\"" + depgenArtifactLabel + "\"",
                                          "\"" + configLabel + "\"",
                                          "\"@bazel_tools//tools/jdk:current_java_runtime\"" ) );

    arguments.put( "toolchains", Collections.singletonList( "\"@bazel_tools//tools/jdk:current_java_runtime\"" ) );
    arguments.put( "outs", Collections.singletonList( "\"command-output.txt\"" ) );

    arguments.put( "cmd", "\"$(JAVA) " +
                          "-jar $(location " + depgenArtifactLabel + ") " +
                          "--config-file $(location " + configLabel + ") " +
                          "--quiet " +
                          "hash " +
                          "--verify-sha256 %s > \\\"$@\\\"\" % (_CONFIG_SHA256)" );
    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    output.writeCall( "native.genrule", arguments );
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
                         .filter( a -> null == a.getReplacementModel() )
                         .map( a -> "omit_" + a.getSymbol() + " = False" )
                         .collect( Collectors.toList() ) :
                       Collections.emptyList(), macro -> {
        final String comment =
          "Macro to define targets for dependencies specified by '" + getPathFromExtensionToConfig() + "'.";
        macro.writeMultilineComment( o -> o.write( comment ) );
        if ( getSource().getOptions().verifyConfigSha256() )
        {
          macro.newLine();
          writeVerifyTarget( output );
        }
        for ( final ArtifactRecord artifact : getArtifacts() )
        {
          if ( null == artifact.getReplacementModel() )
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
                         .filter( a -> null == a.getReplacementModel() )
                         .map( a -> "omit_" + a.getSymbol() + " = False" )
                         .collect( Collectors.toList() ) :
                       Collections.emptyList(), macro -> {
        macro.writeMultilineComment( o -> {
          o.write( "Repository rules macro to load dependencies specified by '" +
                   getPathFromExtensionToConfig() +
                   "'." );
          o.newLine();
          o.write( "Must be run from a WORKSPACE file." );
        } );

        for ( final ArtifactRecord artifact : getArtifacts() )
        {
          if ( null == artifact.getReplacementModel() )
          {
            macro.newLine();
            if ( supportDependencyOmit )
            {
              macro.writeIfCondition( "not omit_" + artifact.getSymbol(), o -> writeArtifactHttpRules( artifact, o ) );
            }
            else
            {
              writeArtifactHttpRules( artifact, macro );
            }
          }
        }
      } );
  }

  private void writeArtifactHttpRules( @Nonnull final ArtifactRecord artifact, @Nonnull final StarlarkOutput output )
    throws IOException
  {
    artifact.writeArtifactHttpFileRule( output );

    final String sourceSha256 = artifact.getSourceSha256();
    if ( null != sourceSha256 )
    {
      output.newLine();
      final List<String> sourceUrls = artifact.getSourceUrls();
      assert null != sourceUrls && !sourceUrls.isEmpty();
      artifact.writeArtifactSourcesHttpFileRule( output );
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
}
