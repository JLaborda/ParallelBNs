package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test for Main Class.
 */
public class MainTest
{


    //Nodes of Cancer Network
    final Node xray = new GraphNode("Xray");
    final Node dyspnoea = new GraphNode("Dyspnoea");
    final Node cancer = new GraphNode ("Cancer");
    final Node pollution = new GraphNode("Pollution");
    final Node smoker = new GraphNode("Smoker");

    final ArrayList<TupleNode> subset1 = new ArrayList<>();
    final ArrayList<TupleNode> subset2 = new ArrayList<>();


    private void initializeSubsets(){
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
     * Testing constructors
     */
    @Test
    public void constructorTest(){
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        DataSet dataset = Main.readData(path);
        int num_cols = 5;

        // Act
        Main main1 = new Main(path, 1);
        Main main2 = new Main(dataset, 1);
        DataSet data1 = main1.getData();
        DataSet data2 = main2.getData();

        // Assert
        assertNotNull(main1);
        assertNotNull(main2);
        assertEquals(num_cols, data1.getNumColumns());
        assertEquals(num_cols, data2.getNumColumns());
    }

    /**
     * Testing csv reading
     */
    @Test
    public void readDataTest(){
        //Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        int num_cols = 5;
        List<Node> columns = new ArrayList<>(Arrays.asList(xray, dyspnoea, cancer, pollution, smoker));
        //Act
        Main main = new Main(path, 1);
        DataSet data = main.getData();
        int result = data.getNumColumns();
        //Assert
        assertEquals(num_cols, result);
        for (Node n: columns) {
            assertTrue(data.getVariableNames().contains(n.getName()));
        }
    }

    @Test(expected = Exception.class)
    public void exceptionReadDataTest(){
        String path = "";
        //noinspection unused
        Main main = new Main(path, 1);
    }

    /**
     * Testing that all the possible arcs from a dataset are generated.
     * We check the size and the equality of the resulting arcs with its expected set.
     */
    @Test
    public void calculateArcsTest(){
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 1);

        TupleNode[] expected = new TupleNode[]{new TupleNode(xray, dyspnoea), new TupleNode(xray, cancer), new TupleNode(xray, pollution), new TupleNode(xray, smoker),
                new TupleNode(dyspnoea, cancer), new TupleNode(dyspnoea, pollution), new TupleNode(dyspnoea, smoker),
                new TupleNode(cancer, pollution), new TupleNode(cancer, smoker),
                new TupleNode(pollution, smoker)};
        // Act
        main.calculateArcs();
        TupleNode[] result =  main.getListOfArcs();

        // Assert
        // Asserting size
        assertEquals(expected.length, result.length);
        for (TupleNode tupleNode1 : expected) {
            boolean isEqual = false;
            for (TupleNode tupleNode2 : result) {
                if (tupleNode1.equals(tupleNode2)) {
                    isEqual = true;
                    break;
                }
            }
            assertTrue(isEqual);
        }
    }

    /**
     * Testing that the arcs have been splitted correctly
     */
    @Test
    public void splitArcsTest(){
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 2);

        // Act
        main.calculateArcs();
        main.splitArcs();
        TupleNode[] arcs = main.getListOfArcs();
        ArrayList<TupleNode>[] subsets = main.getSubSets();

        // Assert
        // Checking that each arc is in fact in a subset, and that it is only once in it.
        for (TupleNode edge : arcs) {
            int counter = 0;
            for (ArrayList<TupleNode> subset : subsets){
                counter += Collections.frequency(subset, edge);
            }
            // Double pairs
            assertEquals(1, counter);
        }


    }

    /**
     * Tests the fes stage for two threads and the subsets of cancer splitted with a seed of 42
     * @throws InterruptedException Exception caused by interruption of the threads.
     */
    @Test
    public void fesStageTest() throws InterruptedException {
        // Arrange
        initializeSubsets();
        // Expectation
        List<Edge> expected1 = new ArrayList<>();
        expected1.add(new Edge(cancer, dyspnoea, Endpoint.TAIL, Endpoint.ARROW));
        expected1.add(new Edge(cancer, xray, Endpoint.TAIL, Endpoint.ARROW));
        expected1.add(new Edge(pollution, cancer, Endpoint.TAIL, Endpoint.ARROW));
        List<Edge> expected2 = new ArrayList<>();
        expected2.add(new Edge(smoker, cancer, Endpoint.TAIL, Endpoint.ARROW));

        // Creating main
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 2);
        main.calculateArcs();
        main.splitArcs();

        // Act
        main.fesStage();
        ArrayList<Dag> resultingGraphs = main.getGraphs();
        Dag gdag1 = resultingGraphs.get(0);
        Dag gdag2 = resultingGraphs.get(1);

        // Assert
        for(Edge edge : expected1){
            assertTrue(gdag1.getEdges().contains(edge));
        }
        for(Edge edge : expected2){
            assertTrue(gdag2.getEdges().contains(edge));
        }
    }


    @Test
    public void setgetMaxIterations(){
        // Arrange
        int expected = 21;
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 1);
        // Act
        main.setMaxIterations(21);
        int actual = main.getMaxIterations();
        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void setgetSeedTest(){
        // Arrange
        long newSeed = 21;
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 1);
        // Act
        main.setSeed(newSeed);
        // Assert
        assertEquals(newSeed, main.getSeed());
    }

    @Test
    public void fusionTest() throws InterruptedException {
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 2);

        List<Node> nodes = new ArrayList<>();
        nodes.add(cancer);
        nodes.add(dyspnoea);
        nodes.add(xray);
        nodes.add(pollution);
        nodes.add(smoker);
        Dag expected = new Dag(nodes);
        expected.addDirectedEdge(cancer, dyspnoea);
        expected.addDirectedEdge(cancer, xray);
        expected.addDirectedEdge(pollution, cancer);
        expected.addDirectedEdge(smoker, cancer);

        // Act
        main.calculateArcs();
        main.splitArcs();
        main.fesStage();
        Dag result = main.fusion();
        System.out.println(result);

        // Assert Nodes
        List<Node> resultingNodes = result.getNodes();
        List<Node> expectedNodes = expected.getNodes();
        for(Node expNode : expectedNodes){
            // System.out.println("Expected Node: " + expNode.getName());
            boolean assertion = false;
            for(Node resNode: resultingNodes){
                // System.out.println("Resulting Node: " + resNode.getName());
                if(expNode.getName().equals(resNode.getName())){
                    assertion = true;
                    break;
                }
            }
            assertTrue(assertion);
        }

        // Assert Edges
        List<Edge> resultingEdges = result.getEdges();
        for(Edge resEdge : resultingEdges){
            Node node1 = resEdge.getNode1();
            Node node2 = resEdge.getNode2();

            // System.out.println("Node1: " + node1.getName());
            // System.out.println("Node2: " + node2.getName());

            if(node1.getName().equals("Cancer")){
                String node2Name = node2.getName();
                assertTrue(((node2Name.equals("Dyspnoea")) || (node2Name.equals("Xray")) ));
                continue;
            }
            if(node1.getName().equals("Pollution")){
                assertEquals("Cancer", node2.getName());
                continue;
            }
            if(node1.getName().equals("Smoker")){
                assertEquals("Cancer", node2.getName());
                continue;
            }
            // If node1 i not any of these nodes, then assert error
            fail("Node1 is not in the expected range.");

        }

    }

    @Test
    public void besStageTest() throws InterruptedException {
        // Arrange
        initializeSubsets();
        // Expectation
        List<Edge> expected = new ArrayList<>();
        expected.add(new Edge(cancer, dyspnoea, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(cancer, xray, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(pollution, cancer, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(smoker, cancer, Endpoint.TAIL, Endpoint.ARROW));

        // Creating main
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 2);
        main.calculateArcs();
        main.splitArcs();
        main.fesStage();
        main.fusion();

        // Act
        main.besStage();
        ArrayList<Dag> results = main.getGraphs();

        // Assert
        for(Dag graph : results){
            for(Edge edge : expected) {
                assertTrue(graph.getEdges().contains(edge));
            }
        }
    }
}
