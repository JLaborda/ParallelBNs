package org.albacete.simd.threads;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.TupleNode;
import org.albacete.simd.utils.Utils;

import org.junit.Test;
import static org.junit.Assert.*;


import java.util.ArrayList;
import java.util.List;

public class ForwardHillClimbingThreadTest {

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

    private Problem problem;


    /**
     * Constructor of the test. It initializes the subsets.
     */
    public ForwardHillClimbingThreadTest(){
        problem = new Problem(dataset);
        initializeSubsets();
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

    @Test
    public void constructorTest() throws InterruptedException{
        // Arrange
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread (problem, subset1, 15);
        thread1.run();
        Graph graph = thread1.getCurrentGraph();
        // Act
        ForwardHillClimbingThread thread2 = new ForwardHillClimbingThread(problem, graph, subset1, 15);
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
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread(problem, subset1, 15);
        ForwardHillClimbingThread thread2 = new ForwardHillClimbingThread(problem, subset2, 15);

        // Expectation
        List<Edge> expected1 = new ArrayList<>();
        expected1.add(new Edge(xray, cancer, Endpoint.TAIL, Endpoint.ARROW));

        List<Edge> expected2 = new ArrayList<>();
        expected2.add(new Edge(cancer, smoker, Endpoint.TAIL, Endpoint.ARROW));


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
     * Checking that FHC stops when there are no more edges to be added.
     * @result The number of iterations is less than the maximum iterations set
     */
    @Test
    public void noMoreEdgesToAddInFESTest(){

        // ThFES objects
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread(problem, subset1, 1000);

        //Act
        thread1.run();

        //Assert
        assertNotEquals(thread1.getIterations(), 1000);
    }


    /**
     * Testing that fhc stops when the maximum number of edges is reached.
     * @result The resulting graph has the same number of edges as the set maximum number of edges.
     * @throws InterruptedException Caused by an external interruption.
     */
    @Test
    public void maximumNumberOfEdgesReachedTest() throws InterruptedException {
        // ThFES objects
        FESThread thread1 = new FESThread(problem, subset1, 1000);
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

        Problem pAlarm = new Problem(alarmDataset);
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread(pAlarm, subset1, 100);
        ForwardHillClimbingThread thread2 = new ForwardHillClimbingThread(pAlarm, subset2, 100);


        //Act
        thread1.run();
        thread2.run();
        Graph result1 = thread1.getCurrentGraph();
        Graph result2 = thread2.getCurrentGraph();
        //Assert
        assertNotNull(result1);
        assertNotNull(result2);

    }



}
