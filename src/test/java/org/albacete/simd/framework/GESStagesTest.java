package org.albacete.simd.framework;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.Resources;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class GESStagesTest {

    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }

    @Test
    public void runTest() throws InterruptedException{
        //Arrange
        String path = Resources.ALARM_BBDD_PATH;
        Problem problem = new Problem(path);
        int nThreads = 2;
        int itInterleaving = 5;
        List<Set<Edge>> subsets = Utils.split(Utils.calculateArcs(problem.getData()), nThreads);
        Stage fesStage = new FESStage(problem, nThreads, itInterleaving, subsets);

        // TESTING FESStage
        // Act
        boolean flag;
        flag = fesStage.run();
        double fesStageScore = (GESThread.scoreGraph(fesStage.getGraphs().get(0), problem) + GESThread.scoreGraph(fesStage.getGraphs().get(1), problem)) / 2;
        //Assert
        assertTrue(flag);
        assertEquals(nThreads, fesStage.getGraphs().size());
        assertNotNull(fesStage.getGraphs().get(0));
        assertNotNull(fesStage.getGraphs().get(1));

        //TESTING FESFusion
        Stage fesFusion = new FESFusion(problem, fesStage.getCurrentGraph(), fesStage.getGraphs());
        flag = fesFusion.run();
        Graph g = fesFusion.getCurrentGraph();
        double fesFusionScore = GESThread.scoreGraph(g, problem);

        assertTrue(flag);
        assertNotNull(g);
        assertTrue(g instanceof Dag);
        assertEquals(g.getNumNodes(), problem.getVariables().size());
        assertTrue(fesFusionScore >= fesStageScore);

        //TESTING BESStage
        Stage besStage = new BESStage(problem,
                g,
                nThreads,
                itInterleaving,
                subsets
        );
        // No edge is deleted
        flag = besStage.run();
        double besStageScore = (GESThread.scoreGraph(besStage.getGraphs().get(0), problem) + GESThread.scoreGraph(besStage.getGraphs().get(1), problem)) / 2;

        assertFalse(flag);
        assertEquals(nThreads, besStage.getGraphs().size());
        assertNotNull(besStage.getGraphs().get(0));
        assertNotNull(besStage.getGraphs().get(1));
        assertTrue(besStageScore >= fesFusionScore);

        //TESTING BESFusion
        Stage besFusion = new BESFusion(problem, besStage.getCurrentGraph(), besStage.getGraphs());
        flag = besFusion.run();
        Graph g2 = besFusion.getCurrentGraph();
        double besFusionScore = GESThread.scoreGraph(g2, problem);

        assertTrue(flag);
        assertNotNull(g2);
        assertTrue(g2 instanceof Dag);
        assertEquals(g2.getNumNodes(), problem.getVariables().size());
        assertTrue(besFusionScore >= besStageScore);

        //SECOND ITERATION
        Stage fesStage2 = new FESStage(problem,
                g2,
                nThreads,
                itInterleaving,
                subsets);
        flag = fesStage2.run();
        double fesStageScore2 = (GESThread.scoreGraph(fesStage2.getGraphs().get(0), problem) + GESThread.scoreGraph(fesStage2.getGraphs().get(1), problem)) / 2;
        //Assert
        // No new edges added
        assertTrue(flag);
        assertEquals(nThreads, fesStage2.getGraphs().size());
        assertNotNull(fesStage2.getGraphs().get(0));
        assertNotNull(fesStage2.getGraphs().get(1));
        assertTrue(fesStageScore2 >= besFusionScore);


    }

}
