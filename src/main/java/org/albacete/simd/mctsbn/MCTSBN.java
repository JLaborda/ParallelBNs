package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MCTSBN {

    /**
     * Time limit in miliseconds
     */
    private int timeLimit;
    /**
     * Iteration limit of the search
     */
    private final int ITERATION_LIMIT;

    /**
     * Exploration constant c for the UCT equation: UCT_j = X_j + c * sqrt(ln(N) / n_j)
     */
    private final double explorationConstant = 1.0 / Math.sqrt(2);

    /**
     * Problem of the search
     */
    private final Problem problem;

    private int nThreads = 1;

    private TreeNode root;

    private double bestScore = Double.NEGATIVE_INFINITY;
    private List<Node> bestOrder = new ArrayList<>();
    private Graph bestDag = null;

    private boolean convergence = false;

    private ConcurrentHashMap<String,Double> cache = new ConcurrentHashMap<>();

    public MCTSBN(Problem problem, int nThreads){
        this.problem = problem;
        this.nThreads = nThreads;
        this.ITERATION_LIMIT = 10000;
    }


    public Dag search(State initialState){
        //1. Set Root
        this.root = new TreeNode(initialState, null);

        //2. Search loop
        for (int i = 0; i < ITERATION_LIMIT; i++) {
            System.out.println("Iteration " + i + "...");
            executeRound();
            System.out.println("Tree Structure:");
            System.out.println(this);
            if(convergence){
                System.out.println("Convergence has been found. Ending search");
                break;
            }
        }
        // return Best Dag
        return new Dag(bestDag);
    }

    public Dag search(){
        State initialState = new State(new GraphNode("root"), new ArrayList<>(), problem);
        return this.search(initialState);
    }

    /**
     * Executes one round of the selection, expansion, rollout and backpropagation iterations.
     */
    private void executeRound(){
        //1. Selection and Expansions
        TreeNode selectedNode = selectNode(this.root);
        if(selectedNode == null) {
            convergence = true;
            return;
        }
        //2. Rollout
        double reward = rollout(selectedNode.getState());
        //3. Backpropagation
        backPropagate(selectedNode, reward);
    }

    /**
     * Selects the best node. The best node is an expansion or the best child if the node is terminal
     * @param node
     * @return
     */
    private TreeNode selectNode(TreeNode node){
        while(!node.isTerminal()){
            if(node.isFullyExpanded())
                node = getBestChild(node, explorationConstant);
            else{
                return expand(node);
            }
        }
        return null;
    }

    public TreeNode expand(TreeNode node){
        //1. Get all possible actions
        List<Node> actions = node.getState().getPossibleActions();
        //2. Get actions already taken for this node
        Set<Node> childrenActions = node.getChildrenAction();
        for (Node action: actions) {
            //3. Check if the actions has already been taken
            if (!childrenActions.contains(action)){
                // 4. Expand the tree by creating a new node and connecting it to the tree.
                TreeNode newNode = new TreeNode(node.getState().takeAction(action), node);
                node.addChild(newNode);
                // 5. Check if there are more actions to be expanded in this node, and if not, change the isFullyExpanded value
                if(node.getChildrenAction().size() == actions.size())
                    node.setFullyExpanded(true);
                // 6. Returning the expanded node
                return newNode;
            }
        }
        throw new IllegalStateException("No node to expand exception on Node: " + node);
    }

    /**
     * Random policy rollout. Given a state with a partial order, we generate a random order that starts off with the initial order
     * @param state State that provides the partial order for a final randomly generated order.
     * @return
     */
    public double rollout(State state){
        // Generating candidates and shuffling
        List<Node> order = state.getOrder();
        List<Node> candidates = problem.getVariables();
        candidates = candidates.stream().filter(node -> !order.contains(node)).collect(Collectors.toList());
        Collections.shuffle(candidates);

        // Creating order for HC
        List<Node> finalOrder = new ArrayList<>(order);
        finalOrder.addAll(candidates);

        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, finalOrder, cache);
        double score = hc.search();

        // Updating best score, order and graph
        if(score > bestScore){
            bestScore = score;
            bestOrder = finalOrder;
            bestDag = hc.getGraph();
        }
        return score;
    }

    public void backPropagate(TreeNode node, double reward){
        TreeNode currentNode = node;
        while (currentNode != null){
            currentNode.incrementOneVisit();
            currentNode.addReward(reward);
            currentNode = currentNode.getParent();
        }
    }

    /**
     *
     * @param node
     * @param explorationValue
     * @return
     */
    public TreeNode getBestChild(TreeNode node, double explorationValue){
        // Initial configuration
        double bestValue = Double.NEGATIVE_INFINITY;
        List<TreeNode> bestNodes = new ArrayList<>();

        for (TreeNode child: node.getChildren()
        ) {
            // Evaluating the child of the node
            double nodeValue = child.getTotalReward() / child.getNumVisits() +
                    explorationValue * Math.sqrt(Math.log(node.getNumVisits()) / child.getNumVisits());
            // Checking if the child is a better selection
            if(nodeValue > bestValue){
                bestValue = nodeValue;
                bestNodes.clear();
                bestNodes.add(child);
            } else if (nodeValue == bestValue) {
                bestNodes.add(child);
            }
        }
        // Select a random TreeNode of the bestNodes list
        int index = (int)(Math.random() * bestNodes.size());

        return bestNodes.get(index);
    }

    public List<Node> getBestOrder(){
        return bestOrder;
    }


    @Override
    public String toString() {
        return root.toString();
    }
}
