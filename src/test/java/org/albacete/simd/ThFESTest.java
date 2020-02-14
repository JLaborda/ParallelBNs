package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ThFESTest {

    //Nodes of Cancer Network
    final String path = "src/test/resources/cancer.xbif_.csv";
    final DataSet dataset = Utils.readData(path);
    final Node xray = dataset.getVariable("Xray");
    final Node dyspnoea = dataset.getVariable("Dyspnoea");
    final Node cancer = dataset.getVariable("Cancer");
    final Node pollution = dataset.getVariable("Pollution");
    final Node smoker = dataset.getVariable("Smoker");

    final ArrayList<TupleNode> subset1 = new ArrayList<>();
    final ArrayList<TupleNode> subset2 = new ArrayList<>();


    public ThFESTest(){
        initializeSubsets();
    }

    private Graph removeInconsistencies(Graph graph){
        // Transforming the current graph into a DAG
        SearchGraphUtils.pdagToDag(graph);

        Node nodeT, nodeH;
        for (Edge e : graph.getEdges()){
            if(!e.isDirected()) continue;
            Endpoint endpoint1 = e.getEndpoint1();
            if (endpoint1.equals(Endpoint.ARROW)){
                nodeT = e.getNode1();
                nodeH = e.getNode2();
            }else{
                nodeT = e.getNode2();
                nodeH = e.getNode1();
            }
            if(graph.existsDirectedPathFromTo(nodeT, nodeH))
                graph.removeEdge(e);
        }
        return graph;
    }

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
     * Checks that both constructors work perfectly
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void constructorTest() throws InterruptedException{
        // Arrange
        ThFES thread1 = new ThFES(dataset, subset1, 15);
        thread1.run();
        Graph graph = thread1.getCurrentGraph();
        // Act
        ThFES thread2 = new ThFES(dataset, graph, subset1, 15);
        // Arrange
        assertNotNull(thread1);
        assertNotNull(thread2);

    }

    /**
     * Checks the first iteration of the Cancer problem for the FES stage
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void searchTwoThreadsTest() throws InterruptedException {

        // ThFES objects
        ThFES thread1 = new ThFES(dataset, subset1, 15);
        ThFES thread2 = new ThFES(dataset, subset2, 15);

        // Expectation
        List<Edge> expected1 = new ArrayList<>();
        expected1.add(new Edge(cancer, dyspnoea, Endpoint.TAIL, Endpoint.ARROW));
        expected1.add(new Edge(cancer, xray, Endpoint.TAIL, Endpoint.ARROW));
        expected1.add(new Edge(pollution, cancer, Endpoint.TAIL, Endpoint.ARROW));

        List<Edge> expected2 = new ArrayList<>();
        expected2.add(new Edge(smoker, cancer, Endpoint.TAIL, Endpoint.ARROW));

        //Act
        thread1.run();
        thread2.run();
        Graph g1 = thread1.getCurrentGraph();
        Graph g2 = thread2.getCurrentGraph();

        // Getting dags
        Dag gdag1 = new Dag(removeInconsistencies(g1));
        Dag gdag2 = new Dag(removeInconsistencies(g2));


        for(Edge edge : expected1){
            assertTrue(gdag1.getEdges().contains(edge));
        }

        for(Edge edge : expected2){
            assertTrue(gdag2.getEdges().contains(edge));
        }

    }

    /**
     * Checking that getter works correctly for AggressivelyPreventCycles variable
     */
    @Test
    public void isAggressivelyPreventCyclesTest(){
        ThFES thfes = new ThFES(dataset, subset1, 15);
        assertFalse(thfes.isAggressivelyPreventCycles());
    }
    /**
     * Checking that setter works correctly for AggressivelyPreventCycles variable
     */
    @Test
    public void setAggresivelyPreventCyclesTest(){
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        thfes.setAggressivelyPreventCycles(true);
        // Assert
        assertTrue(thfes.isAggressivelyPreventCycles());
    }

    /**
     * Checking that getter works correctly for currentGraph variable
     */
    @Test
    public void getCurrentGraphTest() throws InterruptedException {
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        thfes.run();
        Graph result = thfes.getCurrentGraph();
        // Assert
        assertNotNull(result);
    }
    /**
     * Checking that getter works correctly for flag variable
     */
    @Test
    public void getFlagTest() throws InterruptedException {
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        thfes.run();
        boolean result = thfes.getFlag();
        // Assert
        assertTrue(result);
    }
    /**
     * Checking that resetting the flag works correctly
     */
    @Test
    public void resetFlagTest() throws InterruptedException {
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        thfes.run();
        thfes.resetFlag();
        boolean result = thfes.getFlag();
        // Assert
        assertFalse(result);
    }

    /**
     * Checking that the bdeu score works correctly
     */
    @Test
    public void getBDeuScoreTest(){
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        double expected = -10591.313506863182;
        // Act
        thfes.run();
        thfes.resetFlag();
        double result = thfes.getScoreBDeu();
        // Assert
        assertEquals(expected, result, 0);
    }

    /**
     * Checking that setting the initial graph works correctly
     */
    @Test
    public void setterAndGetterOfInitialGraphTest() throws InterruptedException {
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        thfes.run();
        Graph expected = thfes.getCurrentGraph();
        thfes.setInitialGraph(expected);
        Graph result = thfes.getInitialGraph();
        // Assert
        assertEquals(expected, result);
    }

    /**
     * Checking structurePrior getter and setter
     */
    @Test
    public void setterAndGetterOfStructurePriorTest(){
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        double expected = 2.3;
        thfes.setStructurePrior(expected);
        double actual = thfes.getStructurePrior();
        // Assert
        assertEquals(expected, actual, 0);
    }

    /**
     * Checking samplePrior getter and setter
     */
    @Test
    public void setterAndGetterOfSamplePriorTest(){
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        double expected = 2.3;
        thfes.setSamplePrior(expected);
        double actual = thfes.getSamplePrior();
        // Assert
        assertEquals(expected, actual, 0);
    }


    /**
     * Checking elapsed time getter and setter
     */
    @Test
    public void setterAndGetterOfElapsedTimeTest(){
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        long expected = 23;
        thfes.setElapsedTime(expected);
        long actual = thfes.getElapsedTime();
        // Assert
        assertEquals(expected, actual, 0);
    }

    /**
     * Checking elapsed time getter and setter
     */
    @Test
    public void setterAndGetterOfMaxNumEdges(){
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        int expected = 23;
        thfes.setMaxNumEdges(expected);
        int actual = thfes.getMaxNumEdges();
        // Assert
        assertEquals(expected, actual);
    }





}
