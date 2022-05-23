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
    private boolean hasConverged = false;
    private int convergenceCounter = 0;

    public CircularFusion(String path, int numberOfThreads, Clustering clustering){
        this.problem = new Problem(path);
        this.circularFusionThreadsList = new ArrayList<>(numberOfThreads);
        this.clustering = clustering;
        this.numberOfThreads = numberOfThreads;
        this.circularFusionThreadsResults = new ConcurrentHashMap<>(numberOfThreads);
        this.executor = Executors.newWorkStealingPool(numberOfThreads);
    }

    public Dag union(){
        setup();
        runThreads();
        return fusionDag;
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
            circularFusionThreadsResults.put(i, new Dag());
        }
    }

    private void createInitialFusionThreads(){
        for (int i = 0; i < numberOfThreads; i++) {
            addNewSupplier(i);
        }
    }

    private void addNewSupplier(int index){
        CircularFusionSupplier thread;
        Dag initialDag = circularFusionThreadsResults.get(index);
        Dag inputDag = getInputDag(index);
        Set<Edge> subset = subsetEdges.get(index);
        thread = new CircularFusionSupplier(problem, initialDag, inputDag, subset, index);
        circularFusionThreadsList.add(thread);
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

    private void runThreads(){
        while(!hasConverged) {
            submitThreads();
            clearThreadList();
        }
        // Setting fusion dag
        fusionDag = circularFusionThreadsResults.get(0);
    }

    private void submitThreads(){
        // Submitting threads and then adding a listener to add more threads if necessary
        for (CircularFusionSupplier thread : circularFusionThreadsList) {
            CompletableFuture<CircularDag> submitter = CompletableFuture.supplyAsync(thread, executor);
            submitter.thenAccept(this::listener);
        }
    }

    private void clearThreadList(){
        circularFusionThreadsList.clear();
    }

    private void listener(CircularDag dag){
        updateResults(dag);
        if(!checkConvergence(dag)) {
            addNewSupplier(dag.id);
        }

    }

    private void updateResults(CircularDag dag){
        int id = dag.id;
        Dag resultingDag = dag.dag;
        this.circularFusionThreadsResults.replace(id,resultingDag);
    }

    private boolean checkConvergence(CircularDag dag){
        if(dag.convergence){
            convergenceCounter++;
            return true;
        }
        if(convergenceCounter >= numberOfThreads){
            hasConverged = true;
            return true;
        }
        return false;
    }


}
