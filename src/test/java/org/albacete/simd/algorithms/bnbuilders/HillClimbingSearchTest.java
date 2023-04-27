package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.Resources;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.albacete.simd.utils.Problem;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class HillClimbingSearchTest {

    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void constructorTest(){
        Problem problem = new Problem(Resources.CANCER_BBDD_PATH);
        HillClimbingSearch hc1 = new HillClimbingSearch(Resources.CANCER_BBDD_PATH, 15, 5);
        HillClimbingSearch hc2 = new HillClimbingSearch(problem.getData(), 15, 5);
        HillClimbingSearch hc3 = new HillClimbingSearch(Resources.CANCER_BBDD_PATH);
        HillClimbingSearch hc4 = new HillClimbingSearch(problem.getData());

        List<Node> nodes = Arrays.asList(Resources.CANCER, Resources.DYSPNOEA, Resources.POLLUTION, Resources.XRAY, Resources.SMOKER);
        Dag_n initialGraph = new Dag_n(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);

        HillClimbingSearch hc5 = new HillClimbingSearch(initialGraph, Resources.CANCER_BBDD_PATH);
        HillClimbingSearch hc6 = new HillClimbingSearch(initialGraph,Resources.CANCER_DATASET);



        assertNotNull(hc1);
        assertNotNull(hc2);
        assertNotNull(hc3);
        assertNotNull(hc4);
        assertNotNull(hc5);
        assertNotNull(hc6);
        assertNull(hc1.getCurrentGraph());
        assertNull(hc2.getCurrentGraph());
        assertNull(hc3.getCurrentGraph());
        assertNull(hc4.getCurrentGraph());
        assertNotNull(hc5.getCurrentGraph());
        assertNotNull(hc6.getCurrentGraph());

        assertEquals(0, hc1.getnThreads());
        assertEquals(0, hc2.getnThreads());
        assertEquals(0, hc3.getnThreads());
        assertEquals(0, hc4.getnThreads());
        assertEquals(0, hc5.getnThreads());
        assertEquals(0, hc6.getnThreads());
        assertEquals(15, hc1.getMaxIterations());
        assertEquals(15, hc2.getMaxIterations());
        assertEquals(Integer.MAX_VALUE, hc3.getMaxIterations());
        assertEquals(Integer.MAX_VALUE, hc4.getMaxIterations());
        assertEquals(Integer.MAX_VALUE, hc5.getMaxIterations());
        assertEquals(Integer.MAX_VALUE, hc6.getMaxIterations());
        assertEquals(5, hc1.getItInterleaving());
        assertEquals(5, hc2.getItInterleaving());
        assertEquals(Integer.MAX_VALUE, hc3.getItInterleaving());
        assertEquals(Integer.MAX_VALUE, hc4.getItInterleaving());
        assertEquals(Integer.MAX_VALUE, hc5.getItInterleaving());
        assertEquals(Integer.MAX_VALUE, hc6.getItInterleaving());


    }

    @Test
    public void searchTest(){
        HillClimbingSearch hc1 = new HillClimbingSearch(Resources.CANCER_BBDD_PATH, 15, 5);

        hc1.search();

        assertNotNull(hc1.getCurrentGraph());
        assertTrue(hc1.getCurrentGraph() instanceof Dag_n);
    }

    @Test
    public void searchTestWithInitialGraph() {
        List<Node> nodes = Arrays.asList(Resources.XRAY, Resources.DYSPNOEA, Resources.CANCER, Resources.POLLUTION, Resources.SMOKER);
        Dag_n initialGraph = new Dag_n(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);
        //initialGraph.addDirectedEdge(Resources.POLLUTION, Resources.CANCER);
        //initialGraph.addDirectedEdge(Resources.SMOKER, Resources.CANCER);


        HillClimbingSearch hc1 = new HillClimbingSearch(initialGraph, Resources.CANCER_BBDD_PATH);
        Dag_n result = hc1.getCurrentDag();
        // Equals is never gonna work. Because tetrad doesn't have a proper equals
        assertEquals(initialGraph.getNodes(), result.getNodes());

        //Asserting that the edges from the initial Graph inserted and the current graph are the same
        System.out.println("Initial Graph: " + initialGraph.getEdges());
        System.out.println("Current Graph: " + result.getEdges());

        for (Edge edgeInitial : initialGraph.getEdges()) {
            Node x = edgeInitial.getNode1();
            Node y = edgeInitial.getNode2();
            boolean found = false;
            for (Edge edgeCurrent : result.getEdges()) {
                if (edgeCurrent.getNode1().equals(x) && edgeCurrent.getNode2().equals(y)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
        //assertEquals(initialGraph.getEdges(), result.getEdges());
        hc1.search();

        //Asserting that there is a resulting graph from hc1.
        assertNotNull(hc1.getCurrentGraph());
        assertNotNull(hc1.getCurrentDag());

        //Making sure that the graphs are different, because the search should have changed the graph.
        assertNotEquals(initialGraph, hc1.getCurrentDag());

    }

    @Test
    public void testPreStagesMethods() throws InterruptedException {
        // PreStages methods are convergence(), initialConfig(),repartition() and fusion methods. In a Sequential algorithm they must
        // not do anything
        HillClimbingSearch hc1 = new HillClimbingSearch(Resources.CANCER_BBDD_PATH);
        Graph graph = hc1.getCurrentGraph();

        hc1.initialConfig();
        hc1.repartition();
        hc1.forwardFusion();
        hc1.backwardFusion();

        assertFalse(hc1.convergence());
        assertEquals(graph, hc1.getCurrentGraph());


    }



}
