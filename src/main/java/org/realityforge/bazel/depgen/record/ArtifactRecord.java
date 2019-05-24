package org.realityforge.bazel.depgen.record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.realityforge.bazel.depgen.config.AliasStrategy;
import org.realityforge.bazel.depgen.config.Language;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.gen.StarlarkOutput;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.ReplacementModel;
import org.realityforge.bazel.depgen.util.ArtifactUtil;
import org.realityforge.bazel.depgen.util.BazelUtil;

public final class ArtifactRecord
{
  /**
   * The suffix applied to the name of target that imports artifact for use when defining plugins.
   */
  private static final String PLUGIN_LIBRARY_SUFFIX = "__plugin_library";
  /**
   * The suffix applied to every compile plugin.
   */
  private static final String PLUGIN_SUFFIX = "__plugin";
  /**
   * The suffix applied to the library that exports all the plugins.
   * Note that this is only defined if the {@link Nature#LibraryAndPlugin} nature is used by the artifact.
   */
  private static final String PLUGINS_LIBRARY_SUFFIX = "__plugins";
  @Nonnull
  private final ApplicationRecord _application;
  @Nonnull
  private final DependencyNode _node;
  @Nullable
  private final ArtifactModel _artifactModel;
  @Nullable
  private final ReplacementModel _replacementModel;
  @Nullable
  private final String _sha256;
  @Nullable
  private final List<String> _urls;
  @Nullable
  private final String _sourceSha256;
  @Nullable
  private final List<String> _sourceUrls;
  @Nullable
  private final List<String> _processors;
  @Nullable
  private List<ArtifactRecord> _depsCache;
  @Nullable
  private List<ArtifactRecord> _reverseDepsCache;
  @Nullable
  private List<ArtifactRecord> _runtimeDepsCache;
  @Nullable
  private List<ArtifactRecord> _reverseRuntimeDepsCache;

  ArtifactRecord( @Nonnull final ApplicationRecord application,
                  @Nonnull final DependencyNode node,
                  @Nullable final String sha256,
                  @Nullable final List<String> urls,
                  @Nullable final String sourceSha256,
                  @Nullable final List<String> sourceUrls,
                  @Nullable final List<String> processors,
                  @Nullable final ArtifactModel artifactModel,
                  @Nullable final ReplacementModel replacementModel )
  {
    assert ( null == sha256 && null == urls ) || ( null != sha256 && null != urls && !urls.isEmpty() );
    assert ( null == sourceSha256 && null == sourceUrls ) ||
           ( null != sourceSha256 && null != sourceUrls && !sourceUrls.isEmpty() );
    assert null == artifactModel || null == replacementModel;
    _application = Objects.requireNonNull( application );
    _node = Objects.requireNonNull( node );
    if ( null == replacementModel )
    {
      _sha256 = Objects.requireNonNull( sha256 );
      _urls = Collections.unmodifiableList( new ArrayList<>( Objects.requireNonNull( urls ) ) );
      _replacementModel = null;
      _artifactModel = artifactModel;
      _sourceSha256 = sourceSha256;
      _sourceUrls = null != sourceUrls ? Collections.unmodifiableList( new ArrayList<>( sourceUrls ) ) : null;
      _processors = null != processors ? Collections.unmodifiableList( new ArrayList<>( processors ) ) : null;
    }
    else
    {
      assert null == sha256;
      assert null == sourceSha256;
      assert null == processors;
      _sha256 = null;
      _urls = null;
      _sourceSha256 = null;
      _sourceUrls = null;
      _processors = null;
      _replacementModel = replacementModel;
      _artifactModel = null;
    }
  }

  @Nonnull
  public String getKey()
  {
    final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
    return artifact.getGroupId() + ":" + artifact.getArtifactId();
  }

  @Nonnull
  public String getName()
  {
    final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
    return getNamePrefix() +
           BazelUtil.cleanNamePart( artifact.getGroupId() ) +
           "__" +
           BazelUtil.cleanNamePart( artifact.getArtifactId() ) +
           "__" +
           BazelUtil.cleanNamePart( artifact.getVersion() );
  }

  @Nonnull
  public String getLabel()
  {
    return null != _replacementModel ? _replacementModel.getTarget() : getAlias();
  }

  @Nonnull
  public String getAlias()
  {
    final String declaredAlias = null != _artifactModel ? _artifactModel.getAlias() : null;
    if ( null != declaredAlias )
    {
      return BazelUtil.cleanNamePart( declaredAlias );
    }
    else
    {
      final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
      final AliasStrategy aliasStrategy = _application.getSource().getOptions().getAliasStrategy();
      if ( AliasStrategy.GroupIdAndArtifactId == aliasStrategy )
      {
        return getNamePrefix() +
               BazelUtil.cleanNamePart( artifact.getGroupId() ) +
               "__" +
               BazelUtil.cleanNamePart( artifact.getArtifactId() );
      }
      else
      {
        assert AliasStrategy.ArtifactId == aliasStrategy;
        return getNamePrefix() + BazelUtil.cleanNamePart( artifact.getArtifactId() );
      }
    }
  }

  @Nonnull
  public Nature getNature()
  {
    if ( null == _artifactModel || Nature.Auto == _artifactModel.getNature() )
    {
      return null == getProcessors() ? Nature.Library : Nature.Plugin;
    }
    else
    {
      return _artifactModel.getNature();
    }
  }

  @Nonnull
  public List<Language> getLanguages()
  {
    if ( null == _artifactModel )
    {
      return Collections.singletonList( _application.getSource().getOptions().getDefaultLanguage() );
    }
    else
    {
      return _artifactModel.getLanguages( _application.getSource().getOptions().getDefaultLanguage() );
    }
  }

  public boolean generatesApi()
  {
    if ( null == _artifactModel )
    {
      return true;
    }
    else
    {
      final Boolean generatesApi = _artifactModel.getSource().getGeneratesApi();
      return null == generatesApi ? true : generatesApi;
    }
  }

  @Nonnull
  private String getNamePrefix()
  {
    return BazelUtil.cleanNamePart( _application.getSource().getOptions().getNamePrefix() );
  }

  @Nonnull
  public String getMavenCoordinatesBazelTag()
  {
    final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
    return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
  }

  @Nonnull
  public org.eclipse.aether.artifact.Artifact getArtifact()
  {
    return _node.getArtifact();
  }

  @Nonnull
  public DependencyNode getNode()
  {
    return _node;
  }

  /**
   * Return the sha256 of the specified artifact.
   * This MUST be null when {@link #getReplacementModel()} is non-null otherwise it must be non-null.
   *
   * @return the sha256 of artifact or null if {@link #getReplacementModel()} returns a non-null value.
   */
  @Nullable
  public String getSha256()
  {
    return _sha256;
  }

  /**
   * Return the urls that the artifact can be downloaded from.
   * This MUST be null when {@link #getReplacementModel()} is non-null otherwise it must be non-null and non-empty.
   *
   * @return the urls.
   */
  @Nullable
  public List<String> getUrls()
  {
    return _urls;
  }

  /**
   * Return the sha256 of the source artifact associated with the artifact.
   * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
   * if {@link #getSourceUrls()} returns a non-null value.
   *
   * @return the sha256 of the source artifact associated with the artifact.
   */
  @Nullable
  public String getSourceSha256()
  {
    return _sourceSha256;
  }

  /**
   * Return the urls that the source artifact can be downloaded from.
   * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
   * if {@link #getSourceSha256()} returns a non-null value.
   *
   * @return the urls.
   */
  @Nullable
  public List<String> getSourceUrls()
  {
    return _sourceUrls;
  }

  /**
   * Return the model that represents the configuration supplied by the user.
   * This method may return null if the artifact was not declared by the user but
   * is a dependency of another artifact or if {@link #getReplacementModel()} returns
   * a non-null value.
   *
   * @return the model if any.
   */
  @Nullable
  public ArtifactModel getArtifactModel()
  {
    return _artifactModel;
  }

  /**
   * Return the replacement for artifact as supplied by the user.
   * This method may return null if the artifact was not as a replacement.
   *
   * @return the replacement if any.
   */
  @Nullable
  public ReplacementModel getReplacementModel()
  {
    return _replacementModel;
  }

  /**
   * Return the name of any annotation processors that this artifact defines.
   *
   * @return the name of any annotation processors that this artifact defines.
   */
  @Nullable
  public List<String> getProcessors()
  {
    return _processors;
  }

  @Nonnull
  public List<ArtifactRecord> getDeps()
  {
    if ( null == _depsCache )
    {
      _depsCache =
        collectArtifacts( _node
                            .getChildren()
                            .stream()
                            .filter( c -> !_application.getSource()
                              .isExcluded( c.getDependency().getArtifact().getGroupId(),
                                           c.getDependency().getArtifact().getArtifactId() ) )
                            .filter( c -> shouldIncludeDependency( Artifact.SCOPE_COMPILE, c ) )
                            .map( c -> _application.getArtifact( c.getDependency().getArtifact().getGroupId(),
                                                                 c.getDependency().getArtifact().getArtifactId() ) ) );
    }
    return _depsCache;
  }

  @Nonnull
  public List<ArtifactRecord> getReverseDeps()
  {
    if ( null == _reverseDepsCache )
    {
      _reverseDepsCache =
        collectArtifacts( _application
                            .getArtifacts()
                            .stream()
                            .filter( a -> a.getDeps().contains( this ) ) );
    }
    return _reverseDepsCache;
  }

  @Nonnull
  public List<ArtifactRecord> getRuntimeDeps()
  {
    if ( null == _runtimeDepsCache )
    {
      _runtimeDepsCache =
        collectArtifacts( _node
                            .getChildren()
                            .stream()
                            .filter( c -> shouldIncludeDependency( Artifact.SCOPE_RUNTIME, c ) )
                            .filter( c -> !_application.getSource()
                              .isExcluded( c.getDependency().getArtifact().getGroupId(),
                                           c.getDependency().getArtifact().getArtifactId() ) )
                            .map( c -> _application.getArtifact( c.getDependency().getArtifact().getGroupId(),
                                                                 c.getDependency().getArtifact().getArtifactId() ) ) );
    }
    return _runtimeDepsCache;
  }

  @Nonnull
  public List<ArtifactRecord> getReverseRuntimeDeps()
  {
    if ( null == _reverseRuntimeDepsCache )
    {
      _reverseRuntimeDepsCache =
        collectArtifacts( _application
                            .getArtifacts()
                            .stream()
                            .filter( a -> a.getRuntimeDeps().contains( this ) ) );
    }
    return _reverseRuntimeDepsCache;
  }

  public boolean shouldExportDeps()
  {
    return null != _artifactModel && _artifactModel.exportDeps( _application.getSource().getOptions().exportDeps() );
  }

  @Nonnull
  private List<ArtifactRecord> collectArtifacts( @Nonnull final Stream<ArtifactRecord> stream )
  {
    return Collections.unmodifiableList( stream
                                           .distinct()
                                           .sorted( Comparator.comparing( ArtifactRecord::getKey ) )
                                           .collect( Collectors.toList() ) );
  }

  private boolean shouldIncludeDependency( @Nonnull final String scope, @Nonnull final DependencyNode c )
  {
    final boolean includeOptional = null != _artifactModel && _artifactModel.includeOptional();
    return ( includeOptional || !c.getDependency().isOptional() ) && scope.equals( c.getDependency().getScope() );
  }

  boolean shouldMatch( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    final DependencyNode node = getNode();
    final org.eclipse.aether.artifact.Artifact artifact = node.getDependency().getArtifact();
    return groupId.equals( artifact.getGroupId() ) && artifactId.equals( artifact.getArtifactId() );
  }

  public void emitAlias( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + getAlias() + "\"" );
    arguments.put( "actual", "\":" + getName() + "\"" );
    final ArtifactModel artifactModel = getArtifactModel();
    if ( null != artifactModel )
    {
      final List<String> visibility = artifactModel.getVisibility();
      if ( !visibility.isEmpty() )
      {
        arguments.put( "visibility",
                       visibility.stream().map( v -> "\"" + v + "\"" ).collect( Collectors.toList() ) );
      }
    }
    else
    {
      arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    }
    output.writeCall( "native.alias", arguments );
  }

  public void emitJavaImport( @Nonnull final StarlarkOutput output, @Nonnull final String nameSuffix )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + getName() + nameSuffix + "\"" );
    arguments.put( "jars", Collections.singletonList( "\"@" + getName() + "//file\"" ) );
    arguments.put( "licenses", Collections.singletonList( "\"notice\"" ) );
    if ( null != getSourceSha256() )
    {
      arguments.put( "srcjar", "\"@" + getName() + "__sources//file\"" );
    }
    arguments.put( "tags",
                   Collections.singletonList( "\"maven_coordinates=" + getMavenCoordinatesBazelTag() + "\"" ) );
    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    final List<ArtifactRecord> deps = getDeps();
    if ( !deps.isEmpty() )
    {
      arguments.put( "deps",
                     deps.stream().map( a -> "\":" + a.getLabel() + "\"" ).sorted().collect( Collectors.toList() ) );
    }
    final List<ArtifactRecord> runtimeDeps = getRuntimeDeps();
    if ( !runtimeDeps.isEmpty() )
    {
      arguments.put( "runtime_deps",
                     runtimeDeps.stream()
                       .map( a -> "\":" + a.getLabel() + "\"" )
                       .sorted()
                       .collect( Collectors.toList() ) );
    }
    if ( shouldExportDeps() )
    {
      arguments.put( "exports",
                     deps.stream().map( a -> "\":" + a.getLabel() + "\"" ).sorted().collect( Collectors.toList() ) );
    }
    output.writeCall( "native.java_import", arguments );
  }

  void emitJavaPlugin( @Nonnull final StarlarkOutput output, @Nullable final String processorClass )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + pluginName( processorClass ) + "\"" );
    if ( null != processorClass )
    {
      arguments.put( "processor_class", "\"" + processorClass + "\"" );
    }
    if ( null != processorClass && generatesApi() )
    {
      arguments.put( "generates_api", "True" );
    }
    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    arguments.put( "deps", Collections.singletonList( "\":" + getName() + PLUGIN_LIBRARY_SUFFIX + "\"" ) );
    output.writeCall( "native.java_plugin", arguments );
  }

  @Nonnull
  String pluginName( @Nullable final String processorClass )
  {
    return getName() +
           ( null == processorClass ? "" : BazelUtil.cleanNamePart( "__" + processorClass ) ) +
           PLUGIN_SUFFIX;
  }

  public void emitPluginLibrary( @Nonnull final StarlarkOutput output, @Nonnull final String suffix )
    throws IOException
  {
    assert Nature.Library != getNature();
    emitJavaImport( output, PLUGIN_LIBRARY_SUFFIX );
    final List<String> processors = getProcessors();
    if ( null == processors )
    {
      emitJavaPlugin( output, null );
    }
    else
    {
      for ( final String processor : processors )
      {
        emitJavaPlugin( output, processor );
      }
    }
    emitJavaPluginLibrary( output, suffix );
  }

  void emitJavaPluginLibrary( @Nonnull final StarlarkOutput output, @Nonnull final String suffix )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + getName() + suffix + "\"" );
    assert Nature.Library != getNature();
    final ArrayList<String> plugins = new ArrayList<>();
    final List<String> processors = getProcessors();
    if ( null == processors )
    {
      plugins.add( "\"" + pluginName( null ) + "\"" );
    }
    else
    {
      for ( final String processor : processors )
      {
        plugins.add( "\"" + pluginName( processor ) + "\"" );
      }
    }
    arguments.put( "exported_plugins", plugins );
    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    output.writeCall( "native.java_library", arguments );
  }

  public void emitJavaLibraryAndPlugin( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    final String name = getName();
    arguments.put( "name", "\"" + name + "\"" );
    assert Nature.LibraryAndPlugin == getNature();

    arguments.put( "exports",
                   Arrays.asList( "\"" + name + PLUGIN_LIBRARY_SUFFIX + "\"",
                                  "\"" + name + PLUGINS_LIBRARY_SUFFIX + "\"" ) );

    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    output.writeCall( "native.java_library", arguments );
  }

  public void emitArtifactHttpFileRule( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + getName() + "\"" );
    final org.eclipse.aether.artifact.Artifact a = getNode().getArtifact();
    assert null != a;
    arguments.put( "downloaded_file_path", "\"" + ArtifactUtil.artifactToPath( a ) + "\"" );
    final String sha256 = getSha256();
    assert null != sha256;
    arguments.put( "sha256", "\"" + sha256.toLowerCase() + "\"" );
    final List<String> urls = getUrls();
    assert null != urls && !urls.isEmpty();
    arguments.put( "urls", urls.stream().map( v -> "\"" + v + "\"" ).collect( Collectors.toList() ) );
    output.writeCall( "http_file", arguments );
  }

  public void emitArtifactSourcesHttpFileRule( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final String sourceSha256 = getSourceSha256();
    assert null != sourceSha256;

    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + getName() + "__sources\"" );
    final org.eclipse.aether.artifact.Artifact a = getNode().getArtifact();
    assert null != a;

    final String artifactPath =
      ArtifactUtil.artifactToPath( a.getGroupId(), a.getArtifactId(), a.getVersion(), "sources", "jar" );
    arguments.put( "downloaded_file_path", "\"" + artifactPath + "\"" );
    arguments.put( "sha256", "\"" + sourceSha256.toLowerCase() + "\"" );
    final List<String> urls = getSourceUrls();
    assert null != urls && !urls.isEmpty();
    arguments.put( "urls", urls.stream().map( v -> "\"" + v + "\"" ).collect( Collectors.toList() ) );
    output.writeCall( "http_file", arguments );
  }
}
