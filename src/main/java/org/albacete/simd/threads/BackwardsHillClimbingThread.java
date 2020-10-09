package org.albacete.simd.threads;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.TupleNode;

import java.util.*;

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


    @Override
    protected double deleteEval(Node x, Node y, Set<Node> h, Graph graph) {
        Set<Node> set1 = new HashSet<>();
        set1.removeAll(h);
        set1.addAll(graph.getParents(y));
        Set<Node> set2 = new HashSet<>(set1);
        set1.remove(x);
        set2.add(x);
        return scoreGraphChange(y, set1, set2, graph, problem);
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


        System.out.println("[BHC " + getId() + "]" + " Number of edges to check: " + edges.size());


        // Hillclimbing algorithm
        boolean improvement;
        int iteration = 1;
        do {
            System.out.println("[BHC " + getId() + "]" + "iteration: " + iteration);
            improvement = false;
            Node bestX = null;
            Node bestY = null;
            Edge bestEdge = null;
            SubSet bestSubSet = null;
            for (Edge edge : edges) {

                if (!graph.containsEdge(edge))
                    continue;

                Node _x = Edges.getDirectedEdgeTail(edge);
                Node _y = Edges.getDirectedEdgeHead(edge);

                //  Set of parents except _y
                SubSet subset = new SubSet();

                double deleteEval = deleteEval(_x, _y, subset, graph);
                double evalScore = bestScore + deleteEval;

                if (evalScore > bestScore) {
                    bestX = _x;
                    bestY = _y;
                    bestEdge = edge;
                    bestScore = evalScore;
                    bestSubSet = subset;
                    improvement = true;
                }
            }

            if (improvement) {
                System.out.println("Thread BHC " + getId() + " deleting: (" + bestX + ", " + bestY + ", " + bestSubSet+ ")");
                delete(bestX, bestY, bestSubSet, graph);
                System.out.println("[BHC "+getId() + "] Score: " + nf.format(bestScore) + "\tDeleting: " + graph.getEdge(bestX, bestY));

                this.flag = true;
                edges.remove(bestEdge);

            }
            iteration++;
        }
        while (improvement);

        return bestScore;


    }
}
