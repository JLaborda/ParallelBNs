package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.ClusteringBES;
import org.albacete.simd.framework.*;

public class PGESwithStages extends BNBuilder {

    private boolean fesFlag = false;
    private boolean besFlag = false;
    
    private FESStage fesStage;
    private BESStage besStage;

    private Clustering clusteringFES;
    private ClusteringBES clusteringBES;



    public PGESwithStages(DataSet data, Clustering clusteringFES, ClusteringBES clusteringBES, int nThreads, int maxIterations, int nItInterleaving) {
        super(data, nThreads, maxIterations, nItInterleaving);
        this.clusteringFES = clusteringFES;
        this.clusteringBES = clusteringBES;
        this.clusteringFES.setProblem(super.getProblem());
    }

    public PGESwithStages(String path, Clustering clusteringFES, ClusteringBES clusteringBES, int nThreads, int maxIterations, int nItInterleaving) {
        super(path, nThreads, maxIterations, nItInterleaving);
        this.clusteringFES = clusteringFES;
        this.clusteringBES = clusteringBES;
        this.clusteringFES.setProblem(super.getProblem());
    }

    public PGESwithStages(Graph initialGraph, String path, Clustering clusteringFES, ClusteringBES clusteringBES, int nThreads, int maxIterations, int nItInterleaving) {
        super(initialGraph, path, nThreads, maxIterations, nItInterleaving);
        this.clusteringFES = clusteringFES;
        this.clusteringBES = clusteringBES;
        this.clusteringFES.setProblem(super.getProblem());
    }

    public PGESwithStages(Graph initialGraph, DataSet data, Clustering clusteringFES, ClusteringBES clusteringBES, int nThreads, int maxIterations, int nItInterleaving) {
        super(initialGraph, data, nThreads, maxIterations, nItInterleaving);
        this.clusteringFES = clusteringFES;
        this.clusteringBES = clusteringBES;
        this.clusteringFES.setProblem(super.getProblem());
    }

    @Override
    protected boolean convergence() {
        // Checking Iterations
        if (it >= this.maxIterations)
            return true;

        System.out.println("      Comprobando convergencia. FES: " + fesFlag + ", BES: " + besFlag);
        // Checking working status
        if(!fesFlag && !besFlag){
            return true;
        }
        it++;
        System.out.println("\n\nIterations: " + it);
        return false;
    }

    @Override
    protected void initialConfig() {

    }

    @Override
    protected void repartition() {
        this.subSets = clusteringFES.generateEdgeDistribution(nThreads, false);
    }

    @Override
    protected void forwardStage() throws InterruptedException {
        fesStage = new FESStage(problem, currentGraph,nThreads,nItInterleaving, subSets);
        fesFlag = fesStage.run();
        graphs = fesStage.getGraphs();
    }

    @Override
    protected void forwardFusion() throws InterruptedException {
        FESFusion fesFusion = new FESFusion(problem, currentGraph, graphs, fesStage);
        fesFusion.run();
        fesFlag = fesFusion.flag;
        currentGraph = fesFusion.getCurrentGraph();
    }

    @Override
    protected void backwardStage() throws InterruptedException {
        if (clusteringBES != null) {
            this.clusteringBES.setProblem(super.getProblem());
            this.clusteringBES.setGraph(currentGraph);
            besStage = new BESStage(problem, currentGraph, nThreads, nItInterleaving, clusteringBES.generateEdgeDistribution(nThreads, false));
        } else
            besStage = new BESStage(problem, currentGraph, nThreads, nItInterleaving, subSets);
        besFlag = besStage.run();
        graphs = besStage.getGraphs();
    }

    @Override
    protected void backwardFusion() throws InterruptedException {
        BESFusion besFusion = new BESFusion(problem, currentGraph, graphs, besStage);
        besFusion.run();
        besFlag = besFusion.flag;
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
