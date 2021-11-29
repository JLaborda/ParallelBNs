package org.albacete.simd.algorithms;

import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.bnbuilders.HillClimbingSearch;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.Resources;
import org.junit.Test;

import static org.junit.Assert.*;

public class HillClimbingSearchTest {

    String path = Resources.ALARM_BBDD_PATH;
    @Test
    public void constructorTest(){
        Problem problem = new Problem(path);
        HillClimbingSearch hc1 = new HillClimbingSearch(path,15,5);
        HillClimbingSearch hc2 = new HillClimbingSearch(problem.getData(), 15, 5);

        assertNotNull(hc1);
        assertNotNull(hc2);
        assertEquals(0, hc1.getnThreads());
        assertEquals(0, hc2.getnThreads());
        assertEquals(15, hc1.getMaxIterations());
        assertEquals(15, hc2.getMaxIterations());
        assertEquals(5, hc1.getItInterleaving());
        assertEquals(5, hc2.getItInterleaving());

    }

    @Test
    public void searchTest(){
        HillClimbingSearch hc1 = new HillClimbingSearch(path,15,5);

        hc1.search();

        assertNotNull(hc1.getCurrentGraph());
        assertTrue(hc1.getCurrentGraph() instanceof Dag);
    }

    @Test
    public void convergenceTest(){
        HillClimbingSearch hc1 = new HillClimbingSearch(path,1,5);

        hc1.search();

        assertNotNull(hc1.getCurrentGraph());
        assertTrue(hc1.getCurrentGraph() instanceof Dag);
    }
}
