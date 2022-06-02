package consensusBN.circularFusion;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class CircularFusion {

    private Problem problem;
    private List<CircularFusionSupplier> circularFusionThreadsList;
    private ConcurrentHashMap<Integer, Set<Edge>> subsetEdges;
    private Clustering clustering;
    private int numberOfThreads;
    private ConcurrentHashMap<Integer, Dag> circularFusionThreadsResults;
    private ExecutorService executor;
    private Dag fusionDag = null;
    private Convergence hasConverged = new Convergence();
    private int convergenceCounter = 0;
    
    private final boolean[] arrayFinishThreads;
    private final Object[] arraySynchronizeThreads;

    public CircularFusion(String path, int numberOfThreads, Clustering clustering){
        this.problem = new Problem(path);
        this.circularFusionThreadsList = new ArrayList<>(numberOfThreads);
        this.clustering = clustering;
        this.numberOfThreads = numberOfThreads;
        this.circularFusionThreadsResults = new ConcurrentHashMap<>(numberOfThreads);
        this.executor = Executors.newWorkStealingPool(numberOfThreads);
        
        this.arrayFinishThreads = new boolean[numberOfThreads];
        this.arraySynchronizeThreads = new Object[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            arrayFinishThreads[i] = true;
            arraySynchronizeThreads[i] = new Object();
        }
    }

    public Dag union(){
        setup();
        hasConverged.waitConverge();
        return circularFusionThreadsResults.get(0);
    }

    private void setup(){
        splitEdges();
        initializeValuesInResultsMap();
        createInitialFusionThreads();
    }

    private void splitEdges(){
        // Splitting edges with the clustering algorithm and then adding them to its corresponding index
        clustering.setProblem(this.problem);
        List<Set<Edge>> subsetEdgesList = clustering.generateEdgeDistribution(numberOfThreads, false);
        subsetEdges = new ConcurrentHashMap<>(numberOfThreads);
        for (int i = 0; i < subsetEdgesList.size(); i++) {
            subsetEdges.put(i, subsetEdgesList.get(i));
        }
    }

    private void initializeValuesInResultsMap(){
        for (int i = 0; i < numberOfThreads; i++) {
            circularFusionThreadsResults.put(i, new Dag(problem.getVariables()));
        }
    }

    private void createInitialFusionThreads(){
        for (int i = 0; i < numberOfThreads; i++) {
            addNewSupplier(i);
        }
    }

    private void addNewSupplier(int index){
        synchronized(arraySynchronizeThreads[index]) {
            while (!arrayFinishThreads[index]) {
                try {
                    arraySynchronizeThreads[index].wait();
                    
                } catch (InterruptedException ex) {System.out.println("EXCEPCIÓN");}
            }
        }
        
        CircularFusionSupplier thread;
        Dag initialDag = circularFusionThreadsResults.get(index);
        Dag inputDag = getInputDag(index);
        Set<Edge> subset = subsetEdges.get(index);
        thread = new CircularFusionSupplier(problem, initialDag, inputDag, subset, index);
        arrayFinishThreads[index] = false;
        submitThread(thread);
    }

    private Dag getInputDag(int i) {
        Dag inputDag;
        if(i ==0){
            inputDag = circularFusionThreadsResults.get(numberOfThreads - 1);
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
        updateResults(dag);
        if(!checkConvergence(dag)) {
            if(dag.id == numberOfThreads-1) {
                addNewSupplier(0);
            } else {
                addNewSupplier(dag.id + 1);
            }
        }

    }

    private void updateResults(CircularDag dag){
        int id = dag.id;
        Dag resultingDag = dag.dag;
        this.circularFusionThreadsResults.replace(id,resultingDag);

        synchronized(arraySynchronizeThreads[id]) {
            arrayFinishThreads[id] = true;
            arraySynchronizeThreads[id].notify();
        }
    }

    private boolean checkConvergence(CircularDag dag){
        if(dag.convergence){
            convergenceCounter++;

            if(convergenceCounter >= numberOfThreads){
                hasConverged.converged();
            }
            return true;
        }
        return false;
    }


}
