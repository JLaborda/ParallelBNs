package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.Resources;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.RandomClustering;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.albacete.simd.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class PGESwithStagesTest {

    String path = Resources.CANCER_BBDD_PATH;
    DataSet dataSet = Utils.readData(path);
    Clustering clustering = new RandomClustering();

    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void testConstructor(){
        PGESwithStages alg1 = new PGESwithStages(dataSet, clustering, 2, 100, 5);
        PGESwithStages alg2 = new PGESwithStages(path,clustering, 2, 100, 5);

        List<Node> nodes = Arrays.asList(Resources.CANCER, Resources.DYSPNOEA, Resources.POLLUTION, Resources.XRAY, Resources.SMOKER);
        Dag initialGraph = new Dag(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);

        PGESwithStages alg3 = new PGESwithStages(initialGraph, Resources.CANCER_BBDD_PATH, clustering, 2, 100, 5);
        PGESwithStages alg4 = new PGESwithStages(initialGraph, Resources.CANCER_DATASET, clustering, 2, 100, 5);


        assertNotNull(alg1);
        assertNotNull(alg2);
        assertNotNull(alg3);
        assertNotNull(alg4);

        assertEquals(2, alg1.getnThreads());
        assertEquals(2, alg2.getnThreads());
        assertEquals(2, alg3.getnThreads());
        assertEquals(2, alg4.getnThreads());
        assertEquals(100, alg1.getMaxIterations());
        assertEquals(100, alg2.getMaxIterations());
        assertEquals(100, alg3.getMaxIterations());
        assertEquals(100, alg4.getMaxIterations());
        assertEquals(5, alg1.getItInterleaving());
        assertEquals(5, alg2.getItInterleaving());
        assertEquals(5, alg3.getItInterleaving());
        assertEquals(5, alg4.getItInterleaving());
        assertNull(alg1.getCurrentGraph());
        assertNull(alg2.getCurrentGraph());
        assertNotNull(alg3.getCurrentGraph());
        assertNotNull(alg4.getCurrentGraph());

    }

    @Test
    public void searchTest(){
        System.out.println("PGESwithStagesTest: searchTest");
        PGESwithStages alg1 = new PGESwithStages(Resources.CANCER_DATASET, clustering, 2, 100, 5);
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

        alg1.search();

        System.out.println((alg1.getCurrentGraph()));

        assertNotNull(alg1.getCurrentGraph());
        assertTrue(alg1.getCurrentGraph() instanceof Dag);

        Set<Edge> resultingEdges = alg1.getCurrentGraph().getEdges();
        // Asserting
        assertTrue(Resources.equalsEdges(expected.getEdges(), resultingEdges));
    }


    @Test
    public void convergenceTest(){
        PGESwithStages alg = new PGESwithStages(Utils.readData(Resources.CANCER_BBDD_PATH),
                clustering,
                2,
                1,
                5
        );
        alg.search();

        assertNotNull(alg.getCurrentGraph());
        assertTrue(alg.getCurrentGraph() instanceof Dag);
        assertEquals(1,alg.getIterations());

    }

    @Test
    public void testSearchWithInitialGraph() {
        List<Node> nodes = Arrays.asList(Resources.XRAY, Resources.DYSPNOEA, Resources.CANCER, Resources.POLLUTION, Resources.SMOKER);
        Dag initialGraph = new Dag(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);

        Clustering clustering = new RandomClustering(42);
        PGESwithStages alg = new PGESwithStages(initialGraph, Resources.CANCER_BBDD_PATH, clustering, 2, 100, 5);

        Dag result = alg.getCurrentDag();
        // Equals is never gonna work. Because tetrad doesn't have a proper equals
        assertEquals(initialGraph.getNodes(), result.getNodes());
        assertEquals(initialGraph.getEdges(), result.getEdges());
        //&assertEquals(initialGraph, result);
        alg.search();
        assertNotNull(alg.getCurrentGraph());
        assertNotNull(alg.getCurrentDag());
        assertNotEquals(initialGraph, alg.getCurrentDag());

    }


}
