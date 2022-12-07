package org.albacete.simd.algorithms.bnbuilders;

import consensusBN.circularFusion.CircularDag;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.framework.BNBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class Circular_GES extends BNBuilder {
    
    public static final String EXPERIMENTS_FOLDER = "./experiments/";
    private ConcurrentHashMap<Integer, Set<Edge>> subsetEdges;
    private final Clustering clustering;
    private final ConcurrentHashMap<Integer, CircularDag> circularFusionThreadsResults;
    private CircularDag bestDag;
    private double lastBestBDeu;
    private boolean convergence;

    public Circular_GES(String path, Clustering clustering, int nThreads, int nItInterleaving) {
        super(path, nThreads, -1, nItInterleaving);
       
        this.clustering = clustering;
        this.circularFusionThreadsResults = new ConcurrentHashMap<>(nThreads);
    }
    
    public Circular_GES(DataSet data, Clustering clustering, int nThreads, int nItInterleaving) {
        super(data, nThreads, -1, nItInterleaving);

        this.clustering = clustering;
        this.circularFusionThreadsResults = new ConcurrentHashMap<>(nThreads);
    }
    
   
    @Override
    public Graph search(){
        //1. Setup
        initialConfig();
        //2. Do circular fusion while convergence is false
        do {
            iteration();
        } while (!convergence());

        //3. Print and return last graph
        printResults();
        calculateBestGraph();
        currentGraph = bestDag.dag;
        return currentGraph;
    }
    
    @Override
    protected void initialConfig() {
        it = 0;
        repartition();
        initializeValuesInResultsMap();
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

    private void initializeValuesInResultsMap(){
        for (int i = 0; i < nThreads; i++) {
            circularFusionThreadsResults.put(i, new CircularDag(problem,subsetEdges.get(i),nItInterleaving,i));
        }
    }
    
    private void iteration() {
        it++;
        circularFusionThreadsResults.values().parallelStream().forEach((dag) -> {
            try {
                dag.fusionGES(getInputDag(dag.id));
            } catch (InterruptedException ex) {
                System.out.println("Error with InterruptedException: " +
                        "\n Dag Id: " + dag.id +
                        "\n Dag graph: " + dag.dag);
            }
        });
    }

    private CircularDag getInputDag(int i) {
        if(i == 0) {
            return circularFusionThreadsResults.get(nThreads - 1);
        } else {
            return circularFusionThreadsResults.get(i - 1);
        }
    }

    public void calculateBestGraph(){
        circularFusionThreadsResults.values().forEach((dag) -> {
            calculateBestGraph(dag);
        });
    }
    
    public void calculateBestGraph(CircularDag dag){
        if (bestDag == null)
            bestDag = dag;
        else{
            if (dag.getBDeu() > bestDag.getBDeu())
                bestDag = dag;
        }
    }
    
    @Override
    protected boolean convergence() {
        convergence = true;
        
        circularFusionThreadsResults.values().forEach((dag) -> {
            convergence = convergence && dag.convergence;
        });
        return convergence;
    }
    
    protected boolean convergence2() {
        circularFusionThreadsResults.values().forEach((dag) -> {
            calculateBestGraph(dag);
        });
        return lastBestBDeu <= bestDag.getBDeu();
    }
    
    private void printResults() {
        String savePath = EXPERIMENTS_FOLDER + "results.csv";
        File file = new File(savePath);
        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(file,true);
            csvWriter.append("id,stage,BDeu\n");
            
            for (int i = 0; i < nThreads; i++) {
                File doc = new File(EXPERIMENTS_FOLDER + "temp_" + i + ".csv");
                Scanner obj = new Scanner(doc);

                while (obj.hasNextLine()) {
                    csvWriter.append(obj.nextLine() + "\n");
                }
            }

            csvWriter.flush();
        } catch (IOException ex) {}
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
}