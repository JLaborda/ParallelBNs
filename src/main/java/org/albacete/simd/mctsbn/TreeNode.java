package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Node;

import java.util.HashSet;
import java.util.Iterator;
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


    public String toString() {
        StringBuilder buffer = new StringBuilder(50);
        print(buffer, "", "");
        return buffer.toString();
    }

    private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
        buffer.append(prefix);
        buffer.append(state.getNode().getName());
        buffer.append('\n');
        for (Iterator<TreeNode> it = children.iterator(); it.hasNext();) {
            TreeNode next = it.next();
            if (it.hasNext()) {
                next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
            } else {
                next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
    }
}
