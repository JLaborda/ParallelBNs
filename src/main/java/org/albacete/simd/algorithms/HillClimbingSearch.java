package org.albacete.simd.algorithms;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.threads.BackwardsHillClimbingThread;
import org.albacete.simd.threads.ForwardHillClimbingThread;
import org.albacete.simd.utils.Utils;

public class HillClimbingSearch extends BNBuilder {


    boolean fhcFlag = false;
    boolean bhcFlag = false;

    public HillClimbingSearch(DataSet data, int nThreads, int maxIterations, int nItInterleaving) {
        super(data, nThreads, maxIterations, nItInterleaving);
    }

    public HillClimbingSearch(String path, int nThreads, int maxIterations, int nItInterleaving) {
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
        if(!fhcFlag && !bhcFlag){
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
    }

    @Override
    protected void forwardStage() throws InterruptedException {
        Graph g = getCurrentGraph();
        ForwardHillClimbingThread fhc;
        if (g == null){
            fhc = new ForwardHillClimbingThread(getProblem(),getListOfArcs(), getItInterleaving());
        }
        else {
            fhc = new ForwardHillClimbingThread(getProblem(), getCurrentGraph(), getListOfArcs(), getItInterleaving());
        }
        fhc.run();
        fhcFlag = fhc.getFlag();
        Graph graph = fhc.getCurrentGraph();
        currentGraph = Utils.removeInconsistencies(graph);
    }

    @Override
    protected void forwardFusion() throws InterruptedException {

    }

    @Override
    protected void backwardStage() throws InterruptedException {
        BackwardsHillClimbingThread bhc = new BackwardsHillClimbingThread(getProblem(),getCurrentGraph(),getListOfArcs());
        bhc.run();
        bhcFlag = bhc.getFlag();
        Graph g = bhc.getCurrentGraph();
        currentGraph = Utils.removeInconsistencies(g);
    }

    @Override
    protected void backwardFusion() throws InterruptedException {

    }



}
