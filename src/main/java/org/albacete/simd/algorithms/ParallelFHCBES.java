package org.albacete.simd.algorithms;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.framework.*;
import org.albacete.simd.utils.Utils;

public class ParallelFHCBES extends BNBuilder {

    private boolean fhcFlag = false;
    private boolean besFlag = false;

    public ParallelFHCBES(DataSet data, int nThreads, int maxIterations, int nItInterleaving) {
        super(data, nThreads, maxIterations, nItInterleaving);
    }

    public ParallelFHCBES(String path, int nThreads, int maxIterations, int nItInterleaving) {
        super(path, nThreads, maxIterations, nItInterleaving);
    }

    @Override
    protected boolean convergence() {
        // Checking Iterations
        if (it >= this.maxIterations)
            return true;

        //System.out.println("FHCFlag: " + fhcFlag);
        //System.out.println("BHCFlag: " + bhcFlag);


        // Checking working status
        if(!fhcFlag && !besFlag){
            return true;
        }

        it++;
        return false;
    }

    @Override
    protected void initialConfig() {

    }

    @Override
    protected void repartition() {
        // Random Partition
        subSets = Utils.split(setOfArcs, nThreads);
    }

    @Override
    protected void forwardStage() throws InterruptedException {
        Stage fhcStage = new FHCStage(problem, currentGraph, nThreads, nItInterleaving, subSets);
        fhcFlag = fhcStage.run();
        graphs = fhcStage.getGraphs();
    }

    @Override
    protected void forwardFusion() throws InterruptedException {
        Stage fhcFusion = new FHCFusion(problem, currentGraph, graphs);
        fhcFusion.run();
        currentGraph = fhcFusion.getCurrentGraph();
    }

    @Override
    protected void backwardStage() throws InterruptedException {
        Stage besStage = new BESStage(problem, currentGraph, nThreads, nItInterleaving, subSets);
        besFlag = besStage.run();
        graphs = besStage.getGraphs();
    }

    @Override
    protected void backwardFusion() throws InterruptedException {
        Stage besFusion = new BESFusion(problem, currentGraph, graphs);
        besFusion.run();
        currentGraph = besFusion.getCurrentGraph();
    }


}
