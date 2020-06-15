package org.albacete.simd.algorithms.pGESv2;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.utils.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test for Main Class.
 */
public class PGESv2Test
{
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
     * This method initializes the subsets, splitting the nodes in what is expected to happen when the seed is 42
     */
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
     * Testing both possible constructors of the Main class
     * @result Both objects should have the same dataset stored in it.
     */
    @Test
    public void constructorTest(){
        // Arrange
        int num_cols = 5;

        // Act
        PGESv2 PGESv21 = new PGESv2(path, 1);
        PGESv2 PGESv22 = new PGESv2(dataset, 1);
        DataSet data1 = PGESv21.getData();
        DataSet data2 = PGESv22.getData();

        // Assert
        assertNotNull(PGESv21);
        assertNotNull(PGESv22);
        assertEquals(num_cols, data1.getNumColumns());
        assertEquals(num_cols, data2.getNumColumns());
    }

    /**
     * Testing that the csv read is correct
     * @result The resulting DataSet should be the one corresponding to the cancer DataSet.
     */
    @Test
    public void readDataTest(){
        //Arrange
        int num_cols = 5;
        List<Node> columns = new ArrayList<>(Arrays.asList(xray, dyspnoea, cancer, pollution, smoker));
        //Act
        PGESv2 pGESv2 = new PGESv2(path, 1);
        DataSet data = pGESv2.getData();
        int result = data.getNumColumns();
        //Assert
        assertEquals(num_cols, result);
        for (Node n: columns) {
            assertTrue(data.getVariableNames().contains(n.getName()));
        }
    }

    /**
     * When an incorrect path is passed to the Main constructor, an exception should be thrown
     * @result An exception is thrown caused by an incorrect path.
     */
    @Test(expected = Exception.class)
    public void exceptionReadDataTest(){
        String path = "";
        //noinspection unused
        PGESv2 pGESv2 = new PGESv2(path, 1);
        fail();
    }

    /**
     * Testing that all the possible arcs from a dataset are generated.
     * We check the size and the equality of the resulting arcs with its expected set.
     * @result The arcs should be the same as the expected set of TupleNodes.
     */
    @Test
    public void calculateArcsTest(){
        // Arrange
        PGESv2 pGESv2 = new PGESv2(path, 1);

        TupleNode[] expected = new TupleNode[]{new TupleNode(xray, dyspnoea), new TupleNode(xray, cancer), new TupleNode(xray, pollution), new TupleNode(xray, smoker),
                new TupleNode(dyspnoea, cancer), new TupleNode(dyspnoea, pollution), new TupleNode(dyspnoea, smoker),
                new TupleNode(cancer, pollution), new TupleNode(cancer, smoker),
                new TupleNode(pollution, smoker)};
        // Act
        pGESv2.calculateArcs();
        TupleNode[] result =  pGESv2.getListOfArcs();

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
     * Testing that the arcs have been divided correctly.
     * @result Each arc is only once in a subset.
     */
    @Test
    public void splitArcsTest(){
        // Arrange
        PGESv2 pGESv2 = new PGESv2(path, 2);

        // Act
        pGESv2.calculateArcs();
        pGESv2.splitArcs();
        TupleNode[] arcs = pGESv2.getListOfArcs();
        ArrayList<TupleNode>[] subsets = pGESv2.getSubSets();

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
     * Tests the fes stage for two threads with the subsets of cancer divided into two, with a seed equal to 42.
     * @throws InterruptedException Exception caused by interruption of the threads.
     * @result The FES stage executes correctly, and the resulting graphs are the same all the times.
     *
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
        PGESv2 pGESv2 = new PGESv2(path, 2);
        pGESv2.calculateArcs();
        pGESv2.splitArcs();

        // Act
        pGESv2.fesStage();
        ArrayList<Dag> resultingGraphs = pGESv2.getGraphs();
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


    /**
     * Testing the setter and getter of MaxIterations
     * @result MaxIterations is changed to 21 and obtained as such.
     */
    @Test
    public void setgetMaxIterations(){
        // Arrange
        int expected = 21;
        PGESv2 pGESv2 = new PGESv2(path, 1);
        // Act
        pGESv2.setMaxIterations(21);
        int actual = pGESv2.getMaxIterations();
        // Assert
        assertEquals(expected, actual);
    }

    /**
     * Testing the setter and getter of Seed
     * @result Seed is changed to 21 and obtained as such.
     */
    @Test
    public void setgetSeedTest(){
        // Arrange
        long newSeed = 21;
        PGESv2 pGESv2 = new PGESv2(path, 1);
        // Act
        pGESv2.setSeed(newSeed);
        // Assert
        assertEquals(newSeed, pGESv2.getSeed());
    }

    /**
     * Testing the fusion method. The fusion should combine the graphs generated in the FES stage into a single DAG.
     * @result The resulting Graph should have the same nodes and edges as the expected graph.
     * @throws InterruptedException Caused by an external interruption.
     */
    @Test
    public void fusionTest() throws InterruptedException {
        // Arrange
        PGESv2 pGESv2 = new PGESv2(path, 2);

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
        pGESv2.calculateArcs();
        pGESv2.splitArcs();
        pGESv2.fesStage();
        Dag result = pGESv2.fusion();
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

    /**
     * Tests that the bes stage works, and in this case, it doesn't delete any edge added in the fes stage.
     * @result The bes stage doesn't delete any edge from the resulting fusion graph.
     * @throws InterruptedException External interruption.
     */
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
        PGESv2 pGESv2 = new PGESv2(path, 2);
        pGESv2.calculateArcs();
        pGESv2.splitArcs();
        pGESv2.fesStage();
        pGESv2.fusion();

        // Act
        pGESv2.besStage();
        ArrayList<Dag> results = pGESv2.getGraphs();

        // Assert
        for(Dag graph : results){
            for(Edge edge : expected) {
                assertTrue(graph.getEdges().contains(edge));
            }
        }
    }

    /**
     * Tests the search method of the Main class.
     * @result The resulting graph is equal to the expected graph for the cancer dataset.
     */
    @Test
    public void searchCancerTest(){
        //Arrange
        PGESv2 pGESv2 = new PGESv2(path, 2);

        //Expectation
        List<Node> nodes = Arrays.asList(cancer, dyspnoea, pollution, xray, smoker);
        EdgeListGraph expectation = new EdgeListGraph(nodes);
        expectation.addDirectedEdge(cancer, dyspnoea);
        expectation.addDirectedEdge(cancer, xray);
        expectation.addDirectedEdge(pollution, cancer);
        expectation.addDirectedEdge(smoker, cancer);

        // Act
        pGESv2.search();

        //Assert
        assertEquals(expectation, pGESv2.getCurrentGraph());

    }

    /**
     * Executes the main function in order to see that everything is working, and that no exceptions are being thrown.
     * @result No exception is thrown.
     */
    @Test
    public void mainExecutesTest(){
        PGESv2.main(null);
    }

}
