package org.realityforge.bazel.depgen.record;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.AuthenticationContext;
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
    ensureAliasesAreUnique( record );
    record.getArtifacts().forEach( ArtifactRecord::validate );
    return record;
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

  private static void ensureAliasesAreUnique( @Nonnull final ApplicationRecord record )
  {
    final HashMap<String, ArtifactRecord> aliases = new HashMap<>();
    for ( final ArtifactRecord artifact : record.getArtifacts() )
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
    final Path toConfig = getPathFromExtensionToConfig();
    output.write( "# DO NOT EDIT: File is auto-generated from " + toConfig +
                  " by https://github.com/realityforge/bazel-depgen" );
    output.newLine();

    output.writeMultilineComment( o -> {
      o.write( "Macro rules to load dependencies defined in '" + toConfig + "'." );
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

    writeWorkspaceMacro( output );

    output.newLine();

    writeTargetMacro( output );
  }

  public void writeDefaultBuild( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.write( "# File is auto-generated from " + getPathFromExtensionToConfig() +
                  " by https://github.com/realityforge/bazel-depgen" );
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
