package org.realityforge.bazel.depgen.record;

import java.io.File;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.realityforge.bazel.depgen.Constants;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.metadata.RecordBuildCallback;

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
    else if ( !( "".equals( node.getDependency().getScope() ) ||
                 Artifact.SCOPE_COMPILE.equals( node.getDependency().getScope() ) ||
                 Artifact.SCOPE_RUNTIME.equals( node.getDependency().getScope() ) ) )
    {
      // Only compile and runtime scoped dependencies are collected
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

    final DepgenMetadata metadata = DepgenMetadata.fromDirectory( _record.getSource(), file.getParentFile().toPath() );

    final String sha256 = metadata.getSha256( artifact.getClassifier(), artifact.getFile() );
    final List<String> urls =
      metadata.getUrls( artifact, node.getRepositories(), _record.getAuthenticationContexts(), _callback );

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
        metadata.getUrls( sourcesArtifact, node.getRepositories(), _record.getAuthenticationContexts(), _callback );
    }
    else
    {
      sourceSha256 = null;
      sourceUrls = null;
    }

    final List<String> processors = metadata.getProcessors( file );

    _record.artifact( node, sha256, urls, sourceSha256, sourceUrls, processors );
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
