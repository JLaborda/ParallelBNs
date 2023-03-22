package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class TreeNode implements Comparable<TreeNode>{

    private final State state;
    private final TreeNode parent;
    private Set<TreeNode> children = new HashSet<>();
    
    private int numVisits = 0;
    private double totalReward = 0;
    private double UCTSCore = 0;
    
    private boolean fullyExpanded;
    private boolean isExpanded = false;

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
    
    public double getUCTSCore() {
        return UCTSCore;
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

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(50);
        print(buffer, "", "");
        return buffer.toString();
    }

    private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
        buffer.append(prefix);
        buffer.append(state.getNode().getName());
        
        String results;
        if(this.parent == null){
            double exploitationScore = ((this.getTotalReward() / this.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances;
            results = "  \t" + this.getNumVisits() + "   " + Double.MAX_VALUE + ", " + exploitationScore;
        } else {
            double exploitationScore = ((this.getTotalReward() / this.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances;
            double explorationScore = MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(this.parent.getNumVisits()) / this.getNumVisits());
            results = "  \t" + this.getNumVisits() + "   " + (exploitationScore - explorationScore) + ", " + exploitationScore + ", " + explorationScore;
        }
        buffer.append(results);
        
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
    
    public void updateUCT() {
        if(this.parent == null){
            UCTSCore = Double.MAX_VALUE;
        }
        else{
            double exploitationScore = ((this.getTotalReward() / this.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances;
            double explorationScore = MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(this.parent.getNumVisits()) / this.getNumVisits());

            UCTSCore = exploitationScore - explorationScore;
        }
    }

    @Override
    public int compareTo(@NotNull TreeNode o) {
        return Double.compare(this.UCTSCore, o.UCTSCore);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(!(obj instanceof TreeNode))
            return false;
        TreeNode other = (TreeNode) obj;

        return this.state.equals(other.state);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.state);
        return hash;
    }

}
