package org.realityforge.bazel.depgen.record;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.realityforge.bazel.depgen.Constants;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.metadata.RecordBuildCallback;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class DependencyCollector
  implements DependencyVisitor
{
  @Nonnull
  private final ApplicationRecord _record;
  @Nonnull
  private final RecordBuildCallback _callback;

  DependencyCollector( @Nonnull final ApplicationRecord record, @Nonnull final RecordBuildCallback callback )
  {
    _record = Objects.requireNonNull( record );
    _callback = Objects.requireNonNull( callback );
  }

  @Override
  public boolean visitEnter( @Nonnull final DependencyNode node )
  {
    final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
    if ( null == artifact )
    {
      // This is the root node. Skip it and process children.
      return true;
    }
    else if ( node.getData().containsKey( ConflictResolver.NODE_DATA_WINNER ) )
    {
      // This is a duplicate dependency that is NOT the winner but instead points at one.
      // So we can just avoid processing any child nodes.
      return false;
    }
    else if ( hasReplacement( node.getDependency() ) )
    {
      // Manually supplied dependency
      _record.replacement( node );
      return false;
    }
    else
    {
      processArtifact( node );
      return true;
    }
  }

  private void processArtifact( @Nonnull final DependencyNode node )
  {
    final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
    assert null != artifact;
    final File file = artifact.getFile();
    assert null != file;

    final ArtifactModel model = _record.getSource().findArtifact( artifact.getGroupId(), artifact.getArtifactId() );
    final DepgenMetadata metadata = DepgenMetadata.fromDirectory( _record.getSource(), file.getParentFile().toPath() );

    final String sha256 = metadata.getSha256( artifact.getClassifier(), artifact.getFile() );
    final List<RemoteRepository> repositories =
      null != model && !model.getRepositories().isEmpty() ?
      node.getRepositories()
        .stream()
        .filter( r -> model.getRepositories().contains( r.getId() ) )
        .collect( Collectors.toList() ) :
      node.getRepositories()
        .stream()
        .filter( r -> _record.getSource().getRepository( r.getId() ).searchByDefault() )
        .collect( Collectors.toList() );
    final List<String> urls =
      metadata.getUrls( artifact, repositories, _record.getAuthenticationContexts(), _callback );

    final String sourceSha256;
    final List<String> sourceUrls;
    final String sourcesFilename = artifact.getProperty( Constants.SOURCE_ARTIFACT_FILENAME, null );
    if ( null != sourcesFilename )
    {
      final File sourcesFile = new File( sourcesFilename );
      final org.eclipse.aether.artifact.Artifact sourcesArtifact =
        new SubArtifact( artifact, "sources", "jar" ).setFile( sourcesFile );

      sourceSha256 = metadata.getSha256( sourcesArtifact.getClassifier(), sourcesArtifact.getFile() );
      sourceUrls =
        metadata.getUrls( sourcesArtifact, repositories, _record.getAuthenticationContexts(), _callback );
    }
    else
    {
      sourceSha256 = null;
      sourceUrls = null;
    }
    final String externalAnnotationSha256;
    final List<String> externalAnnotationUrls;
    final String externalAnnotationsFilename =
      artifact.getProperty( Constants.EXTERNAL_ANNOTATIONS_ARTIFACT_FILENAME, null );
    if ( null != externalAnnotationsFilename )
    {
      final File sourcesFile = new File( externalAnnotationsFilename );
      final org.eclipse.aether.artifact.Artifact sourcesArtifact =
        new SubArtifact( artifact, "annotations", "jar" ).setFile( sourcesFile );

      externalAnnotationSha256 = metadata.getSha256( sourcesArtifact.getClassifier(), sourcesArtifact.getFile() );
      externalAnnotationUrls =
        metadata.getUrls( sourcesArtifact, repositories, _record.getAuthenticationContexts(), _callback );
    }
    else
    {
      externalAnnotationSha256 = null;
      externalAnnotationUrls = null;
    }

    final List<String> processors = metadata.getProcessors( file );

    _record.artifact( node,
                      sha256,
                      urls,
                      sourceSha256,
                      sourceUrls,
                      externalAnnotationSha256,
                      externalAnnotationUrls,
                      processors );
  }

  private boolean hasReplacement( @Nonnull final Dependency dependency )
  {
    return _record
      .getSource()
      .getReplacements()
      .stream()
      .anyMatch( replacement -> replacement.getGroup().equals( dependency.getArtifact().getGroupId() ) &&
                                replacement.getId().equals( dependency.getArtifact().getArtifactId() ) );
  }

  @Override
  public boolean visitLeave( @Nonnull final DependencyNode node )
  {
    return true;
  }
}
