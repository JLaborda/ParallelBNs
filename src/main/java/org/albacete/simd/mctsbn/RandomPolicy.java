package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RandomPolicy {

    Problem problem;

    public RandomPolicy(Problem problem){
        this.problem = problem;
    }

    public double rollout(State state){
        // Generating candidates and shuffling
        List<Node> order = state.getOrder();
        List<Node> candidates = problem.getVariables();
        candidates = candidates.stream().filter(node -> {
           return !order.contains(node);
        }).collect(Collectors.toList());
        Collections.shuffle(candidates);

        // Creating order for HC
        List<Node> finalOrder = new ArrayList<>(order);
        finalOrder.addAll(candidates);

        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, finalOrder);
        hc.search();


        return hc.getScore();
    }
}
