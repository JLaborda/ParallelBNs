package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;
import consensusBN.ConsensusUnion;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ThBESTest {
    //Nodes of Cancer Network
    final String path = "src/test/resources/cancer.xbif_.csv";
    final DataSet dataset = Main.readData(path);
    final Node xray = dataset.getVariable("Xray");
    final Node dyspnoea = dataset.getVariable("Dyspnoea");
    final Node cancer = dataset.getVariable("Cancer");
    final Node pollution = dataset.getVariable("Pollution");
    final Node smoker = dataset.getVariable("Smoker");

    final ArrayList<TupleNode> subset1 = new ArrayList<>();
    final ArrayList<TupleNode> subset2 = new ArrayList<>();


    public ThBESTest(){
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
     * Checks that the constructor works perfectly
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void constructorTest() throws InterruptedException{
        // Arrange
        ThFES thread1 = new ThFES(dataset, subset1, 15);
        thread1.run();
        Graph graph = thread1.getCurrentGraph();
        // Act
        ThBES thread2 = new ThBES(dataset, graph, subset1, 15);
        // Arrange
        assertNotNull(thread2);
    }


    private Dag[] fesStageExperiment() throws InterruptedException {
        // ThFES objects
        ThFES thread1 = new ThFES(dataset, subset1, 15);
        ThFES thread2 = new ThFES(dataset, subset2, 15);

        List<Edge> edges1 = new ArrayList<>();
        edges1.add(new Edge(cancer, dyspnoea, Endpoint.TAIL, Endpoint.ARROW));
        edges1.add(new Edge(cancer, xray, Endpoint.TAIL, Endpoint.ARROW));
        edges1.add(new Edge(pollution, cancer, Endpoint.TAIL, Endpoint.ARROW));

        List<Edge> edges2 = new ArrayList<>();
        edges2.add(new Edge(smoker, cancer, Endpoint.TAIL, Endpoint.ARROW));

        // Running fesStage
        thread1.run();
        thread2.run();
        Graph g1 = thread1.getCurrentGraph();
        Graph g2 = thread2.getCurrentGraph();

        // Getting dags
        Dag gdag1 = new Dag(removeInconsistencies(g1));
        Dag gdag2 = new Dag(removeInconsistencies(g2));

        return new Dag[]{gdag1, gdag2};
    }

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


        ThBES thread1 = new ThBES(dataset, fusionGraph, subset1, 15);
        //ThBES thread2 = new ThBES(dataset, fusionGraph, subset2, 15);

        List<Edge> expected = new ArrayList<>();
        expected.add(new Edge(cancer,xray,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(pollution,cancer,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(smoker,cancer,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(smoker,pollution,Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(xray,dyspnoea,Endpoint.TAIL, Endpoint.ARROW));
        // Act
        thread1.run();
        //thread2.run();

        Graph g1 = thread1.getCurrentGraph();
        //Graph g2 = thread2.getCurrentGraph();

        // Getting dags
        Dag gdag1 = new Dag(removeInconsistencies(g1));
        //Dag gdag2 = new Dag(removeInconsistencies(g2));

        System.out.println("ThBES");
        System.out.println(gdag1);
        //System.out.println("ThBES2");
        //System.out.println(gdag2);

        // Asserting
        for(Edge edge : expected){
            assertTrue(gdag1.getEdges().contains(edge));
            //assertTrue(gdag2.getEdges().contains(edge));
        }

    }








}
