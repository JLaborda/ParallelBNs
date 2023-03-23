package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TreeNode implements Comparable<TreeNode>{

    private final State state;
    private final TreeNode parent;
    private int numVisits = 0;
    private double totalReward = 0;
    private Set<TreeNode> children = new HashSet<>();
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

    @Override
    public int compareTo(@NotNull TreeNode o) {
        // child.getTotalReward() / child.getNumVisits() +
        //                    explorationValue * Math.sqrt(Math.log(node.getNumVisits()) / child.getNumVisits());

        // ESTO ESTÁ MAL!
        // El reward debe ser positivo y acotado, porque sino es una búsqueda aleatorio
        // Restarle por el score de la red vacía (un delta) y dividirlo por el número de instancias para acotar el score.
        // Hay que pensar en la ecuación UCT para evitar una búsqueda de anchura.+

        double thisScore, thatScore;

        if(this.parent == null && o.parent == null){
            // Ambos son la raíz, esto no debería pasar...
            throw new IllegalStateException("Root is in the selection queue twice");
        }


        // Checking if this TreeNode is the root
        if(this.parent == null){
            thisScore = Double.MAX_VALUE;
        }
        else{
            double exploitationScore = ((this.getTotalReward() / this.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances;
            //System.out.println("Exploitation Score for node  [" + this.state.getOrder() + "] is: " + exploitationScore);
            //System.out.println("******");

            double explorationScore = MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(this.parent.getNumVisits()) / this.getNumVisits());
            //System.out.println("Exploration Score for node  [" + this.state.getOrder() + "] is: " + explorationScore);
            //System.out.println("******");

            thisScore = ((this.getTotalReward() / this.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances +
                    MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(this.parent.getNumVisits()) / this.getNumVisits());

            //System.out.println("This Score: " + thisScore);
        }

        // Checking if o is the root
        if(o.parent == null){
            thatScore = Double.MAX_VALUE;
        }
        else{
            double exploitationScore = ((o.getTotalReward() / o.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances;
            //System.out.println("Exploitation Score for node  [" + o.state.getOrder() + "] is: " + exploitationScore);
            //System.out.println("******");

            double explorationScore = MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(o.parent.getNumVisits()) / o.getNumVisits());
            //System.out.println("Exploration Score for node  [" + o.state.getOrder() + "] is: " + explorationScore);
            //System.out.println("******");

            thatScore = ((o.getTotalReward() / o.getNumVisits()) - Problem.emptyGraphScore ) / Problem.nInstances +
                    MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(o.parent.getNumVisits()) / o.getNumVisits());

            //System.out.println("That Score: " + thatScore);
        }

        // El valor de las visitas del padre de la raiz cuál es?


        return Double.compare(thisScore, thatScore);

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

}
