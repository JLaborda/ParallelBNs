package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@SuppressWarnings({"SpellCheckingInspection", "SuspiciousNameCombination"})
public class GESThreadTest {

    final String path = "src/test/resources/cancer.xbif_.csv";
    final DataSet dataset = Main.readData(path);
    final Node xray = dataset.getVariable("Xray");
    final Node dyspnoea = dataset.getVariable("Dyspnoea");
    final Node cancer = dataset.getVariable("Cancer");
    final Node pollution = dataset.getVariable("Pollution");
    final Node smoker = dataset.getVariable("Smoker");

    final ArrayList<TupleNode> subset1 = new ArrayList<>();
    final ArrayList<TupleNode> subset2 = new ArrayList<>();


    public GESThreadTest() {
        initializeSubsets();
    }


    private void initializeSubsets() {
        // Seed used for arc split is 42

        // Subset 1:
        subset1.add(new TupleNode(dyspnoea, cancer));
        subset1.add(new TupleNode(dyspnoea, smoker));
        subset1.add(new TupleNode(xray, pollution));
        subset1.add(new TupleNode(xray, cancer));
        subset1.add(new TupleNode(cancer, pollution));


        //Subset 2:
        subset2.add(new TupleNode(pollution, smoker));
        subset2.add(new TupleNode(cancer, smoker));
        subset2.add(new TupleNode(dyspnoea, pollution));
        subset2.add(new TupleNode(xray, smoker));
        subset2.add(new TupleNode(xray, dyspnoea));

    }

    @Test
    public void insertTest(){
        // Arrange
        List<Node> nodes = new ArrayList<>();
        nodes.add(dyspnoea);
        nodes.add(smoker);
        nodes.add(xray);
        nodes.add(cancer);
        nodes.add(pollution);

        Graph g = new Dag(nodes);
        Set<Node> setNode = new HashSet<>();
        setNode.add(dyspnoea);
        setNode.add(smoker);

        //Expectations
        Edge edge1 = new Edge(cancer,xray, Endpoint.TAIL, Endpoint.ARROW);
        Edge edge2 = new Edge(dyspnoea,xray, Endpoint.TAIL, Endpoint.ARROW);
        Edge edge3 = new Edge(smoker,xray, Endpoint.TAIL, Endpoint.ARROW);


        // Act
        GESThread.insert(cancer,xray,setNode,g);

        // Assert
        assertTrue(g.getEdges().contains(edge1));
        assertTrue(g.getEdges().contains(edge2));
        assertTrue(g.getEdges().contains(edge3));
    }

    @Test
    public void getSubsetOfNeighborsTest(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node P = new GraphNode("P");
        Node Z = new GraphNode("Z");
        Node M = new GraphNode("M");
        Node T = new GraphNode("T");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(P);
        nodes.add(Z);
        nodes.add(M);
        nodes.add(T);
        // Graph
        Graph g = new EdgeListGraph(nodes);

        // Edges
        g.addUndirectedEdge(X,P);
        g.addUndirectedEdge(X,Y);
        g.addUndirectedEdge(Y,Z);
        g.addDirectedEdge(Y,M);
        g.addUndirectedEdge(Y,T);

        List<Node> expected = new ArrayList<>();
        expected.add(Z);
        expected.add(T);

        // Act
        List<Node> result = GESThread.getSubsetOfNeighbors(X,Y,g);


        // Assert
        for(Node n : expected){
            assertTrue(result.contains(n));
        }
    }

    @Test
    public void delete1Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        // Graph
        Graph g = new EdgeListGraph(nodes);

        // Edges
        g.addUndirectedEdge(X,Y);
        g.addUndirectedEdge(Y,Z);

        Set<Node> subset = new HashSet<>();
        subset.add(Z);

        Graph expected = new EdgeListGraph(nodes);
        expected.addDirectedEdge(X,Z);
        expected.addDirectedEdge(Y,Z);


        // Act
        GESThread.delete(X,Y,subset,g);

        // Assert
        for(Edge edge : expected.getEdges()){
            assertTrue(g.getEdges().contains(edge));
            Edge res_edge = g.getEdge(edge.getNode1(), edge.getNode2());
            assertEquals(res_edge, edge);
        }
    }


    @Test
    public void delete2Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        // Graph
        Graph g = new EdgeListGraph(nodes);

        // Edges
        g.addUndirectedEdge(X,Y);
        g.addUndirectedEdge(X,Z);

        Set<Node> subset = new HashSet<>();
        subset.add(Z);

        Graph expected = new EdgeListGraph(nodes);
        expected.addDirectedEdge(X,Z);
        expected.addDirectedEdge(Y,Z);


        // Act
        GESThread.delete(X,Y,subset,g);

        // Assert

        // Assert
        for(Edge edge : expected.getEdges()){
            assertTrue(g.getEdges().contains(edge));
            Edge res_edge = g.getEdge(edge.getNode1(), edge.getNode2());
            assertEquals(res_edge, edge);
        }
    }

    @Test
    public void delete3Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        // Graph
        Graph g = new EdgeListGraph(nodes);

        // Edges
        g.addUndirectedEdge(X,Y);
        g.addDirectedEdge(X,Z);

        Set<Node> subset = new HashSet<>();
        subset.add(Z);

        Graph expected = new EdgeListGraph(nodes);
        expected.addDirectedEdge(X,Z);
        expected.addDirectedEdge(Y,Z);


        // Act
        GESThread.delete(X,Y,subset,g);

        // Assert
        for(Edge edge : expected.getEdges()){
            assertTrue(g.getEdges().contains(edge));
            Edge res_edge = g.getEdge(edge.getNode1(), edge.getNode2());
            assertEquals(res_edge, edge);
        }
    }

    @Test
    public void delete4Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        // Graph
        Graph g = new EdgeListGraph(nodes);

        // Edges
        g.addUndirectedEdge(X,Y);
        g.addDirectedEdge(Z,X);

        Set<Node> subset = new HashSet<>();
        subset.add(Z);

        Graph expected = new EdgeListGraph(nodes);
        expected.addDirectedEdge(Z,X);
        expected.addDirectedEdge(Y,Z);


        // Act
        GESThread.delete(X,Y,subset,g);

        // Assert
        for(Edge edge : expected.getEdges()){
            assertTrue(g.getEdges().contains(edge));
            Edge res_edge = g.getEdge(edge.getNode1(), edge.getNode2());
            assertEquals(res_edge, edge);
        }
    }


    @Test
    public void findNaYXTest(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        Node T = new GraphNode("T");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        nodes.add(T);

        // Graph
        Graph g = new EdgeListGraph(nodes);

        // Edges
        g.addUndirectedEdge(X,Z);
        g.addUndirectedEdge(X,T);
        g.addUndirectedEdge(Z,Y);
        g.addDirectedEdge(T,Y);

        List<Node> expected = new ArrayList<>();
        expected.add(Z);

        // Act
        List<Node> result = GESThread.findNaYX(X,Y,g);

        // Assert
        for(Node n : expected){
            assertTrue(result.contains(n));
        }

    }

    @Test
    public void isCliqueTest(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        Node T = new GraphNode("T");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        nodes.add(T);

        // Graph
        Graph g1 = new EdgeListGraph(nodes);
        Graph g2 = new EdgeListGraph(nodes);

        // Edges
        g1.addUndirectedEdge(X,Y);
        g1.addUndirectedEdge(X,T);
        g1.addUndirectedEdge(X,Z);
        g1.addUndirectedEdge(Y,Z);
        g1.addUndirectedEdge(Y,T);
        g1.addUndirectedEdge(Z,T);

        g2.addUndirectedEdge(X,Y);
        g2.addUndirectedEdge(X,T);
        g2.addUndirectedEdge(X,Z);
        g2.addUndirectedEdge(Y,Z);
        g2.addUndirectedEdge(Y,T);

        // Act
        boolean result1 = GESThread.isClique(nodes, g1);
        boolean result2 = GESThread.isClique(nodes, g2);
        // Assert
        assertTrue(result1);
        assertFalse(result2);
    }

    @Test
    public void isSemiDirectedBlocked1Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        Node T = new GraphNode("T");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        nodes.add(T);

        // Graph
        Graph g = new EdgeListGraph(nodes);
        List<Node> naYXT = new ArrayList<>();
        naYXT.add(Y);

        // Act
        boolean result = GESThread.isSemiDirectedBlocked(X, Y, naYXT, g, null);

        // Assert
        assertTrue(result);
    }

    @Test
    public void isSemiDirectedBlocked2Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        Node T = new GraphNode("T");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        nodes.add(T);

        // Graph
        Graph g = new EdgeListGraph(nodes);
        List<Node> naYXT = new ArrayList<>();
        naYXT.add(Y);

        // Act
        boolean result = GESThread.isSemiDirectedBlocked(X, X, naYXT, g, null);

        // Assert
        assertFalse(result);
    }

    @Test
    public void isSemiDirectedBlocked3Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        Node T = new GraphNode("T");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        nodes.add(T);

        // Graph
        Graph g = new EdgeListGraph(nodes);
        g.addUndirectedEdge(X,Z);
        g.addUndirectedEdge(Z,T);
        g.addUndirectedEdge(T,Y);


        List<Node> naYXT = new ArrayList<>();
        naYXT.add(Z);
        naYXT.add(T);

        // Act
        boolean result = GESThread.isSemiDirectedBlocked(X, Y, naYXT, g, new HashSet<>());

        // Assert
        assertTrue(result);
    }

    @Test
    public void isSemiDirectedBlocked4Test(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        Node Z = new GraphNode("Z");
        Node T = new GraphNode("T");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        nodes.add(Z);
        nodes.add(T);

        // Graph
        Graph g = new EdgeListGraph(nodes);
        g.addUndirectedEdge(X,Z);
        g.addUndirectedEdge(Z,T);
        g.addUndirectedEdge(T,Y);

        List<Node> naYXT = new ArrayList<>();
        //naYXT.add(Z);

        // Act
        boolean result = GESThread.isSemiDirectedBlocked(X, Y, naYXT, g, new HashSet<>());

        // Assert
        assertFalse(result);
    }


    @Test
    public void buildIndexingNotBuiltTest(){
        // Arrange
        GESThread thread = new ThFES(dataset, subset1, 15);
        thread.varNames = new String[0];

        // Act
        thread.buildIndexing(thread.initialDag);

        // Assert
        assertTrue(thread.hashIndices.isEmpty());
    }


    @Test
    public void setMaxNumEdgesNormalTest(){
        // Arrange
        GESThread thread = new ThFES(dataset, subset1, 15);

        // Act
        thread.setMaxNumEdges(5);

        // Assert
        assertEquals(5, thread.getMaxNumEdges());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMaxNumEdgesErrorTest(){
        // Arrange
        GESThread thread = new ThFES(dataset, subset1, 15);

        // Act
        thread.setMaxNumEdges(-2);
    }


}












    // Not working. Make Issue for this test
    /*
    @Test
    public void getFlagTest() throws InterruptedException {

        // Arrange
        GESThread gesThread = new ThFES(dataset, subset1,15);
        ThreadTest threadTest = new ThreadTest(gesThread);
        Thread thread = new Thread(threadTest);
        // Act
        thread.run();
        Graph g = new Dag();
        gesThread.currentGraph = g;
        boolean flag = threadTest.getFlag();

        // Assert
        assertFalse(flag);
    }

    private class ThreadTest implements Runnable{

        GESThread gesThread;
        boolean flag = false;
        ThreadTest(GESThread gesThread){
            this.gesThread = gesThread;
        }

        @Override
        public void run() {
            try {
                flag = this.gesThread.getFlag();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean getFlag() throws InterruptedException {
            this.flag = this.gesThread.getFlag();
            return this.flag;
        }
    }
    */


