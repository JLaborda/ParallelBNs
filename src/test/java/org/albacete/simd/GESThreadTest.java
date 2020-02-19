package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for the GESThread class
 */
@SuppressWarnings({"SpellCheckingInspection", "SuspiciousNameCombination"})
public class GESThreadTest {

    /**
     * String containing the path to the data used in the test. The data used in these tests is made by sampling the
     * cancer Bayesian Network @see
     * <a href="https://www.bnlearn.com/bnrepository/discrete-small.html">https://www.bnlearn.com/bnrepository/discrete-small.html</a>
     */
    final String path = "src/test/resources/cancer.xbif_.csv";
    /**
     * Dataset created from the data file
     */
    final DataSet dataset = Utils.readData(path);
    /**
     * Variable X-Ray
     */
    final Node xray = dataset.getVariable("Xray");
    /**
     * Variable Dysponea
     */
    final Node dyspnoea = dataset.getVariable("Dyspnoea");
    /**
     * Variabe Cancer
     */
    final Node cancer = dataset.getVariable("Cancer");
    /**
     * Variable Pollution
     */
    final Node pollution = dataset.getVariable("Pollution");
    /**
     * Variable Smoker
     */
    final Node smoker = dataset.getVariable("Smoker");

    /**
     * Subset1 of pairs of nodes or variables.
     */
    final ArrayList<TupleNode> subset1 = new ArrayList<>();
    /**
     * Subset2 of pairs of nodes or variables.
     */
    final ArrayList<TupleNode> subset2 = new ArrayList<>();


    /**
     * Constructor of the test. It initializes the subsets
     */
    public GESThreadTest() {
        initializeSubsets();
    }

    /**
     * This method initializes the subsets, splitting the nodes in what is expected to happen when the seed is 42
     */
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

    /**
     * Testing the insert method.
     * @result insert(X,Y,S,G) adds {X-Y, A-Y, B-Y} to the graph (in this case S is composed by two nodes A and B)
     */
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

    /**
     * Testing that we get the correct neighbors of nodes for Y that are not adjacent to X. Directed nodes should not be added
     * @result Y should only have as neighbors nodes Z and T
     */
    @Test
    public void getSubsetOfUndirectedNeighborsTest(){
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
        g.addUndirectedEdge(P,X);
        g.addUndirectedEdge(X,Y);
        g.addUndirectedEdge(Y,Z);
        g.addDirectedEdge(Y,M);
        g.addUndirectedEdge(Y,T);
        g.addUndirectedEdge(Y,P);

        List<Node> expected = new ArrayList<>();
        expected.add(Z);
        expected.add(T);

        // Act
        List<Node> result = GESThread.getSubsetOfNeighbors(X,Y,g);

        // Assert
        for(Node n : expected) {
            assertTrue(result.contains(n));
        }
        assertFalse(result.contains(P));
    }

    /**
     * Testing that we get no neighbors for only one node
     * @result An empty set of neighbors.
     */
    @Test
    public void getEmptySubsetOfNeighborsTest(){
        // Arrange
        // Nodes
        Node X = new GraphNode("X");
        Node Y = new GraphNode("Y");
        List<Node> nodes = new ArrayList<>();
        nodes.add(X);
        nodes.add(Y);
        // Graph
        Graph g = new EdgeListGraph(nodes);
        // Act
        List<Node> result = GESThread.getSubsetOfNeighbors(X,Y,g);
        assertEquals(0, result.size());
    }


    /**
     * Testing that in the graph X-Y-Z, when delete(X,Y,{Z}) is executed, the resulting edges are two directed edges X-Z
     * and Y-Z.
     * @result A graph with X-Y deleted and two directed edges added: X-Z and Y-Z
     */
    @Test
    public void deleteTest(){
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

    /**
     * Tests that for the graph {X-Z, X-T, Z-Y, T-Y (directed)} the nodes connected to Y by an undirected edge adjacent
     * to X is only Z.
     * @result A list with only the Node Z
     */
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

    /**
     * Tests that the isClique detects cliques correctly both true cliques and false cliques
     * @result true for the graph that has a clique, false for the graph that doesn't have one.
     */
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

    /**
     * Tests that the isSemiDirectedBlocked function returns true when the list has the Y Node
     * @result This test should be true
     */
    @Test
    public void ifNaYXTListContainsYisSemiDirectedBlockedShouldBeTrueTest(){
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

    /**
     * Tests that the isSemiDirectedBlocked function returns true when X and Y are the same.
     * @result This test should be true
     */
    @Test
    public void ifNaYXTListContainsOnlyXisSemiDirectedBlockedShouldBeTrueTest(){
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

    /**
     * Tests that for all the paths from X and Y contain a node from the list naYXT.
     * @result Test should be true
     */
    @Test
    public void isSemiDirectedBlockedShouldBeTrueForTest(){
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

    /**
     * Tests that there is a path from X and Y that doesn't contain a node from the list naYXT.
     * @result Test should be false
     */

    @Test
    public void isSemiDirectedBlockedShouldBeFalseTest(){
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

    /**
     * Testing that a buildingIndexing is built when asked to.
     * @result Test should be true
     */
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


    /**
     * Tests that the maximum number of edges is modified.
     * @result The max number of edges is changed to 5
     */
    @Test
    public void setMaxNumEdgesNormalTest(){
        // Arrange
        GESThread thread = new ThFES(dataset, subset1, 15);

        // Act
        thread.setMaxNumEdges(5);

        // Assert
        assertEquals(5, thread.getMaxNumEdges());
    }

    /**
     * Tests that if a negative number is set as the maximum number of edges, an IllegalArgumentException is thrown
     * @result An IllegalArgumenException is thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void setMaxNumEdgesErrorTest(){
        // Arrange
        GESThread thread = new ThFES(dataset, subset1, 15);

        // Act
        thread.setMaxNumEdges(-2);

        //Assert
        fail();
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


