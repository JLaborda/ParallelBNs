package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.Resources;
import org.albacete.simd.algorithms.ParallelFHCBES;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.albacete.simd.utils.Utils;
import org.junit.Before;
import org.junit.Test;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import static org.junit.Assert.*;

import java.util.*;

public class PGESwithStagesTest {

    String path = Resources.CANCER_BBDD_PATH;
    DataSet dataSet = Utils.readData(path);


    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void testConstructor(){

        PGESwithStages alg1 = new PGESwithStages(dataSet, 2, 100, 5);
        PGESwithStages alg2 = new PGESwithStages(path, 2, 100, 5);

        assertNotNull(alg1);
        assertNotNull(alg2);

        assertEquals(2, alg1.getnThreads());
        assertEquals(2, alg2.getnThreads());
        assertEquals(100, alg1.getMaxIterations());
        assertEquals(100, alg2.getMaxIterations());
        assertEquals(5, alg1.getItInterleaving());
        assertEquals(5, alg2.getItInterleaving());

    }

    @Test
    public void searchTest(){
        PGESwithStages alg1 = new PGESwithStages(dataSet, 2, 100, 5);
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

        List<Edge> resultingEdges = alg1.getCurrentGraph().getEdges();
        for(Edge edge: resultingEdges){
            assertTrue(expected.containsEdge(edge));
        }
    }


    @Test
    public void convergenceTest(){
        PGESwithStages alg = new PGESwithStages(Utils.readData(Resources.ALARM_BBDD_PATH),
                2,
                1,
                5
        );
        alg.search();

        assertNotNull(alg.getCurrentGraph());
        assertTrue(alg.getCurrentGraph() instanceof Dag);
        assertEquals(1,alg.getIterations());

    }




}
