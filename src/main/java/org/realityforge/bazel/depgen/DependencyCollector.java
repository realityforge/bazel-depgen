package org.realityforge.bazel.depgen;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import javax.annotation.Nonnull;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.realityforge.bazel.depgen.model.ApplicationModel;

final class DependencyCollector
  implements DependencyVisitor
{
  @Nonnull
  private final ApplicationModel _model;
  /**
   * The set of non-optional dependencies already processed.
   * A dependency that is present in this set should NOT be present in {@link #_optionalDependencies}.
   * If a dependency is added to this set, it should be removed from {@link #_optionalDependencies}.
   */
  private final Set<Dependency> _dependencies = new HashSet<>();
  /**
   * The set of optional dependencies already processed.
   * A dependency that is present in this set should NOT be present in {@link #_dependencies}.
   */
  private final Set<Dependency> _optionalDependencies = new HashSet<>();
  private final Stack<Dependency> _stack = new Stack<>();

  public DependencyCollector( @Nonnull final ApplicationModel model )
  {
    _model = Objects.requireNonNull( model );
  }

  @Override
  public boolean visitEnter( @Nonnull final DependencyNode node )
  {
    final Dependency dependency = node.getDependency();
    final boolean includeDependency = shouldIncludeDependency( dependency );
    if ( includeDependency )
    {
      if ( !_dependencies.add( dependency ) )
      {
        // Already present in dependencies so we can skip processing
        return false;
      }
      else
      {
        // Remove it from optional dependencies as it is now required dependency
        _optionalDependencies.remove( dependency );
      }
    }
    else if ( !_optionalDependencies.add( dependency ) )
    {
      // Already present in optionalDependencies so we can skip processing
      return false;
    }


    /*
     * If we
     */
    return includeDependency && !hasReplacement( dependency );
  }

  private boolean hasReplacement( @Nonnull final Dependency dependency )
  {
    return _model
      .getReplacements()
      .stream()
      .anyMatch( replacement -> replacement.getGroup().equals( dependency.getArtifact().getGroupId() ) &&
                                replacement.getId().equals( dependency.getArtifact().getArtifactId() ) );
  }

/*
     * Some maven artifacts are replaced, meaning we deal with them and
     * their dependencies manually. If this is true, never follow (but
     * we do add the edges to the node in such cases
    def notReplaced(m: MavenCoordinate): Boolean =
      model.getReplacements
        .get(m.unversioned)
        .isEmpty

 def visitEnter(depNode: DependencyNode): Boolean = {
      logger.info(s"${depNode.getDependency} -> ${depNode.getChildren.asScala.toList.map(_.getDependency)}")
      val dep = depNode.getDependency
      val shouldAdd = addEdgeTo(dep)
      / **
       * unfollowed nodes are distinct from followed nodes.
       * If project a has an optional dependency on b, that does
       * not mean another project does not have a non-optional dependency
       * /
      if (visited((dep, shouldAdd))) {
        logger.info(s"already seen dep: ($dep, $shouldAdd)")
        false
      } else {
        visited = visited + (dep -> shouldAdd)
        val mvncoord = coord(dep)
        if (shouldAdd) {
          logger.info(s"adding dep: ($dep, ${dep.isOptional}, ${dep.getScope})")
          currentDeps = currentDeps.addNode(mvncoord)
        } else {
          logger.info(s"not adding dep: ($dep, ${dep.isOptional}, ${dep.getScope})")
        }
        logger.info(s"path depth: ${stack.size}")
        stack match {
          case Nil =>
            ()
          case h :: _ =>
            val src = coord(h)
            if (shouldAdd && !excludeEdge(src, mvncoord)) {
              logger.info(s"adding edge: $src -> $mvncoord")
              currentDeps = currentDeps.addEdge(Edge(src, mvncoord, ()))
            } else {
              logger.info(s"not adding edge: $src -> $mvncoord")
            }
        }
        stack = dep :: stack
        shouldAdd && notReplaced(mvncoord)
      }
    }
*/

  @Override
  public boolean visitLeave( @Nonnull final DependencyNode node )
  {
    assert _stack.peek() == node.getDependency();
    _stack.pop();
    return true;
  }

  private boolean shouldIncludeDependency( @Nonnull final Dependency dependency )
  {
    if ( dependency.isOptional() )
    {
      return false;
    }
    else
    {
      final String scope = dependency.getScope();
      if ( "".equals( scope ) ||
           Artifact.SCOPE_COMPILE.equals( scope ) ||
           Artifact.SCOPE_RUNTIME.equals( scope ) ||
           Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals( scope ) ||
           Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals( scope ) )
      {
        // Compile => deps
        // Runtime => runtime_deps
        return true;
      }
      else
      {
        assert Artifact.SCOPE_PROVIDED.equals( scope ) ||
               Artifact.SCOPE_TEST.equals( scope ) ||
               Artifact.SCOPE_SYSTEM.equals( scope ) ||
               Artifact.SCOPE_IMPORT.equals( scope );
        return false;
      }
    }
  }
}
