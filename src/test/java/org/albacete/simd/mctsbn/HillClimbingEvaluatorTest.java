package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.BDeuScore;
import org.albacete.simd.utils.Problem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HillClimbingEvaluatorTest {

    @Test
    public void scoreTest(){
        String networkFolder = "./res/networks/";
        String net_name = "alarm";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";

        int seed = 11231231;
        Problem problem = new Problem(bbdd_path);
        List<Node> order = randomOrder(problem, seed);


        int child = problem.getHashIndices().get(order.get(5));
        int candidate = problem.getHashIndices().get(order.get(4));


        Set<Integer> parents = new HashSet<>();
        Integer[] parentsArr = new Integer[parents.size()];
        parents.toArray(parentsArr);
        Arrays.sort(parentsArr);


        ConcurrentHashMap<String,Double> cache = new ConcurrentHashMap<>();
        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, order, cache);

        // OPERATION ADD
        double scoreAdd = hc.getAdditionScore(child, candidate, new HashSet<>(parents), parentsArr);

        // Update Graph
        parents.add(candidate);
        parentsArr = new Integer[parents.size()];
        parents.toArray(parentsArr);
        Arrays.sort(parentsArr);

        // OPERATION DELETE
        double scoreDelete = hc.getDeleteScore(child, candidate, new HashSet<>(parents), parentsArr);

        Assert.assertEquals(scoreAdd, -scoreDelete, 0.0000001);
    }


    @Test
    public void bestOrderTest(){ String networkFolder = "./res/networks/";
        String net_name = "earthquake";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        Problem problem = new Problem(bbdd_path);

        List<Node> badOrder = new ArrayList<>();
        badOrder.add(problem.getNode("JohnCalls"));
        badOrder.add(problem.getNode("MaryCalls"));
        badOrder.add(problem.getNode("Alarm"));
        badOrder.add(problem.getNode("Earthquake"));
        badOrder.add(problem.getNode("Burglary"));

        List<Node> optimalOrder = new ArrayList<>();
        optimalOrder.add(problem.getNode("Burglary"));
        optimalOrder.add(problem.getNode("Earthquake"));
        optimalOrder.add(problem.getNode("Alarm"));
        optimalOrder.add(problem.getNode("MaryCalls"));
        optimalOrder.add(problem.getNode("JohnCalls"));


        ConcurrentHashMap<String,Double> cache1 = new ConcurrentHashMap<>();
        ConcurrentHashMap<String,Double> cache2 = new ConcurrentHashMap<>();
        HillClimbingEvaluator hcBadOrder = new HillClimbingEvaluator(problem, badOrder, cache1);
        HillClimbingEvaluator hcOptimalOrder = new HillClimbingEvaluator(problem, optimalOrder, cache2);

        double badScore = hcBadOrder.search();
        double optimalScore = hcOptimalOrder.search();

        System.out.println("Bad Score: " + badScore);
        System.out.println("Optimal Score: " + optimalScore);

        Assert.assertTrue(optimalScore > badScore);

    }

    public static List<Node> randomOrder(Problem problem, int seed){
        List<Node> randomOrder = new ArrayList<>(problem.getVariables());
        Random random = new Random(seed);
        Collections.shuffle(randomOrder, random);
        return randomOrder;
    }

}
