package org.albacete.simd.threads;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.TupleNode;

import java.util.*;

public class ForwardHillClimbingThread extends GESThread {

    private static int threadCounter = 1;


    /**
     * Constructor of ThFES with an initial DAG
     *
     * @param problem    object containing all the information of the problem
     * @param initialDag initial DAG with which the FES stage starts with, if it's null, use the other constructor
     * @param subset     subset of edges the fes stage will try to add to the resulting graph
     * @param maxIt      maximum number of iterations allowed in the fes stage
     */
    public ForwardHillClimbingThread(Problem problem, Graph initialDag, ArrayList<TupleNode> subset, int maxIt) {
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
     * @param maxIt   maximum number of iterations allowed in the fes stage
     */
    public ForwardHillClimbingThread(Problem problem, ArrayList<TupleNode> subset, int maxIt) {
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
        numTotalCalls=0;
        numNonCachedCalls=0;


        Graph graph = new EdgeListGraph(this.initialDag);

        double score = scoreGraph(graph, problem);

        // Do Forwards HillClimbing
        score = fhc(graph, score);

        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;
        return graph;


    }

    private double fhc(Graph graph, double score){


        double bestScore = score;
        List<Edge> edges = new ArrayList<>();

        for(TupleNode tupleNode : this.S){
            Node _x = tupleNode.x;
            Node _y = tupleNode.y;

            if(_x == _y)
                continue;

            // Adding Edges to check
            edges.add(Edges.directedEdge(_x, _y));
            edges.add(Edges.directedEdge(_y, _x));
        }


        // Hillclimbing algorithm
        boolean improvement = false;
        int iteration = 1;
        do{
            improvement = false;
            Node bestX = null;
            Node bestY = null;
            Edge bestEdge = null;
            SubSet bestSubSet = null;
            for(Edge edge : edges) {

                if (graph.containsEdge(edge))
                    continue;


                Node _x = Edges.getDirectedEdgeTail(edge);
                Node _y = Edges.getDirectedEdgeHead(edge);

                if (graph.isAdjacentTo(_x, _y)) {
                    continue;
                }

                // Comprobar ciclos dirigidos aquí?
                //if(graph.existsDirectedPathFromTo(_x, _y)) {
                //    continue;
                //}

                // Selecting parents of the head (_y)
                SubSet subset = new SubSet();
                subset.addAll((graph.getParents(_y)));
                subset.remove(_x);

                double insertEval = insertEval(_x, _y, subset, graph, problem, false);
                double evalScore = score + insertEval;

                if (evalScore > bestScore) {
                    //insert(_x, _y, subset, graph);
                    bestX = _x;
                    bestY = _y;
                    bestEdge = edge;
                    bestScore = evalScore;
                    bestSubSet = subset;
                    improvement = true;
                }
            }

            if(improvement){
                // Checking directed cycles
                if(!graph.existsDirectedPathFromTo(bestX, bestY)) {
                    insert(bestX, bestY, bestSubSet, graph);
                    this.flag = true;
                }
                edges.remove(bestEdge);
            }
        iteration++;
        }
        while(improvement && iteration <= this.maxIt);

        return bestScore;
    }
}
