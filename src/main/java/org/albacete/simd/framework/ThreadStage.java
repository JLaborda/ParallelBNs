package org.albacete.simd.framework;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.List;

public abstract class ThreadStage extends Stage{
    /**
     * The {@link GESThread GESThread} array that will be executed in each stage.
     * They can either be {@link FESThread ThFES} or {@link BESThread ThBES} threads.
     */
    protected GESThread[] gesThreads = null;

    /**
     * The {@link Thread Thread} array that encapsulate the {@link GESThread GESThread} for each stage.
     */
    protected Thread[] threads = null;

    protected boolean flag = false;

    protected int itInterleaving;

    protected int nThreads;

    protected List<List<Edge>> subsets;

    public ThreadStage(Problem problem, int nThreads, int itInterleaving, List<List<Edge>> subsets){
        super(problem);
        this.nThreads = nThreads;
        this.threads = new Thread[nThreads];
        this.gesThreads = new GESThread[nThreads];
        this.itInterleaving = itInterleaving;
        this.subsets = subsets;
    }

    public ThreadStage(Problem problem, Graph currentGraph, int nThreads, int itInterleaving, List<List<Edge>> subsets) {
        super(problem, currentGraph);
        this.currentGraph = currentGraph;
        this.nThreads = nThreads;
        this.threads = new Thread[nThreads];
        this.gesThreads = new GESThread[nThreads];
        this.itInterleaving = itInterleaving;
        this.subsets = subsets;
    }

    /**
     * Executing the threads for the corresponding stage
     * @throws InterruptedException Exception caused by an external interruption.
     */
    protected void runThreads() throws InterruptedException {
        // Starting the threads
        for (Thread thread: this.threads) {
            thread.start();
        }

        // Getting results
        double score_threads = 0;
        for(int i = 0 ; i< this.threads.length; i++){
            // Joining threads and getting currentGraph
            threads[i].join();
            Graph g = gesThreads[i].getCurrentGraph();

            // Thread Score
            score_threads = score_threads + gesThreads[i].getScoreBDeu();

            // Removing Inconsistencies and transforming it to a DAG
            Dag gdag = Utils.removeInconsistencies(g);

            // Adding the new dag to the graph list
            this.graphs.add(gdag);

            //Debug
            //System.out.println("Graph of Thread " + (i +1) + ": \n" + gdag);

        }

    }

    protected abstract void config();


    public boolean checkWorkingStatus() throws InterruptedException {
        for (GESThread g: gesThreads) {
            if (g.getFlag() ){
                return true;
            }
        }
        return false;
    }



}
