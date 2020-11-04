package org.albacete.simd.algorithms;

import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.utils.Problem;
import org.junit.Test;

import static org.junit.Assert.*;

public class HillClimbingSearchTest {

    @Test
    public void constructorTest(){
        String path = "src/test/resources/alarm.xbif_.csv";
        Problem problem = new Problem(path);
        HillClimbingSearch hc1 = new HillClimbingSearch(path,2,15,5);
        HillClimbingSearch hc2 = new HillClimbingSearch(problem.getData(), 2, 15, 5);

        assertNotNull(hc1);
        assertNotNull(hc2);
        assertEquals(2, hc1.getnThreads());
        assertEquals(2, hc2.getnThreads());
        assertEquals(15, hc1.getMaxIterations());
        assertEquals(15, hc2.getMaxIterations());
        assertEquals(5, hc1.getItInterleaving());
        assertEquals(5, hc2.getItInterleaving());

    }

    @Test
    public void searchTest(){
        String path = "src/test/resources/alarm.xbif_.csv";
        HillClimbingSearch hc1 = new HillClimbingSearch(path,2,15,5);

        hc1.search();

        assertNotNull(hc1.getCurrentGraph());
        assertTrue(hc1.getCurrentGraph() instanceof Dag);
    }

    @Test
    public void convergenceTest(){
        String path = "src/test/resources/alarm.xbif_.csv";
        HillClimbingSearch hc1 = new HillClimbingSearch(path,2,1,5);

        hc1.search();

        assertNotNull(hc1.getCurrentGraph());
        assertTrue(hc1.getCurrentGraph() instanceof Dag);
    }
}
