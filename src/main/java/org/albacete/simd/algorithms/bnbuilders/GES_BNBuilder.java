package org.albacete.simd.algorithms.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;

import java.util.LinkedList;

public class GES_BNBuilder extends BNBuilder {


    private Graph initialDag;

    public GES_BNBuilder(DataSet data) {
        super(data, -1, -1, -1);
        initialDag = new EdgeListGraph(new LinkedList<>(problem.getVariables()));
    }

    public GES_BNBuilder(String path) {
        super(path, -1, -1, -1);
        initialDag = new EdgeListGraph(new LinkedList<>(problem.getVariables()));
    }

    public GES_BNBuilder(Graph initialDag, DataSet data) {
        this(data);
        this.initialDag = new EdgeListGraph(initialDag);
    }

    public GES_BNBuilder(Graph initialDag, String path) {
        this(path);
        this.initialDag = new EdgeListGraph(initialDag);
    }

    @Override
    protected boolean convergence() {
        return true;
    }

    @Override
    protected void initialConfig() {
    }

    @Override
    protected void repartition() {

    }

    @Override
    protected void forwardStage() throws InterruptedException {
        FESThread fes = new FESThread(problem, initialDag, listOfArcs, Integer.MAX_VALUE);
        fes.run();
        currentGraph = fes.getCurrentGraph();
        score = fes.getScoreBDeu();
    }

    @Override
    protected void forwardFusion() throws InterruptedException {

    }

    @Override
    protected void backwardStage() throws InterruptedException {
        BESThread bes = new BESThread(problem, initialDag, listOfArcs);
        bes.run();
        currentGraph = bes.getCurrentGraph();
        score = bes.getScoreBDeu();
        currentGraph = Utils.removeInconsistencies(currentGraph);
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
