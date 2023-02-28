package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.List;

public class State {

    private Node node;

    private List<Node> order;

    private Problem problem;

    public State(Node node, List<Node> order, Problem problem){
        this.node = node;
        this.order = order;
        this.problem = problem;
    }

    public List<Node> getPossibleActions(){
        List<Node> allVars = problem.getVariables();
        List<Node> possibleActions = new ArrayList<>();
        for(Node var : allVars){
            if(!order.contains(var)){
                possibleActions.add(var);
            }
        }
        return possibleActions;
    }

    public State takeAction(Node action){
        List<Node> newOrder = new ArrayList<>(order);
        newOrder.add(action);
        return new State(action, newOrder, problem);
    }

    public boolean isTerminal(){
        return order.size() == problem.getVariables().size();
    }

    public Node getNode(){
        return this.node;
    }

    public List<Node> getOrder() {
        return order;
    }

    @Override
    public String toString() {
     return "State with node: " + node + "----- and order: " + order;
    }
}
