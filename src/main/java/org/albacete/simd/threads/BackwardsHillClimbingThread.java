package org.albacete.simd.threads;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.TupleNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BackwardsHillClimbingThread extends GESThread {
    private static int threadCounter = 1;


    /**
     * Constructor of ThFES with an initial DAG
     *
     * @param problem    object containing all the information of the problem
     * @param initialDag initial DAG with which the FES stage starts with, if it's null, use the other constructor
     * @param subset     subset of edges the fes stage will try to add to the resulting graph
     */
    public BackwardsHillClimbingThread(Problem problem, Graph initialDag, ArrayList<TupleNode> subset) {
        this.problem = problem;
        setInitialGraph(initialDag);
        setSubSetSearch(subset);
        setMaxIt(maxIt);
        this.id = threadCounter;
        threadCounter++;
    }

    /**
     * Constructor of FESThread with an initial DataSet
     *
     * @param problem object containing information of the problem such as data or variables.
     * @param subset  subset of edges the fes stage will try to add to the resulting graph
     */
    public BackwardsHillClimbingThread(Problem problem, ArrayList<TupleNode> subset) {
        this.problem = problem;
        this.initialDag = new EdgeListGraph(new LinkedList<>(getVariables()));
        setSubSetSearch(subset);
        setMaxIt(maxIt);
        this.id = threadCounter;
        threadCounter++;
    }


    @Override
    public void run() {
        this.currentGraph = search();
    }

    private Graph search() {
        long startTime = System.currentTimeMillis();
        numTotalCalls = 0;
        numNonCachedCalls = 0;


        Graph graph = new EdgeListGraph(this.initialDag);

        double score = scoreGraph(graph, problem);

        // Do Forwards HillClimbing
        score = bhc(graph, score);

        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;
        return graph;


    }


    private double bhc(Graph graph, double score) {


        double bestScore = score;
        List<Edge> edges = new ArrayList<>();

        for (TupleNode tupleNode : this.S) {
            Node _x = tupleNode.x;
            Node _y = tupleNode.y;

            if (_x == _y)
                continue;

            // Adding Edges to check
            edges.add(Edges.directedEdge(_x, _y));
            edges.add(Edges.directedEdge(_y, _x));
        }


        // Hillclimbing algorithm
        boolean improvement = false;
        do {
            improvement = false;
            Node bestX = null;
            Node bestY = null;
            Edge bestEdge = null;
            for (Edge edge : edges) {

                if (!graph.containsEdge(edge))
                    continue;

                Node _x = Edges.getDirectedEdgeTail(edge);
                Node _y = Edges.getDirectedEdgeHead(edge);

                SubSet subset = new SubSet();
                double deleteEval = deleteEval(_x, _y, subset, graph);
                double evalScore = score + deleteEval;

                if (evalScore > bestScore) {
                    //insert(_x, _y, subset, graph);
                    bestX = _x;
                    bestY = _y;
                    bestEdge = edge;
                    bestScore = evalScore;
                    improvement = true;
                }
            }

            if (improvement) {
                delete(bestX, bestY, new SubSet(), graph);
                this.flag = true;
                edges.remove(bestEdge);

            }

        }
        while (improvement);

        return bestScore;


    }
}
