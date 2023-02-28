package org.albacete.simd.mctsbn;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.BDeuScore;
import org.albacete.simd.threads.EdgeSearch;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HillClimbingEvaluator {

    private Problem problem;

    private List<Node> order;

    private List<Edge> edges;

    private Graph graph;

    private double score = 0;

    private final static int MAX_ITERATIONS = 1000;

    private BDeuScore metric;

    private enum Operation {ADD, DELETE}

    public HillClimbingEvaluator(Problem problem, List<Node> order){
        this.problem = problem;
        this.order = order;
        this.graph = new EdgeListGraph_n();
        metric = new BDeuScore(problem.getData());
        generateEdges();
    }

    private void generateEdges(){
        edges = new ArrayList<>();
        // Given an order, parents can only go before the child nodes.
        for (int i = 0; i < order.size(); i++) {
            for (int j = i+1; j < order.size(); j++) {
                // Getting the parent and child node
                Node parent = order.get(i);
                Node child = order.get(j);

                //Creating the edge
                Edge edge = new Edge(parent, child, Endpoint.TAIL, Endpoint.ARROW);
                edges.add(edge);
            }
        }
    }

    private void evaluate(Node child, List<Node> parents, Graph graph){
        boolean improvement = true;
        double result = 0;
        int indexChild = problem.getHashIndices().get(child);

        while(improvement) {
            improvement = false;
            double bestScore = Double.NEGATIVE_INFINITY;
            Operation bestOperation = null;
            Edge bestEdge = null;

            for (Node parent : parents) {
                double score = 0;
                Operation operation = null;
                Edge edge = Edges.directedEdge(parent, child);
                int indexParent = problem.getHashIndices().get(parent);
                List<Node> parentsGraph = graph.getParents(child);
                int[] indexParentsGraph = new int[parentsGraph.size()];
                // OPERATION ADD
                if (!graph.containsEdge(edge)) {
                    // Transforming indexes of parent and child node, as well as the parents of the graph
                    int[] indexUnion = new int[parentsGraph.size() + 1];
                    for (int i = 0; i < parentsGraph.size(); i++) {
                        Node p = parentsGraph.get(i);
                        indexParentsGraph[i] = problem.getHashIndices().get(p);
                        indexUnion[i] = problem.getHashIndices().get(p);
                    }
                    // Adding the parent to the end of the indexUnion array
                    indexUnion[parentsGraph.size()] = indexParent;

                    // Calculating the score of this edge
                    // Score = localbdeu(x,P(G) + {x_p}) - localbdeu(x,P(G))
                    score = metric.localScore(indexChild, indexUnion) - metric.localScore(indexChild, indexParentsGraph);
                    operation = Operation.ADD;
                }
                // OPERATION DELETE
                else{
                    // Calculating indexes for the difference set of parents
                    List<Node> parentsAux = new ArrayList<>(parentsGraph);
                    parentsAux.remove(parent);
                    int[] indexDifference = new int[parentsGraph.size() -1];
                    for (int i = 0; i < indexDifference.length; i++) {
                        Node p = parentsAux.get(i);
                        indexDifference[i] = problem.getHashIndices().get(p);
                    }

                    // Score = localbdeu(x,P(G) - {x_p}) - localbdeu(x,P(G))
                    score = metric.localScore(indexChild, indexDifference) - metric.localScore(indexChild, indexParentsGraph);
                    operation = Operation.DELETE;
                }

                if(score > bestScore){
                    bestScore = score;
                    bestOperation = operation;
                    bestEdge = edge;
                }
            }

            // Updating graph
            if(bestScore > 0){
                improvement = true;
                // ¿Esto sería así?
                //result+=bestScore;
                if(bestOperation.equals(Operation.ADD)){
                    graph.addEdge(bestEdge);
                }
                else{
                    graph.removeEdge(bestEdge);
                }
            }
        }
    }


    public double search(){
        int iterations = 0;
        double score = 0;
        Graph graph = new EdgeListGraph_n(problem.getVariables());
        for (int i = 0; i <order.size() ; i++) {
            Node child = order.get(i);
            List<Node> parents = new ArrayList<>();
            for(int j=i-1; j >= 0; j--){
                Node parent = order.get(j);
                parents.add(parent);
            }
            evaluate(child, parents, graph);
        }
        //Score the resulting graph
        return GESThread.scoreGraph(graph,problem);

        /* //PREVIOUS VERSION: WRONG
        int iterations = 0;
        while (iterations < MAX_ITERATIONS) {
            EdgeSearch bestEdge = edges.parallelStream()
                    .map(edge -> {
                    // checking if the edge passes all the tests
                    if (passedTests(edge)){
                        SubSet subset = new SubSet();
                        Node parent = Edges.getDirectedEdgeTail(edge);
                        Node child = Edges.getDirectedEdgeHead(edge);
                        double insertEval = GESThread.insertEval(parent, child, subset, graph, problem);
                        return new EdgeSearch(insertEval, new SubSet(), edge);
                    }
                    return null;
                     } )
                    .filter(Objects::nonNull)
                    .max(EdgeSearch::compareTo).orElse(null);

            // Checking if there has been a convergence
            if(bestEdge == null )
                break;
            if(score + bestEdge.getScore() < score)
                break;
            // Adding best edge
            graph.addEdge(bestEdge.getEdge());
            score += bestEdge.getScore();
            edges.remove(bestEdge.getEdge());
            iterations++;
        }
        */
    }


    public double getScore() {
        return score;
    }

    public Graph getGraph() {
        return graph;
    }
}
