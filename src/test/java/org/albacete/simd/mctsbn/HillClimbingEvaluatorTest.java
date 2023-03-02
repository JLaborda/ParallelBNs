package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.BDeuScore;
import org.albacete.simd.utils.Problem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HillClimbingEvaluatorTest {

    @Test
    public void scoreTest(){
        String networkFolder = "./res/networks/";
        String net_name = "alarm";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";

        int seed = 11231231;
        Problem problem = new Problem(bbdd_path);
        List<Node> order = randomOrder(problem, seed);

        Node child = order.get(5);
        Node parent = order.get(4);
        Graph graph = new EdgeListGraph_n(problem.getVariables());
        Edge edge = Edges.directedEdge(parent, child);

        // Getting indexes of the parent and the parents inside the graph
        int indexChild = problem.getHashIndices().get(child);
        int indexParent = problem.getHashIndices().get(parent);
        List<Node> parentsGraph = graph.getParents(child);
        int[] indexParentsGraph = new int[parentsGraph.size()];
        for (int i = 0; i < parentsGraph.size(); i++) {
            indexParentsGraph[i] = problem.getHashIndices().get(parentsGraph.get(i));
        }

        // OPERATION ADD
        double scoreAdd = getAdditionScore(indexChild, indexParent, parentsGraph, indexParentsGraph, problem);

        // Update Graph
        graph.addEdge(edge);
        parentsGraph = graph.getParents(child);
        indexParentsGraph = new int[parentsGraph.size()];
        for (int i = 0; i < parentsGraph.size(); i++) {
            indexParentsGraph[i] = problem.getHashIndices().get(parentsGraph.get(i));
        }

        // OPERATION DELETE
        double scoreDelete = getDeleteScore(indexChild, parent, parentsGraph, indexParentsGraph, problem);

        Assert.assertEquals(scoreAdd, -scoreDelete, 0.0000001);


    }

    public static List<Node> randomOrder(Problem problem, int seed){
        List<Node> randomOrder = new ArrayList<>(problem.getVariables());
        Random random = new Random(seed);
        Collections.shuffle(randomOrder, random);
        return randomOrder;
    }

    private double getDeleteScore(int indexChild, Node parent, List<Node> parentsGraph, int[] indexParentsGraph, Problem problem) {
        BDeuScore metric = new BDeuScore(problem.getData());
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
        score = metric.localScore(indexChild, indexDifference) - metric.localScore(indexChild, indexParentsGraph);
        return score;
    }

    private double getAdditionScore(int indexChild, int indexParent, List<Node> parentsGraph, int[] indexParentsGraph, Problem problem) {
        BDeuScore metric = new BDeuScore(problem.getData());
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

        score = metric.localScore(indexChild, indexUnion) - metric.localScore(indexChild, indexParentsGraph);
        return score;
    }
}
