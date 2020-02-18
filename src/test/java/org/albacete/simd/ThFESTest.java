package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ThFESTest {

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
     * Constructor of the test. It initializes the subsets.
     */
    public ThFESTest(){
        initializeSubsets();
    }

    /**
     * Method used to remove inconsistencies in the graph passed as a parameter.
     * @param graph Graph that will have its inconsistencies removed
     * @return The modified graph
     */
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
     * Checks that both constructors work perfectly.
     * @result  Both constructors create a ThFES object.
     * @throws InterruptedException Exception caused by thread interruption.
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
     * @result Each expected node is in the resulting graph after executing the first iteration of FES stage
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
     * Checking that fes stops when there are no more edges to be added.
     * @result The number of iterations is less than the maximum iterations set
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void noMoreEdgesToAddInFESTest(){

        // ThFES objects
        ThFES thread1 = new ThFES(dataset, subset1, 1000);

        //Act
        thread1.run();

        //Assert
        assertNotEquals(thread1.getIterations(), 1000);
    }

    /**
     * Testing that fes stops when the maximum number of edges is reached.
     * @result The resulting graph has the same number of edges as the set maximum number of edges.
     * @throws InterruptedException Caused by an external interruption.
     */
    @Test
    public void maximumNumberOfEdgesReachedTest() throws InterruptedException {
        // ThFES objects
        ThFES thread1 = new ThFES(dataset, subset1, 1000);
        thread1.setMaxNumEdges(2);

        //Act
        thread1.run();
        Graph result = thread1.getCurrentGraph();
        //Assert
        assertEquals(2, result.getEdges().size());

    }

    /**
     * Tests that the algorithm works correct with the Alarm network.
     * @result The resulting graph has the same number of edges as the set maximum number of edges.
     * @throws InterruptedException Caused by an external interruption.
     */
    @Test
    public void alarmExecutionTest() throws InterruptedException {
        // ThFES objects
        String alarmPath = "src/test/resources/alarm.xbif_.csv";
        DataSet alarmDataset = Utils.readData(alarmPath);
        TupleNode[] listOfArcs = Utils.calculateArcs(alarmDataset);
        ArrayList<TupleNode>[] subsets = Utils.split(listOfArcs,2,42);
        ArrayList<TupleNode> subset1 = subsets[0];
        ArrayList<TupleNode> subset2 = subsets[1];


        ThFES thread1 = new ThFES(alarmDataset, subset1, 100);
        ThFES thread2 = new ThFES(alarmDataset, subset2, 100);


        //Act
        thread1.run();
        thread2.run();
        Graph result1 = thread1.getCurrentGraph();
        Graph result2 = thread2.getCurrentGraph();
        //Assert
        assertNotNull(result1);
        assertNotNull(result2);

    }

    /**
     * Tests that if two nodes X and Y are equal in the subset S, then it should not be considered in the fes stage.
     * @result The resulting graph must not contain an edge formed by the same Node.
     */
    @Test
    public void xAndYAreEqualShouldContinueTest() throws InterruptedException {
        // Arrange
        TupleNode tuple1 = new TupleNode(this.cancer,this.cancer);
        TupleNode tuple2 = new TupleNode(this.cancer, this.smoker);

        ArrayList<TupleNode> S = new ArrayList<>();
        S.add(tuple1);
        S.add(tuple2);

        ThFES fes = new ThFES(dataset, S, 100);
        Thread tFES = new Thread(fes);

        // Act
        tFES.start();
        tFES.join();
        Graph result = fes.getCurrentGraph();

        // Assert
        Edge badEdge1 = Edges.undirectedEdge(this.cancer, this.cancer);
        Edge badEdge2 = Edges.directedEdge(this.cancer, this.cancer);
        Edge goodEdge1 = Edges.undirectedEdge(this.smoker, this.cancer);

        assertFalse(result.getEdges().contains(badEdge1));
        assertFalse(result.getEdges().contains(badEdge2));
        assertTrue(result.getEdges().contains(goodEdge1));
    }



    /**
     * Checking that getter works correctly for AggressivelyPreventCycles variable
     * @result Checks that the boolean value is false.
     */
    @Test
    public void isAggressivelyPreventCyclesTest(){
        ThFES thfes = new ThFES(dataset, subset1, 15);
        assertFalse(thfes.isAggressivelyPreventCycles());
    }
    /**
     * Checking that setter works correctly for AggressivelyPreventCycles variable
     * @result Checking that the value has changed to true
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
     * @result CurrentGraph is not null
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
     * @result flag is true when we create a new ThFES
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
     * @result flag should be false since we have done a reset.
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
     * @result The value of the score is the same for the same problem and subset no matter how many times we run it.
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
     * @result Changing the graph should give us a new graph not equal to the previous currentGraph.
     */
    @Test
    public void setterAndGetterOfInitialGraphTest() throws InterruptedException {
        // Arrange
        ThFES thfes = new ThFES(dataset, subset1, 15);
        // Act
        thfes.run();
        Graph expected = thfes.getCurrentGraph();

        thfes.setInitialGraph(null);
        Graph result = thfes.getInitialGraph();
        // Assert
        assertNotEquals(expected, result);
        assertNull(result);
    }

    /**
     * Checking structurePrior getter and setter
     * @result the value is changed
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
     * @result  the samplePrior is changed
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
     * @result the ElapsedTime has changed
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
     * Checking maxNumEdges getter and setter
     * @result the maxNumEdges has changed its value.
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
