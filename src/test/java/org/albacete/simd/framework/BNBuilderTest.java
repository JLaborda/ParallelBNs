package org.albacete.simd.framework;

import edu.cmu.tetrad.graph.Edge;
import org.albacete.simd.algorithms.HillClimbingSearch;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BNBuilderTest {

    @Test
    public void settersAndGettersTest(){
        String path = "src/test/resources/alarm.xbif_.csv";
        BNBuilder algorithm = new HillClimbingSearch(path, 15, 5);
        Problem problem = new Problem(path);
        List<Edge> arcs = Utils.calculateArcs(problem.getData());

        algorithm.setSeed(30);
        algorithm.setMaxIterations(30);
        algorithm.setnItInterleaving(20);


        assertEquals(30, algorithm.getSeed());
        assertEquals(arcs, algorithm.getListOfArcs());
        assertTrue(algorithm.getSubSets().isEmpty());
        assertEquals(problem.getData(), algorithm.getData());
        assertEquals(30, algorithm.getMaxIterations());
        assertNull(algorithm.getGraphs());
        assertEquals(20,algorithm.getItInterleaving());
        assertNull(algorithm.getCurrentGraph());
        assertEquals(1, algorithm.getIterations());
        assertEquals(problem, algorithm.getProblem());
        assertEquals(1, algorithm.getnThreads());


    }
}
