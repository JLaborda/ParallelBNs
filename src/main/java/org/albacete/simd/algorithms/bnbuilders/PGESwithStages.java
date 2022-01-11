package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.framework.*;
import org.albacete.simd.utils.Utils;

public class PGESwithStages extends BNBuilder {

    private boolean fesFlag = false;
    private boolean besFlag = false;

    public PGESwithStages(DataSet data, int nThreads, int maxIterations, int nItInterleaving) {
        super(data, nThreads, maxIterations, nItInterleaving);
    }

    public PGESwithStages(String path, int nThreads, int maxIterations, int nItInterleaving) {
        super(path, nThreads, maxIterations, nItInterleaving);
    }

    @Override
    protected boolean convergence() {
        // Checking Iterations
        if (it >= this.maxIterations)
            return true;

        // Checking working status
        if(!fesFlag && !besFlag){
            return true;
        }
        it++;
        System.out.println("Iterations: " + it);
        return false;
    }

    @Override
    protected void initialConfig() {

    }

    @Override
    protected void repartition() {
        this.subSets = Utils.split(listOfArcs, nThreads);
    }

    @Override
    protected void forwardStage() throws InterruptedException {
        Stage fesStage = new FESStage(problem, currentGraph,nThreads,nItInterleaving, subSets);
        fesFlag = fesStage.run();
        graphs = fesStage.getGraphs();
    }

    @Override
    protected void forwardFusion() throws InterruptedException {
        Stage fesFusion = new FESFusion(problem, currentGraph, graphs);
        fesFusion.run();
        currentGraph = fesFusion.getCurrentGraph();
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
/*
    public static void main(String[] args){
        // 1. Read Data
        String path = "src/test/resources/alarm.xbif_.csv";
        DataSet ds = Utils.readData(path);

        // 2. Configuring algorithm
        PGESwithStages pGESv2= new PGESwithStages(ds, 2, 15, 5);

        // 3. Running Algorithm
        pGESv2.search();

        // 4. Printing out the results
        System.out.println("Number of Iterations: " + pGESv2.getIterations());
        System.out.println("Resulting Graph: " + pGESv2.getCurrentGraph());


    }
 */
}
