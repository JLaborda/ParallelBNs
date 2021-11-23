package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.framework.*;
import org.albacete.simd.utils.Utils;

public class PHC_BNBuilder extends BNBuilder{

    private boolean forwardFlag = false;
    private boolean backwardsFlag = false;

    public PHC_BNBuilder(DataSet data, int nThreads, int maxIterations, int nItInterleaving) {
        super(data, nThreads, maxIterations, nItInterleaving);
    }

    public PHC_BNBuilder(String path, int nThreads, int maxIterations, int nItInterleaving) {
        super(path, nThreads, maxIterations, nItInterleaving);
    }

    @Override
    protected boolean convergence() {
        // Checking Iterations
        if (it >= this.maxIterations)
            return true;

        // Checking working status
        if(!forwardFlag && !backwardsFlag){
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
        Stage stage = new FHCStage(problem, currentGraph, nThreads, nItInterleaving, subSets);
        //Stage fesStage = new FESStage(problem, currentGraph,nThreads,nItInterleaving, subSets);
        forwardFlag = stage.run();
        graphs = stage.getGraphs();
    }

    @Override
    protected void forwardFusion() throws InterruptedException {
        Stage fusion = new FHCFusion(problem, currentGraph, graphs);
        fusion.run();
        currentGraph = fusion.getCurrentGraph();
    }

    @Override
    protected void backwardStage() throws InterruptedException {
        Stage stage = new BHCStage(problem, currentGraph, nThreads, nItInterleaving, subSets);
        backwardsFlag = stage.run();
        graphs = stage.getGraphs();
    }

    @Override
    protected void backwardFusion() throws InterruptedException {
        Stage fusion = new BHCFusion(problem, currentGraph, graphs);
        fusion.run();
        currentGraph = fusion.getCurrentGraph();
    }

    public static void main(String[] args){
        // 1. Read Data
        String path = "src/test/resources/alarm.xbif_.csv";
        DataSet ds = Utils.readData(path);

        // 2. Configuring algorithm
        PHC_BNBuilder alg= new PHC_BNBuilder(ds, 2, 15, 5);

        // 3. Running Algorithm
        alg.search();

        // 4. Printing out the results
        System.out.println("Number of Iterations: " + alg.getIterations());
        System.out.println("Resulting Graph: " + alg.getCurrentGraph());


    }

}
