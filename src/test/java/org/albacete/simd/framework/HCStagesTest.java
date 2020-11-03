package org.albacete.simd.framework;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class HCStagesTest {
    @Test
    public void runTest() throws InterruptedException{

        //Arrange
        String path = "src/test/resources/alarm.xbif_.csv";
        Problem problem = new Problem(path);
        int nThreads = 2;
        int itInterleaving = 5;
        List<List<Edge>> subsets = Utils.split(Utils.calculateArcs(problem.getData()), nThreads);
        boolean flag;

        // FHC Stage
        Stage fhcStage = new FHCStage(problem, nThreads, itInterleaving, subsets);
        flag = fhcStage.run();
        ArrayList<Dag> graphs = fhcStage.getGraphs();
        double fhcStageScore = (GESThread.scoreGraph(graphs.get(0), problem) + GESThread.scoreGraph(graphs.get(1), problem)) / 2;

        assertTrue(flag);
        assertEquals(nThreads, fhcStage.getGraphs().size());
        assertNotNull(fhcStage.getGraphs().get(0));
        assertNotNull(fhcStage.getGraphs().get(1));

        // FHC Fusion
        Stage fhcFusion = new FHCFusion(problem, fhcStage.getCurrentGraph(), graphs);
        flag = fhcFusion.run();
        Graph g = fhcFusion.getCurrentGraph();
        double fhcFusionScore = GESThread.scoreGraph(g, problem);

        assertTrue(flag);
        assertNotNull(g);
        assertTrue(g instanceof Dag);
        assertEquals(g.getNumNodes(), problem.getVariables().size());
        assertTrue(fhcFusionScore >= fhcStageScore);

        // BHC Stage
        Stage bhcStage = new BHCStage(problem, g, nThreads, itInterleaving, subsets);
        flag = bhcStage.run();
        graphs = bhcStage.getGraphs();
        double bhcStageScore = (GESThread.scoreGraph(graphs.get(0), problem) + GESThread.scoreGraph(graphs.get(1), problem)) / 2;
        //No Edge is deleted
        assertFalse(flag);
        assertEquals(nThreads, bhcStage.getGraphs().size());
        assertNotNull(bhcStage.getGraphs().get(0));
        assertNotNull(bhcStage.getGraphs().get(1));
        assertTrue(bhcStageScore >= fhcFusionScore);

        // BHC Fusion
        Stage bhcFusion = new BHCFusion(problem, bhcStage.getCurrentGraph(), graphs);
        flag = bhcFusion.run();
        g = bhcFusion.getCurrentGraph();
        double bhcFusionScore = GESThread.scoreGraph(g, problem);

        assertTrue(flag);
        assertNotNull(g);
        assertTrue(g instanceof Dag);
        assertEquals(g.getNumNodes(), problem.getVariables().size());
        assertTrue(bhcFusionScore >= bhcStageScore);

        // FHC Stage 2
        Stage fhcStage2 = new FHCStage(problem, g, nThreads, itInterleaving, subsets);
        flag = fhcStage2.run();
        graphs = fhcStage2.getGraphs();
        double fhcStageScore2 = (GESThread.scoreGraph(graphs.get(0), problem) + GESThread.scoreGraph(graphs.get(1), problem)) / 2;

        // No new Edges added
        assertTrue(flag);
        assertEquals(nThreads, fhcStage2.getGraphs().size());
        assertNotNull(fhcStage2.getGraphs().get(0));
        assertNotNull(fhcStage2.getGraphs().get(1));
        assertTrue(fhcStageScore2 >= bhcFusionScore);

    }
}
