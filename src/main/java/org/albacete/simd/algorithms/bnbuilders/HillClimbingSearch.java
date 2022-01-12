package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.threads.BackwardsHillClimbingThread;
import org.albacete.simd.threads.ForwardHillClimbingThread;
import org.albacete.simd.utils.Utils;

public class HillClimbingSearch extends BNBuilder {


    boolean fhcFlag = false;
    boolean bhcFlag = false;

    public HillClimbingSearch(String path, int maxIterations, int nItInterleaving) {
        super(path, 0, maxIterations, nItInterleaving);
    }

    public HillClimbingSearch(DataSet data, int maxIterations, int nItInterleaving) {
        super(data, 0, maxIterations, nItInterleaving);
    }

    public HillClimbingSearch(DataSet data) {
        super(data, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public HillClimbingSearch(String path) {
        super(path, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public HillClimbingSearch(Graph initialGraph, String path) {
        super(initialGraph, path, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public HillClimbingSearch(Graph initialGraph, DataSet data){
        super(initialGraph, data, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }



    @Override
    protected boolean convergence() {
        return false;
    }

    @Override
    protected void initialConfig() {
    }

    @Override
    protected void repartition() {
    }

    @Override
    protected void forwardStage() throws InterruptedException {
        Graph g = getCurrentGraph();
        ForwardHillClimbingThread fhc;
        if (g == null){
            fhc = new ForwardHillClimbingThread(getProblem(),getListOfArcs(), getItInterleaving());
        }
        else {
            fhc = new ForwardHillClimbingThread(getProblem(), getCurrentGraph(), getListOfArcs(), getItInterleaving());
        }
        fhc.run();
        fhcFlag = fhc.getFlag();
        Graph graph = fhc.getCurrentGraph();
        currentGraph = Utils.removeInconsistencies(graph);
    }

    @Override
    protected void forwardFusion() throws InterruptedException {

    }

    @Override
    protected void backwardStage() throws InterruptedException {
        BackwardsHillClimbingThread bhc = new BackwardsHillClimbingThread(getProblem(),getCurrentGraph(),getListOfArcs());
        bhc.run();
        bhcFlag = bhc.getFlag();
        Graph g = bhc.getCurrentGraph();
        currentGraph = Utils.removeInconsistencies(g);
    }

    @Override
    protected void backwardFusion() throws InterruptedException {

    }

    @Override
    public Graph search(){
        try {
            forwardStage();
            backwardStage();
        }catch(InterruptedException e){
            System.err.println("Interrupted Exception");
            e.printStackTrace();
        }
        return this.currentGraph;
    }
}
