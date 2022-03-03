package org.albacete.simd.algorithms;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.Resources;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.albacete.simd.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit test for Main Class.
 */
public class PGESv2Test
{


    /**
     * Subset1 of pairs of nodes or variables.
     */
    final ArrayList<Edge> subset1 = new ArrayList<>();
    /**
     * Subset2 of pairs of nodes or variables.
     */
    final ArrayList<Edge> subset2 = new ArrayList<>();


    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    /**
     * This method initializes the subsets, splitting the nodes in what is expected to happen when the seed is 42
     */
    private void initializeSubsets(){
        // Seed used for arc split is 42

        // Subset 1:
        subset1.add(Edges.directedEdge(Resources.DYSPNOEA, Resources.CANCER));
        subset1.add(Edges.directedEdge(Resources.CANCER, Resources.DYSPNOEA));
        subset1.add(Edges.directedEdge(Resources.DYSPNOEA, Resources.SMOKER));
        subset1.add(Edges.directedEdge(Resources.SMOKER, Resources.DYSPNOEA));
        subset1.add(Edges.directedEdge(Resources.XRAY, Resources.POLLUTION));
        subset1.add(Edges.directedEdge(Resources.POLLUTION, Resources.XRAY));
        subset1.add(Edges.directedEdge(Resources.XRAY , Resources.CANCER));
        subset1.add(Edges.directedEdge(Resources.CANCER, Resources.XRAY));
        subset1.add(Edges.directedEdge(Resources.CANCER, Resources.POLLUTION));
        subset1.add(Edges.directedEdge(Resources.POLLUTION, Resources.CANCER));

        //Subset 2:
        subset2.add(Edges.directedEdge(Resources.POLLUTION, Resources.SMOKER));
        subset2.add(Edges.directedEdge(Resources.SMOKER, Resources.POLLUTION));
        subset2.add(Edges.directedEdge(Resources.CANCER, Resources.SMOKER));
        subset2.add(Edges.directedEdge(Resources.SMOKER, Resources.CANCER));
        subset2.add(Edges.directedEdge(Resources.DYSPNOEA, Resources.POLLUTION));
        subset2.add(Edges.directedEdge(Resources.POLLUTION, Resources.DYSPNOEA));
        subset2.add(Edges.directedEdge(Resources.XRAY, Resources.SMOKER));
        subset2.add(Edges.directedEdge(Resources.SMOKER, Resources.XRAY));
        subset2.add(Edges.directedEdge(Resources.XRAY, Resources.DYSPNOEA));
        subset2.add(Edges.directedEdge(Resources.DYSPNOEA, Resources.XRAY));

    }

    /**
     * Testing both possible constructors of the Main class
     * @result Both objects should have the same dataset stored in it.
     */
    @Test
    public void constructorAndGettersTest(){
        // Arrange
        int num_cols = 5;

        // Act
        PGESv2 PGESv21 = new PGESv2(Resources.CANCER_BBDD_PATH, 1);
        PGESv2 PGESv22 = new PGESv2(Resources.CANCER_DATASET, 2);
        PGESv2 PGESv23 = new PGESv2(Resources.CANCER_BBDD_PATH,4, 30, 8);
        PGESv2 PGESv24 = new PGESv2(Resources.CANCER_DATASET,8, 35, 10);

        DataSet data1 = PGESv21.getData();
        DataSet data2 = PGESv22.getData();
        DataSet data3 = PGESv23.getData();
        DataSet data4 = PGESv24.getData();

        int threads1 = PGESv21.getnThreads();
        int threads2 = PGESv22.getnThreads();
        int threads3 = PGESv23.getnThreads();
        int threads4 = PGESv24.getnThreads();

        int maxIterations1 = PGESv21.getMaxIterations();
        int maxIterations2 = PGESv22.getMaxIterations();
        int maxIterations3 = PGESv23.getMaxIterations();
        int maxIterations4 = PGESv24.getMaxIterations();

        int interleaving1 = PGESv21.getnFESItInterleaving();
        int interleaving2 = PGESv22.getnFESItInterleaving();
        int interleaving3 = PGESv23.getnFESItInterleaving();
        int interleaving4 = PGESv24.getnFESItInterleaving();

        // Assert
        assertNotNull(PGESv21);
        assertNotNull(PGESv22);
        assertNotNull(PGESv23);
        assertNotNull(PGESv24);
        assertEquals(num_cols, data1.getNumColumns());
        assertEquals(num_cols, data2.getNumColumns());
        assertEquals(num_cols, data3.getNumColumns());
        assertEquals(num_cols, data4.getNumColumns());
        assertEquals(1, threads1);
        assertEquals(2, threads2);
        assertEquals(4, threads3);
        assertEquals(8, threads4);
        assertEquals(15, maxIterations1);
        assertEquals(15, maxIterations2);
        assertEquals(30, maxIterations3);
        assertEquals(35, maxIterations4);
        assertEquals(5, interleaving1);
        assertEquals(5, interleaving2);
        assertEquals(8, interleaving3);
        assertEquals(10, interleaving4);
    }

    /**
     * Testing that the csv read is correct
     * @result The resulting DataSet should be the one corresponding to the Resources.CANCER DataSet.
     */
    @Test
    public void readDataTest(){
        //Arrange
        int num_cols = 5;
        List<Node> columns = new ArrayList<>(Arrays.asList(Resources.XRAY, Resources.DYSPNOEA, Resources.CANCER, Resources.POLLUTION, Resources.SMOKER));
        //Act
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 1);
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
        new PGESv2(path, 1);
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
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 1);

        List<Edge> expected = Arrays.asList(Edges.directedEdge(Resources.XRAY, Resources.DYSPNOEA), Edges.directedEdge(Resources.XRAY, Resources.CANCER), Edges.directedEdge(Resources.XRAY, Resources.POLLUTION), Edges.directedEdge(Resources.XRAY, Resources.SMOKER),
                Edges.directedEdge(Resources.DYSPNOEA, Resources.CANCER), Edges.directedEdge(Resources.DYSPNOEA, Resources.POLLUTION), Edges.directedEdge(Resources.DYSPNOEA, Resources.SMOKER),
                Edges.directedEdge(Resources.CANCER, Resources.POLLUTION), Edges.directedEdge(Resources.CANCER, Resources.SMOKER),
                Edges.directedEdge(Resources.POLLUTION, Resources.SMOKER),
                Edges.directedEdge(Resources.DYSPNOEA, Resources.XRAY), Edges.directedEdge(Resources.CANCER, Resources.XRAY), Edges.directedEdge(Resources.POLLUTION, Resources.XRAY), Edges.directedEdge(Resources.SMOKER, Resources.XRAY),
                Edges.directedEdge(Resources.CANCER, Resources.DYSPNOEA), Edges.directedEdge(Resources.POLLUTION, Resources.DYSPNOEA), Edges.directedEdge(Resources.SMOKER, Resources.DYSPNOEA),
                Edges.directedEdge(Resources.POLLUTION, Resources.CANCER), Edges.directedEdge(Resources.SMOKER, Resources.CANCER),
                Edges.directedEdge(Resources.SMOKER, Resources.POLLUTION)
        );
        // Act
        pGESv2.calculateArcs();
        Set<Edge> result = pGESv2.getSetOfArcs();

        // Assert
        // Asserting size
        assertEquals(expected.size(), result.size());
        for (Edge edge1 : expected) {
            boolean isEqual = false;
            for (Edge edge2 : result) {
                if (edge1.equals(edge2)) {
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
    public void splitArcsTest() {
        // Arrange
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 2);

        // Act
        pGESv2.calculateArcs();
        pGESv2.splitArcs();
        Set<Edge> arcs = pGESv2.getSetOfArcs();
        List<Set<Edge>> subsets = pGESv2.getSubSets();

        // Assert
        // Checking that each arc is in fact in a subset, and that it is only once in it.
        for (Edge edge : arcs) {
            int counter = 0;
            for (Set<Edge> subset : subsets) {
                counter += Collections.frequency(subset, edge);
            }
            // Double pairs
            assertEquals(1, counter);
        }
    }


    /*
    @Test
    public void fesStageTest() throws InterruptedException {
        // Arrange
        initializeSubsets();
        // Expectation
        List<Edge> expected1 = new ArrayList<>();
        expected1.add(new Edge(Resources.CANCER, Resources.DYSPNOEA, Endpoint.TAIL, Endpoint.ARROW));
        expected1.add(new Edge(Resources.CANCER, Resources.XRAY, Endpoint.TAIL, Endpoint.ARROW));
        expected1.add(new Edge(Resources.POLLUTION, Resources.CANCER, Endpoint.TAIL, Endpoint.ARROW));
        List<Edge> expected2 = new ArrayList<>();
        expected2.add(new Edge(Resources.SMOKER, Resources.CANCER, Endpoint.TAIL, Endpoint.ARROW));

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
*/
    /**
     * Testing the setter and getter of MaxIterations
     * @result MaxIterations is changed to 21 and obtained as such.
     */
    @Test
    public void setgetMaxIterations(){
        // Arrange
        int expected = 21;
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 1);
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
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 1);
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
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 2);
        Utils.setSeed(42);
        List<Node> nodes = new ArrayList<>();
        nodes.add(Resources.CANCER);
        nodes.add(Resources.DYSPNOEA);
        nodes.add(Resources.XRAY);
        nodes.add(Resources.POLLUTION);
        nodes.add(Resources.SMOKER);
        Dag expected = new Dag(nodes);
        expected.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        expected.addDirectedEdge(Resources.CANCER, Resources.XRAY);
        expected.addDirectedEdge(Resources.CANCER, Resources.POLLUTION);
        expected.addDirectedEdge(Resources.SMOKER, Resources.CANCER);

        // Act
        pGESv2.calculateArcs();
        pGESv2.splitArcs();
        pGESv2.fesStage();
        Dag result = pGESv2.fusionFES();
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
        Set<Edge> resultingEdges = result.getEdges();
        for(Edge resEdge : resultingEdges){
            /*Node node1 = resEdge.getNode1();
            Node node2 = resEdge.getNode2();

            // System.out.println("Node1: " + node1.getName());
            // System.out.println("Node2: " + node2.getName());

            if(node1.getName().equals("Cancer")){
                String node2Name = node2.getName();
                assertTrue(((node2Name.equals("Dyspnoea")) || (node2Name.equals("Xray")) || (node2Name.equals("Pollution")) ));
                continue;
            }
            if(node1.getName().equals("Smoker")){
                assertEquals("Cancer", node2.getName());
                continue;
            }
            // If node1 i not any of these nodes, then assert error
            fail("Node1 is not in the expected range.");
            */
            assertTrue(expected.containsEdge(resEdge));
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
        expected.add(new Edge(Resources.CANCER, Resources.DYSPNOEA, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(Resources.CANCER, Resources.XRAY, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(Resources.CANCER, Resources.POLLUTION, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(Resources.SMOKER, Resources.CANCER, Endpoint.TAIL, Endpoint.ARROW));

        // Creating main
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 2);
        // Initial Configuration: Cases
        //GESThread.setProblem(pGESv2.getData());

        pGESv2.calculateArcs();
        pGESv2.splitArcs();
        pGESv2.fesStage();
        pGESv2.fusionFES();

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
        PGESv2 pGESv2 = new PGESv2(Resources.CANCER_BBDD_PATH, 2);

        //Expectation
        List<Node> nodes = Arrays.asList(Resources.CANCER, Resources.DYSPNOEA, Resources.POLLUTION, Resources.XRAY, Resources.SMOKER);
        EdgeListGraph expectation = new EdgeListGraph(nodes);
        expectation.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        expectation.addDirectedEdge(Resources.CANCER, Resources.XRAY);
        expectation.addDirectedEdge(Resources.POLLUTION, Resources.CANCER);
        expectation.addDirectedEdge(Resources.SMOKER, Resources.CANCER);

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
