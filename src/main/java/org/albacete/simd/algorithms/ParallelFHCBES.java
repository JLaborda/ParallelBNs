package org.albacete.simd.algorithms;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.algorithms.framework.BNBuilder;
import org.albacete.simd.algorithms.framework.stages.*;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.ForwardHillClimbingThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;

import java.util.ArrayList;
import java.util.List;

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
        subSets = Utils.split(listOfArcs, nThreads);
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
