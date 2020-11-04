package org.albacete.simd.algorithms;

import org.albacete.simd.utils.Problem;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParallelFHCBESTest {

    @Test
    public void constructorTest(){
        String path = "src/test/resources/alarm.xbif_.csv";
        Problem problem = new Problem(path);

        ParallelFHCBES alg1 = new ParallelFHCBES(problem.getData(),
                2,
                15,
                5
        );
        ParallelFHCBES alg2 = new ParallelFHCBES(path,
                2,
                15,
                5
        );

        assertNotNull(alg1);
        assertNotNull(alg2);

        assertEquals(problem.getData(), alg1.getData());
        assertEquals(problem.getData(), alg2.getData());
        assertEquals(2,alg1.getnThreads());
        assertEquals(2,alg2.getnThreads());
        assertEquals(15, alg1.getMaxIterations());
        assertEquals(15, alg2.getMaxIterations());
        assertEquals(5, alg1.getItInterleaving());
        assertEquals(5, alg2.getItInterleaving());

    }

    @Test
    public void runTest(){
        String path = "src/test/resources/alarm.xbif_.csv";
        ParallelFHCBES alg = new ParallelFHCBES(path,
                2,
                15,
                5
        );
        alg.search();

        assertNotNull(alg.getCurrentGraph());

    }

    @Test
    public void convergenceTest(){
        String path = "src/test/resources/alarm.xbif_.csv";
        ParallelFHCBES alg = new ParallelFHCBES(path,
                2,
                1,
                5
        );
        alg.search();

        assertNotNull(alg.getCurrentGraph());

    }


}
