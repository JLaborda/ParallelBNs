package org.albacete.simd.algorithms;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph_n;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.LinkedList;
import java.util.Set;

public class GES{

    private FESThread fes;
    private BESThread bes;
    private Graph initialDag;
    private long elapsedTime;
    private double modelBDeu;
    //private int interleaving = Integer.MAX_VALUE;
    private Set<Edge> combinations;
    private Graph graph;
    private Problem problem;


    public GES(DataSet dataSet){
        this.problem = new Problem(dataSet);
        this.combinations = Utils.calculateArcs(problem.getData());
        this.initialDag = new EdgeListGraph_n(new LinkedList<>(problem.getVariables()));
    }

    public GES(DataSet dataSet, Graph initialDag){
        this(dataSet);
        this.initialDag = initialDag;
    }
/*
    public GES(DataSet dataSet, int interleaving){
        this(dataSet);
        this.interleaving = interleaving;
    }

    public GES(DataSet dataSet, Graph initialDag, int interleaving){
        this(dataSet, initialDag);
        this.interleaving = interleaving;
    }
*/

/*
    */
/**
     * Greedy equivalence search: Start from the empty graph, add edges till
     * model is significant. Then start deleting edges till a minimum is
     * achieved.
     *
     * @return the resulting Pattern.
     *//*

    public Graph search() throws InterruptedException {
        long startTime = System.currentTimeMillis();


        //buildIndexing(graph);

        // Method 1-- original.
        double score = 0;

        // Doing forward search
        System.out.println("FES stage: ");
        fes = new FESThread(problem, combinations,100);
        fes.run();
        graph = fes.getCurrentGraph();


        // Doing backward search
        System.out.println("BES stage: ");
        bes = new BESThread(problem, graph, combinations);
        bes.run();
        graph = bes.getCurrentGraph();
        score = bes.getScoreBDeu();

        // Transforming the resulting graph into a DAG
        graph = Utils.removeInconsistencies(graph);

        // Measuring stats
        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;


        return graph;
    }
*/

    /**
     * Greedy equivalence search: Start from the empty graph, add edges till
     * model is significant. Then start deleting edges till a minimum is
     * achieved.
     *
     * @return the resulting Pattern.
     */
    public Graph search() throws InterruptedException {
        long startTime = System.currentTimeMillis();


        //buildIndexing(graph);

        // Method 1-- original.
        double score = 0;

        // Doing forward search without interleaving
        System.out.println("FES stage: ");
        fes = new FESThread(problem, initialDag, combinations, Integer.MAX_VALUE);
        fes.run();
        graph = fes.getCurrentGraph();


        // Doing backward search
        System.out.println("BES stage: ");
        bes = new BESThread(problem, graph, combinations);
        bes.run();
        graph = bes.getCurrentGraph();
        score = bes.getScoreBDeu();

        // Transforming the resulting graph into a DAG
        graph = Utils.removeInconsistencies(graph);

        // Measuring stats
        long endTime = System.currentTimeMillis();
        this.elapsedTime =  endTime - startTime;
        this.modelBDeu = score;
        return graph;
    }



    public synchronized Graph getCurrentGraph() throws InterruptedException {
        while(graph == null){
            wait();
        }
        return this.graph;
    }

    public Problem getProblem() {
        return problem;
    }

    public double getModelBDeu(){
        return modelBDeu;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }
}
