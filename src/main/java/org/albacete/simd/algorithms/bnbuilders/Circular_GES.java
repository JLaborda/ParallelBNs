package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;

import consensusBN.circularFusion.CircularDag;
import consensusBN.circularFusion.CircularFusionSupplier;
import consensusBN.circularFusion.Convergence;

import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.framework.BNBuilder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Circular_GES extends BNBuilder {

    private ConcurrentHashMap<Integer, Set<Edge>> subsetEdges;
    private final Clustering clustering;
    private final ConcurrentHashMap<Integer, Dag> circularFusionThreadsResults;
    private final ExecutorService executor;
    private CircularDag bestDag;
    
    private final Convergence hasConverged = new Convergence();
    private int convergenceCounter = 0;
    private final Object finishLock;

    public Circular_GES(String path, Clustering clustering, int nThreads){
        super(path, nThreads, -1, -1);
       
        this.clustering = clustering;
        this.circularFusionThreadsResults = new ConcurrentHashMap<>(nThreads);
        this.executor = Executors.newWorkStealingPool(nThreads);
        
        finishLock = new Object();
    }
    
    public Circular_GES(DataSet data, Clustering clustering, int nThreads){
        super(data, nThreads, -1, -1);

        this.clustering = clustering;
        this.circularFusionThreadsResults = new ConcurrentHashMap<>(nThreads);
        this.executor = Executors.newWorkStealingPool(nThreads);
        
        finishLock = new Object();
    }
   

    @Override
    protected void initialConfig() {
        it = 0;
        repartition();
        initializeValuesInResultsMap();
        createInitialFusionThreads();
    }

    @Override
    protected void repartition() {
        // Splitting edges with the clustering algorithm and then adding them to its corresponding index
        clustering.setProblem(this.problem);
        List<Set<Edge>> subsetEdgesList = clustering.generateEdgeDistribution(nThreads, false);

        subsetEdges = new ConcurrentHashMap<>(nThreads);
        for (int i = 0; i < subsetEdgesList.size(); i++) {
            subsetEdges.put(i, subsetEdgesList.get(i));
        }
    }

    @Override
    protected void forwardStage() throws InterruptedException {
    }

    @Override
    protected void forwardFusion() throws InterruptedException {
    }

    @Override
    protected void backwardStage() throws InterruptedException {
    }

    @Override
    protected void backwardFusion() throws InterruptedException {
    }
    
    @Override
    protected boolean convergence() {
        return convergenceCounter >= nThreads;
    }
    
    @Override
    public Graph search(){
        initialConfig();
        hasConverged.waitConverge();
        currentGraph = bestDag.dag;
        it /= nThreads;
        it++;
        return currentGraph;
    }

    
    private void initializeValuesInResultsMap(){
        for (int i = 0; i < nThreads; i++) {
            circularFusionThreadsResults.put(i, new Dag(problem.getVariables()));
        }
    }

    private void createInitialFusionThreads(){
        for (int i = 0; i < nThreads; i++) {
            addNewSupplier(i);
        }
    }
    
    private void addNewSupplier(int index){
        CircularFusionSupplier thread;
        Dag initialDag = circularFusionThreadsResults.get(index);
        Dag inputDag = getInputDag(index);
        Set<Edge> subset = subsetEdges.get(index);
        thread = new CircularFusionSupplier(problem, initialDag, inputDag, subset, index);
        submitThread(thread);
    }

    private Dag getInputDag(int i) {
        Dag inputDag;
        if(i ==0){
            inputDag = circularFusionThreadsResults.get(nThreads - 1);
        }
        else {
            inputDag = circularFusionThreadsResults.get(i - 1);
        }
        return inputDag;
    }


    private void submitThread(CircularFusionSupplier thread){
        // Submitting threads and then adding a listener to add more threads if necessary
        CompletableFuture<CircularDag> submitter = CompletableFuture.supplyAsync(thread, executor);
        submitter.thenAccept(this::listener);
    }


    private void listener(CircularDag dag){
        calculateBestGraph(dag);
        if(!convergence(dag)) {
            if(dag.id == nThreads-1) {
                addNewSupplier(0);
            } else {
                addNewSupplier(dag.id + 1);
            }
        }

    }

    protected boolean convergence(CircularDag dag) {
        it++;
        int id = dag.id;
        Dag resultingDag = dag.dag;
        this.circularFusionThreadsResults.replace(id,resultingDag);

        synchronized(finishLock) {
            if(dag.convergence){
                convergenceCounter++;
            }
            
            if(it % nThreads == 0) {
                if(convergenceCounter >= nThreads){
                    hasConverged.converged();
                } else {
                    convergenceCounter = 0;
                }
                
                finishLock.notifyAll();
            } else {
                try {
                    finishLock.wait();
                } catch (InterruptedException ex) {}
            }
        }
     
        return (convergenceCounter >= nThreads);
    }
    
    public void calculateBestGraph(CircularDag dag){
        if (bestDag == null)
            bestDag = dag;
        else{
            if (dag.getBDeu() > bestDag.getBDeu())
                bestDag = dag;
        }
    }
}
