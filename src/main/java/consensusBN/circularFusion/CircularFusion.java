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

        System.out.println(" Edges list: "+subsetEdgesList+ "\n");
        
        subsetEdges = new ConcurrentHashMap<>(numberOfThreads);
        for (int i = 0; i < subsetEdgesList.size(); i++) {
            subsetEdges.put(i, subsetEdgesList.get(i));
        }
        System.out.println(" Edges MAP: "+subsetEdges+ "\n");
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
        System.out.println("\n INTENTANDO AÑADIR NEW SUPPLIER  " + index);
        synchronized(arraySynchronizeThreads[index]) {
            for (int i = 0; i < arrayFinishThreads.length; i++) {
                System.out.println("FUERA DE WHILE: i " + i + ": " + arrayFinishThreads[i]);
            }
            while (!arrayFinishThreads[index]) {
                System.out.println("\n");
                for (int i = 0; i < arrayFinishThreads.length; i++) {
                    System.out.println("DENTRO DE WHILE: i " + i + ": " + arrayFinishThreads[i]);
                }
                try {
                    arraySynchronizeThreads[index].wait();
                    
                } catch (InterruptedException ex) {System.out.println("EXCEPCIÓN");}
            }
        }
        
        CircularFusionSupplier thread;
        Dag initialDag = circularFusionThreadsResults.get(index);
        Dag inputDag = getInputDag(index);
        System.out.println("\n  NEW SUPPLIER, index " + index + ", Initial DAG edges = " + initialDag.getEdges().size() + ", Input DAG edges = " + inputDag.getEdges().size() + "\n");
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
        System.out.println("Hace el submitThreads");
    }


    private void listener(CircularDag dag){
        System.out.println("Entra en el listener");
        updateResults(dag);
        if(!checkConvergence(dag)) {
            if(dag.id == numberOfThreads-1) {
                System.out.println("DAG " + dag.id + " añadiendo supplier " + 0);
                addNewSupplier(0);
            } else {
                System.out.println("DAG " + dag.id + " añadiendo supplier " + (dag.id+1));
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
            System.out.println("\n\n LIBERAMOSSSSSSSSSSSS  " + id);
            arraySynchronizeThreads[id].notify();
        }
    }

    private boolean checkConvergence(CircularDag dag){
        if(dag.convergence){
            convergenceCounter++;
            System.out.println("CONVERGEN: " + convergenceCounter + ", total: " + numberOfThreads);
            
            if(convergenceCounter >= numberOfThreads){
                System.out.println("\n\n\n\nCONVERGEEEE\n\n\n\n");
                hasConverged.converged();
            }
            return true;
        }
        return false;
    }


}
