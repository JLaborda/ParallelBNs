package org.albacete.simd.algorithms.pGESv2;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;

import org.albacete.simd.utils.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the ThBES class works as expected.
 */
public class ThBESTest {
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
    public ThBESTest(){
        GESThread.setProblem(dataset);
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
     * Checks that the constructor works perfectly
     * @result  Both constructors create a ThBES object.
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void constructorTest() throws InterruptedException{
        // Arrange
        ThFES thread1 = new ThFES(dataset, subset1, 15);
        thread1.run();
        Graph graph = thread1.getCurrentGraph();
        // Act
        ThBES thread2 = new ThBES(dataset, graph, subset1);
        // Arrange
        assertNotNull(thread2);
    }

    /**
     * Checks that the BES stage works as expected
     * @result All edges are in the expected result.
     * @throws InterruptedException Interruption caused by external forces.
     */
    @Test
    public void searchTwoThreadsTest() throws InterruptedException {
        //Arrange
        /*
        Dag [] fesGraphs = fesStageExperiment();
        ArrayList<Dag> graphs = new ArrayList<>();
        Collections.addAll(graphs,fesGraphs);
        Dag fusionGraph = (new ConsensusUnion(graphs)).union();
        */
        List<Node> nodes = Arrays.asList(cancer, xray, dyspnoea, pollution, smoker);
        Graph fusionGraph = new EdgeListGraph(nodes);
        fusionGraph.addDirectedEdge(cancer, dyspnoea);
        fusionGraph.addDirectedEdge(cancer, xray);
        fusionGraph.addDirectedEdge(pollution, cancer);
        fusionGraph.addDirectedEdge(smoker, cancer);
        fusionGraph.addDirectedEdge(xray, dyspnoea);
        fusionGraph.addDirectedEdge(pollution, smoker);


        System.out.println("Initial Graph");
        System.out.println(fusionGraph);


        ThBES thread1 = new ThBES(dataset, fusionGraph, subset1);

        List<Edge> expected = new ArrayList<>();
        expected.add(new Edge(cancer,xray,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(pollution,cancer,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(smoker,cancer,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(smoker,pollution,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(xray,dyspnoea,Endpoint.TAIL, Endpoint.ARROW));

        // Act
        thread1.run();
        Graph g1 = thread1.getCurrentGraph();

        // Getting dag
        Dag gdag1 = new Dag(removeInconsistencies(g1));

        System.out.println("ThBES");
        System.out.println(gdag1);

        // Asserting
        for(Edge edge : expected){
            assertTrue(gdag1.getEdges().contains(edge));
        }

    }








}
