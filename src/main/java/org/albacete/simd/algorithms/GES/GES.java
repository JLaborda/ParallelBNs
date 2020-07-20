package org.albacete.simd.algorithms.GES;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.LocalScoreCache;
import org.albacete.simd.algorithms.pGESv2.*;
import org.albacete.simd.utils.Utils;

import java.util.*;

public class GES {

    private ThFES fes;
    private ThBES bes;
    private Graph initialDag;
    private long elapsedTime;
    private double modelBDeu;

    private int maxIt = 15;
    private ArrayList<TupleNode> combinations = new ArrayList<>();
    private Graph graph;
    private Problem problem;


    public GES(DataSet dataSet){

        problem = new Problem(dataSet);
        // Getting the complete set of arc combinations.
        TupleNode[] arcs = Utils.calculateArcs(problem.getData());
        Collections.addAll(combinations, arcs);

        this.initialDag = new EdgeListGraph(new LinkedList<>(problem.getVariables()));

    }

    public GES(DataSet dataSet, Graph initialDag){
        this(dataSet);
        this.initialDag = initialDag;
    }

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

        // Doing forward search
        System.out.println("FES stage: ");
        fes = new ThFES(problem, combinations,100);
        fes.run();
        graph = fes.getCurrentGraph();


        // Doing backward search
        System.out.println("BES stage: ");
        bes = new ThBES(problem, graph, combinations);
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


    public synchronized Graph getCurrentGraph() throws InterruptedException {
        while(graph == null){
            wait();
        }
        return this.graph;
    }

    public Problem getProblem() {
        return problem;
    }
}
