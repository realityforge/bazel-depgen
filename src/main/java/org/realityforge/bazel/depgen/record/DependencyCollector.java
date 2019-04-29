package org.realityforge.bazel.depgen.record;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.realityforge.bazel.depgen.util.HashUtil;

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
      // Manually supplied dependency
      return false;
    }
    else
    {
      final File file = artifact.getFile();
      assert null != file;
      _record.artifact( node, sha256( file ) );
      return true;
    }
  }

  @Nonnull
  private String sha256( @Nonnull final File file )
  {
    try
    {
      return HashUtil.sha256( Files.readAllBytes( file.toPath() ) );
    }
    catch ( final IOException ioe )
    {
      throw new IllegalStateException( "Error generating sha256 hash for file " + file, ioe );
    }
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
