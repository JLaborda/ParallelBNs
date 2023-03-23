package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class State {

    private final Integer node;

    private final List<Integer> order;
    
    private final List<Integer> allVars;

    private final Problem problem;

    public State(Integer node, List<Integer> order, List<Integer> allVars, Problem problem){
        this.node = node;
        this.order = order;
        this.allVars = allVars;
        this.problem = problem;
    }

    public List<Integer> getPossibleActions(){
        List<Integer> possibleActions = new ArrayList<>();
        for(Integer var : allVars){
            if(!order.contains(var)){
                possibleActions.add(var);
            }
        }
        return possibleActions;
    }

    public State takeAction(Integer action){
        List<Integer> newOrder = new ArrayList<>(order);
        newOrder.add(0, action);
        return new State(action, newOrder, allVars, problem);
    }

    public boolean isTerminal(){
        return order.size() == problem.getVariables().size();
    }

    public Integer getNode(){
        return this.node;
    }

    public List<Integer> getOrder() {
        return order;
    }

    @Override
    public String toString() {
     return "State with node: " + node + "----- and order: " + order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(node, state.node) && Objects.equals(order, state.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, order);
    }
}
