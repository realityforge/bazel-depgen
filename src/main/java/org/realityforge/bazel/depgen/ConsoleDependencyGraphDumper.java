/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.realityforge.bazel.depgen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

/**
 * A dependency visitor that dumps the graph to the console.
 */
final class ConsoleDependencyGraphDumper
  implements DependencyVisitor
{
  private final List<ChildInfo> _childInfos = new ArrayList<>();
  @Nonnull
  private final Logger _logger;

  ConsoleDependencyGraphDumper( @Nonnull final Logger logger )
  {
    _logger = Objects.requireNonNull( logger );
  }

  @Override
  public boolean visitEnter( DependencyNode node )
  {
    _logger.info( formatIndentation() + formatNode( node ) );
    _childInfos.add( new ChildInfo( node.getChildren().size() ) );
    return true;
  }

  @Nonnull
  private String formatIndentation()
  {
    final StringBuilder buffer = new StringBuilder( 128 );
    for ( final Iterator<ChildInfo> it = _childInfos.iterator(); it.hasNext(); )
    {
      buffer.append( it.next().formatIndentation( !it.hasNext() ) );
    }
    return buffer.toString();
  }

  @Nonnull
  private String formatNode( @Nonnull final DependencyNode node )
  {
    final StringBuilder buffer = new StringBuilder( 128 );
    final Artifact a = node.getArtifact();
    final Dependency d = node.getDependency();
    buffer.append( a );
    if ( null != d && d.getScope().length() > 0 )
    {
      buffer.append( " [" ).append( d.getScope() );
      if ( d.isOptional() )
      {
        buffer.append( ", optional" );
      }
      buffer.append( "]" );
    }
    {
      final String premanaged = DependencyManagerUtils.getPremanagedVersion( node );
      if ( null != premanaged && !premanaged.equals( a.getBaseVersion() ) )
      {
        buffer.append( " (version managed from " ).append( premanaged ).append( ")" );
      }
    }
    {
      final String premanaged = DependencyManagerUtils.getPremanagedScope( node );
      if ( null != premanaged && null != d && !premanaged.equals( d.getScope() ) )
      {
        buffer.append( " (scope managed from " ).append( premanaged ).append( ")" );
      }
    }
    final DependencyNode winner = (DependencyNode) node.getData().get( ConflictResolver.NODE_DATA_WINNER );
    if ( winner != null && !ArtifactIdUtils.equalsId( a, winner.getArtifact() ) )
    {
      final Artifact w = winner.getArtifact();
      buffer.append( " (conflicts with " );
      if ( ArtifactIdUtils.toVersionlessId( a ).equals( ArtifactIdUtils.toVersionlessId( w ) ) )
      {
        buffer.append( w.getVersion() );
      }
      else
      {
        buffer.append( w );
      }
      buffer.append( ")" );
    }
    return buffer.toString();
  }

  @Override
  public boolean visitLeave( @Nonnull final DependencyNode node )
  {
    if ( !_childInfos.isEmpty() )
    {
      _childInfos.remove( _childInfos.size() - 1 );
    }
    if ( !_childInfos.isEmpty() )
    {
      _childInfos.get( _childInfos.size() - 1 )._index++;
    }
    return true;
  }

  private static class ChildInfo
  {
    final int _count;
    int _index;

    ChildInfo( final int count )
    {
      this._count = count;
    }

    String formatIndentation( final boolean end )
    {
      final boolean last = _index + 1 >= _count;
      if ( end )
      {
        return last ? "\\- " : "+- ";
      }
      else
      {
        return last ? "   " : "|  ";
      }
    }
  }
}
