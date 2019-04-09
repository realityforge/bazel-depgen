package org.realityforge.bazel.depgen.util;

import java.util.Collection;
import java.util.NoSuchElementException;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DirectedGraphTest
{
  @Test
  public void addNode()
  {
    final DirectedGraph<String, Integer> graph = new DirectedGraph<>();
    assertTrue( graph.getNodes().isEmpty() );

    final String nodeElement = ValueUtil.randomString();

    assertTrue( graph.addNode( nodeElement ) );

    final Collection<Node<String, Integer>> nodes = graph.getNodes();
    assertEquals( nodes.size(), 1 );
    final Node<String, Integer> node = nodes.iterator().next();
    assertEquals( node.getElement(), nodeElement );
    assertTrue( node.getIncomingEdges().isEmpty() );
    assertTrue( node.getIncomingNodes().isEmpty() );
    assertTrue( node.getOutgoingEdges().isEmpty() );
    assertTrue( node.getOutgoingNodes().isEmpty() );
  }

  @Test
  public void addNode_whenNodeExists()
  {
    final DirectedGraph<String, Integer> graph = new DirectedGraph<>();
    assertTrue( graph.getNodes().isEmpty() );

    final String nodeElement = ValueUtil.randomString();

    assertTrue( graph.addNode( nodeElement ) );

    assertEquals( graph.getNodes().size(), 1 );

    assertFalse( graph.addNode( nodeElement ) );

    assertEquals( graph.getNodes().size(), 1 );
  }

  @Test
  public void containsNode()
  {
    final DirectedGraph<String, Integer> graph = new DirectedGraph<>();
    assertTrue( graph.getNodes().isEmpty() );

    final String nodeElement = ValueUtil.randomString();

    assertFalse( graph.containsNode( nodeElement ) );

    assertTrue( graph.addNode( nodeElement ) );

    assertTrue( graph.containsNode( nodeElement ) );
  }

  @Test
  public void findNodeByElement()
  {
    final DirectedGraph<String, Integer> graph = new DirectedGraph<>();
    assertTrue( graph.getNodes().isEmpty() );

    final String nodeElement = ValueUtil.randomString();

    assertNull( graph.findNodeByElement( nodeElement ) );

    assertTrue( graph.addNode( nodeElement ) );

    assertNotNull( graph.findNodeByElement( nodeElement ) );
  }

  @Test
  public void getNodeByElement()
  {
    final DirectedGraph<String, Integer> graph = new DirectedGraph<>();
    assertTrue( graph.getNodes().isEmpty() );

    final String nodeElement = ValueUtil.randomString();

    assertThrows( NoSuchElementException.class, () -> graph.getNodeByElement( nodeElement ) );

    assertTrue( graph.addNode( nodeElement ) );

    assertEquals( graph.getNodeByElement( nodeElement ).getElement(), nodeElement );
  }

  @Test
  public void addEdge()
  {
    final DirectedGraph<String, Integer> graph = new DirectedGraph<>();
    assertTrue( graph.getNodes().isEmpty() );

    final String n1 = ValueUtil.randomString();
    final String n2 = ValueUtil.randomString();
    final Integer e = ValueUtil.randomInt();

    assertTrue( graph.addNode( n1 ) );
    assertTrue( graph.addNode( n2 ) );
    assertTrue( graph.addEdge( e, n1, n2 ) );

    assertEquals( graph.getNodes().size(), 2 );

    final Node<String, Integer> node1 = graph.getNodeByElement( n1 );
    assertEquals( node1.getIncomingEdges().size(), 0 );
    assertEquals( node1.getIncomingNodes().size(), 0 );
    assertEquals( node1.getOutgoingEdges().size(), 1 );
    assertEquals( node1.getOutgoingNodes().size(), 1 );

    final Node<String, Integer> node2 = graph.getNodeByElement( n2 );
    assertEquals( node2.getIncomingEdges().size(), 1 );
    assertEquals( node2.getIncomingNodes().size(), 1 );
    assertEquals( node2.getOutgoingEdges().size(), 0 );
    assertEquals( node2.getOutgoingNodes().size(), 0 );

    assertTrue( node1.getOutgoingNodes().contains( node2 ) );
    assertTrue( node2.getIncomingNodes().contains( node1 ) );

    final Edge<String, Integer> edge = node1.getOutgoingEdges().iterator().next();
    assertEquals( edge, node2.getIncomingEdges().iterator().next() );
    assertEquals( edge.getElement(), e );
    assertEquals( edge.getSource(), node1 );
    assertEquals( edge.getDestination(), node2 );
  }
}
