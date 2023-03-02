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

    private final Problem problem;

    private final List<Node> order;


    private Graph graph;

    private double score = 0;

    private final static int MAX_ITERATIONS = 1000;

    private BDeuScore metric;

    private enum Operation {ADD, DELETE}

    private double scorePart1;
    private double scorePart2;

    private double s1;
    private double s2;


    public HillClimbingEvaluator(Problem problem, List<Node> order){
        this.problem = problem;
        this.order = order;
        this.graph = new EdgeListGraph_n();
        metric = new BDeuScore(problem.getData());
    }



    private void evaluate(Node child, List<Node> parents, Graph graph){
        boolean improvement = true;
        int indexChild = problem.getHashIndices().get(child);
        int iteration = 0;

        System.out.println("Starting evaluation of " + child + " with parents: " + parents);

        while(improvement && iteration < MAX_ITERATIONS) {
            //System.out.println("Iteration " + iteration);
            improvement = false;
            double bestScore = Double.NEGATIVE_INFINITY;
            Operation bestOperation = null;
            Edge bestEdge = null;

            for (Node parent : parents) {
                //System.out.println("Checking " + child + " -> " + parent + " ...");
                // Defining score and operation variables
                double score = 0;
                Operation operation = null;

                // Creating edge from parent to child: parent -> child
                Edge edge = Edges.directedEdge(parent, child);

                // Getting indexes of the parent and the parents inside the graph
                int indexParent = problem.getHashIndices().get(parent);
                List<Node> parentsGraph = graph.getParents(child);
                int[] indexParentsGraph = new int[parentsGraph.size()];
                for (int i = 0; i < parentsGraph.size(); i++) {
                    indexParentsGraph[i] = problem.getHashIndices().get(parentsGraph.get(i));
                }

                // OPERATION ADD
                if (!graph.containsEdge(edge)) {
                    score = getAdditionScore(indexChild, indexParent, parentsGraph, indexParentsGraph);
                    operation = Operation.ADD;
                }
                // OPERATION DELETE
                else{
                    score = getDeleteScore(indexChild, parent, parentsGraph, indexParentsGraph);
                    operation = Operation.DELETE;
                }

                //System.out.println("Score of edge " + edge + " (" + operation + ")"+ " is: " + score);
                if(score > bestScore && score > 0){
                    //System.out.println("Prev Best Score: " + bestScore);
                    //System.out.println("Changing best score, bestOperation and bestEdge: ");
                    //System.out.println("New Best Score: " + score);
                    //System.out.println("New Best Operation: " + operation);
                    //System.out.println("New Best Edge: " + edge);
                    bestScore = score;
                    bestOperation = operation;
                    bestEdge = edge;
                    s1 = scorePart1;
                    s2 = scorePart2;
                }
            }

            System.out.println("Iteration: " + iteration + " - Best Edge found: " + bestEdge);
            System.out.println("Iteration: " + iteration + " - Best Score found: " + bestScore);
            System.out.println("Iteration: " + iteration + " - Best Operation found: " + bestOperation);
            System.out.println("Iteration: " + iteration + " - ScorePart1: " + s1);
            System.out.println("Iteration: " + iteration + " - ScorePart2: " + s2);

            // Updating graph
            if(bestScore > 0){
                System.out.println("Improvement is true");
                improvement = true;
                // ¿Esto sería así? El resultado debería ser la suma de todos los enlaces y operaciones realizadas?
                //result+=bestScore;
                if(bestOperation.equals(Operation.ADD)){
                    graph.addEdge(bestEdge);
                }
                else{
                    graph.removeEdge(bestEdge);
                }
            }
            iteration++;
        }
        System.out.println("END OF EVALUATION");
    }

    private double getDeleteScore(int indexChild, Node parent, List<Node> parentsGraph, int[] indexParentsGraph) {
        double score;
        // Calculating indexes for the difference set of parents
        List<Node> parentsAux = new ArrayList<>(parentsGraph);
        parentsAux.remove(parent);
        int[] indexDifference = new int[parentsGraph.size() -1];
        for (int i = 0; i < indexDifference.length; i++) {
            Node p = parentsAux.get(i);
            indexDifference[i] = problem.getHashIndices().get(p);
        }



        // Score = localbdeu(x,P(G) - {x_p}) - localbdeu(x,P(G))
        scorePart1 = metric.localScore(indexChild, indexDifference);
        scorePart2 = metric.localScore(indexChild, indexParentsGraph);
        score = metric.localScore(indexChild, indexDifference) - metric.localScore(indexChild, indexParentsGraph);
        return score;
    }

    private double getAdditionScore(int indexChild, int indexParent, List<Node> parentsGraph, int[] indexParentsGraph) {
        double score;
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
        //System.out.println("Add  Con padre: " + metric.localScore(indexChild, indexUnion));
        //System.out.println("Add  Sin padre: " + metric.localScore(indexChild, indexParentsGraph));

        scorePart1 = metric.localScore(indexChild, indexUnion);
        scorePart2 = metric.localScore(indexChild, indexParentsGraph);
        score = metric.localScore(indexChild, indexUnion) - metric.localScore(indexChild, indexParentsGraph);
        return score;
    }


    public double search(){
        graph = new EdgeListGraph_n(problem.getVariables());
        order.parallelStream().forEach(child -> {
            int i = order.indexOf(child);
            List<Node> parents = new ArrayList<>();
            for(int j=i-1; j >= 0; j--){
                Node parent = order.get(j);
                parents.add(parent);
            }
            System.out.println("---------------------");
            System.out.println("Subset " + i + ": Starting evaluation of child - " + child + " and parents: " + parents + " with graph " + graph);
            evaluate(child, parents, graph);
        });
/*        for (int i = 0; i <order.size() ; i++) {
            Node child = order.get(i);
            List<Node> parents = new ArrayList<>();
            for(int j=i-1; j >= 0; j--){
                Node parent = order.get(j);
                parents.add(parent);
            }
            System.out.println("---------------------");
            System.out.println("Subset " + i + ": Starting evaluation of child - " + child + " and parents: " + parents + " with graph " + graph);
            evaluate(child, parents, graph);
        }
*/
        //Score the resulting graph
        score = GESThread.scoreGraph(graph,problem);
        return score;

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
