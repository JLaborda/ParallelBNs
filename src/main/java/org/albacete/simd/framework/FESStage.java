package org.albacete.simd.framework;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.List;

public class FESStage extends ThreadStage{

    public FESStage(Problem problem, int nThreads, int itInterleaving, List<List<Edge>> subsets) {
        super(problem, nThreads, itInterleaving, subsets);
    }

    public FESStage(Problem problem, Graph currentGraph, int nThreads, int itInterleaving, List<List<Edge>> subsets) {
        super(problem, currentGraph, nThreads, itInterleaving, subsets);
    }

    @Override
    public boolean run() {
        config();
        try {
            runThreads();
            flag = checkWorkingStatus();
            return flag;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void config() {
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();

        // Rebuilding hashIndex
        //problem.buildIndexing(currentGraph);

        // Creating each ThFES runnable
        if (this.currentGraph == null) {
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new FESThread(this.problem, this.subsets.get(i), this.itInterleaving);
            }
        }
        else{
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new FESThread(this.problem, this.currentGraph, this.subsets.get(i), this.itInterleaving);
            }
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            // Resetting the search flag
            this.gesThreads[i].resetFlag();
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }
}
