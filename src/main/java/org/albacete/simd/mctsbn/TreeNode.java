package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Node;

import java.util.HashSet;
import java.util.Set;

public class TreeNode {

    private final State state;
    private final TreeNode parent;
    private int numVisits = 0;
    private double totalReward = 0;
    private Set<TreeNode> children = new HashSet<>();

    private boolean fullyExpanded;

    public TreeNode(State state, TreeNode parent){
        this.state = state;
        this.parent = parent;
        this.numVisits = 0;
        this.totalReward = 0;
        this.fullyExpanded = state.isTerminal();
    }

    public boolean isTerminal() {
        return state.isTerminal();
    }

    public State getState() {
        return state;
    }

    public TreeNode getParent() {
        return parent;
    }

    public int getNumVisits() {
        return numVisits;
    }

    public double getTotalReward() {
        return totalReward;
    }

    public Set<TreeNode> getChildren() {
        return children;
    }

    public Set<Node> getChildrenAction(){
        Set<Node> actions = new HashSet<>();
        for (TreeNode child: children) {
            actions.add(child.state.getNode());
        }
        return actions;
    }

    public void setChildren(Set<TreeNode> children) {
        this.children = children;
    }

    public void addChild(TreeNode child){
        this.children.add(child);
    }

    public boolean isFullyExpanded() {
        return fullyExpanded;
    }

    public void setFullyExpanded(boolean fullyExpanded) {
        this.fullyExpanded = fullyExpanded;
    }

    public void setNumVisits(int numVisits) {
        this.numVisits = numVisits;
    }

    public void setTotalReward(double totalReward) {
        this.totalReward = totalReward;
    }

    public void addReward(double reward){
        totalReward += reward;
    }

    public void incrementOneVisit(){
        this.numVisits++;
    }


    @Override
    public String toString() {
        return "TreeNode:\n State: \n\t" + state +
                "Parent State: \n\t" + parent.state +
                "Number of children: " + this.children.size();

    }
}
