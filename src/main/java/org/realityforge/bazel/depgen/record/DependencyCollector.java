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

final class DependencyCollector
  implements DependencyVisitor
{
  @Nonnull
  private final ApplicationRecord _record;

  DependencyCollector( @Nonnull final ApplicationRecord record )
  {
    _record = Objects.requireNonNull( record );
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
    else if ( hasExclude( node.getDependency() ) )
    {
      // Explicitly excluded dependency
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
      final File file = artifact.getFile();
      assert null != file;
      final List<String> urls =
        RecordUtil.deriveUrls( artifact, node.getRepositories(), _record.getAuthenticationContexts() );

      final String sourceSha256;
      final List<String> sourceUrls;
      final String sourcesFilename = artifact.getProperty( Constants.SOURCE_ARTIFACT_FILENAME, null );
      if ( null != sourcesFilename )
      {
        final File sourcesFile = new File( sourcesFilename );
        final org.eclipse.aether.artifact.Artifact sourcesArtifact =
          new SubArtifact( artifact, "sources", "jar" ).setFile( sourcesFile );

        sourceSha256 = RecordUtil.sha256( sourcesFile );
        sourceUrls =
          RecordUtil.deriveUrls( sourcesArtifact, node.getRepositories(), _record.getAuthenticationContexts() );
      }
      else
      {
        sourceSha256 = null;
        sourceUrls = null;
      }

      _record.artifact( node, RecordUtil.sha256( file ), urls, sourceSha256, sourceUrls );
      return true;
    }
  }

  private boolean hasExclude( @Nonnull final Dependency dependency )
  {
    final org.eclipse.aether.artifact.Artifact artifact = dependency.getArtifact();
    return _record.getSource().isExcluded( artifact.getGroupId(), artifact.getArtifactId() );
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
