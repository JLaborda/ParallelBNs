package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag_n;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PHCTest {

    String path = Resources.CANCER_BBDD_PATH;
    DataSet dataSet = Utils.readData(path);
    Clustering clustering = new RandomClustering();

    @Before
    public void restartMeans() {
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void testConstructor() {
        PGESwithStages alg1 = new PGESwithStages(dataSet, clustering, 2, 100, 5, false);
        PGESwithStages alg2 = new PGESwithStages(path, clustering, 2, 100, 5, false);

        List<Node> nodes = Arrays.asList(Resources.CANCER, Resources.DYSPNOEA, Resources.POLLUTION, Resources.XRAY, Resources.SMOKER);
        Dag_n initialGraph = new Dag_n(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);

        PGESwithStages alg3 = new PGESwithStages(initialGraph, Resources.CANCER_BBDD_PATH, clustering, 2, 100, 5, false);
        PGESwithStages alg4 = new PGESwithStages(initialGraph, Resources.CANCER_DATASET, clustering, 2, 100, 5, false);


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
    public void searchTest() {
        Utils.setSeed(42);
        PGESwithStages alg1 = new PGESwithStages(Resources.CANCER_BBDD_PATH, clustering, 2, 100, 5, false);

        alg1.search();

        //Asserting that there is a result
        assertNotNull(alg1.getCurrentGraph());
        assertTrue(alg1.getCurrentGraph() instanceof Dag_n);
        assertEquals(5, alg1.getCurrentGraph().getNodes().size());
    }


    @Test
    public void convergenceTest() {
        PGESwithStages alg = new PGESwithStages(Utils.readData(Resources.CANCER_BBDD_PATH),
                clustering,
                2,
                1,
                5,
                false
        );
        alg.search();

        assertNotNull(alg.getCurrentGraph());
        assertTrue(alg.getCurrentGraph() instanceof Dag_n);
        assertEquals(1, alg.getIterations());

    }

    @Test
    public void testSearchWithInitialGraph() {
        List<Node> nodes = Arrays.asList(Resources.XRAY, Resources.DYSPNOEA, Resources.CANCER, Resources.POLLUTION, Resources.SMOKER);
        Dag_n initialGraph = new Dag_n(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);

        Clustering clustering = new RandomClustering(42);
        PGESwithStages alg = new PGESwithStages(initialGraph, Resources.CANCER_BBDD_PATH, clustering, 2, 100, 5, false);

        Dag_n result = alg.getCurrentDag();
        // Equals is never gonna work. Because tetrad doesn't have a proper equals
        assertEquals(initialGraph.getNodes(), result.getNodes());
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
        //&assertEquals(initialGraph, result);
        alg.search();
        assertNotNull(alg.getCurrentGraph());
        assertNotNull(alg.getCurrentDag());
        assertNotEquals(initialGraph, alg.getCurrentDag());

    }


}
