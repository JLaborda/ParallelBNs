package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

public class MCTSBN {

    /**
     * Time limit in miliseconds
     */
    private int TIME_LIMIT;
    /**
     * Iteration limit of the search
     */
    private final int ITERATION_LIMIT;

    /**
     * Exploration constant c for the UCT equation: UCT_j = X_j + c * sqrt(ln(N) / n_j)
     */
    public static final double EXPLORATION_CONSTANT = 4 * Math.sqrt(2); //1.0 / Math.sqrt(2);

    private static final int NUM_ROLLOUTS = 1;

    private static final int NUM_SELECTION = 1;

    private static final int NUM_EXPAND = 1;

    /**
     * Problem of the search
     */
    private final Problem problem;

    private int nThreads = 1;

    private TreeNode root;

    private double bestScore = Double.NEGATIVE_INFINITY;
    private List<Node> bestOrder = new ArrayList<>();
    private List<Node> bestPartialOrder = new ArrayList<>();
    private Graph bestDag = null;

    private boolean convergence = false;

    private ConcurrentHashMap<String,Double> cache = new ConcurrentHashMap<>();

    private PriorityBlockingQueue<TreeNode> selectionQueue = new PriorityBlockingQueue<>();

    private String saveFilePath = "prueba-mctsbn.csv";

    public MCTSBN(Problem problem, int nThreads, int iterationLimit){
        this.problem = problem;
        this.nThreads = nThreads;
        this.ITERATION_LIMIT = iterationLimit;
    }


    public Dag search(State initialState){
        //1. Set Root
        this.root = new TreeNode(initialState, null);
        selectionQueue.add(root);
        //2. Search loop
        for (int i = 0; i < ITERATION_LIMIT; i++) {
            System.out.println("Iteration " + i + "...");

            // Executing round
            long startTime = System.currentTimeMillis();
            executeRound();
            long endTime = System.currentTimeMillis();
            // Calculating time of the iteration
            double totalTimeRound = 1.0 * (endTime - startTime) / 1000;

            // Showing tree structure
            //System.out.println("Tree Structure:");
            //System.out.println(this);

            saveRound(i, totalTimeRound);
            if(convergence){
                System.out.println("Convergence has been found. Ending search");
                break;
            }
        }
        //System.out.println("Finished...");
        //System.out.println("Tree Structure: ");
        //System.out.println(this);
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
        //TreeNode selectedNode = selectNode(root);
        List<TreeNode> selectedNodes = selectNode();
        if(selectedNodes.size() == 0) {
            convergence = true;
            return;
        }
        // 2. Expand selected node
        //TreeNode expandedNode = expand(selectedNode);
        List<TreeNode> expandedNodes = expand(selectedNodes);

        //3. Rollout and Backpropagation
        expandedNodes.parallelStream().forEach(expandedNode -> {
            double reward = rollout(expandedNode.getState());
            backPropagate(expandedNode, reward);
        });
        /*for (TreeNode expandedNode: expandedNodes) {
            double reward = rollout(expandedNode.getState());
            //4. Backpropagation
            backPropagate(expandedNode, reward);
        }*/
    }

    private void saveRound(int iteration, double totalTimeRound) {

        File file = new File(saveFilePath);
        BufferedWriter csvWriter = null;
        try {
            csvWriter = new BufferedWriter(new FileWriter(saveFilePath, true));

            //FileWriter csvWriter = new FileWriter(savePath, true);
            if (file.length() == 0) {
                String header = "iterations,time(s),score\n";
                csvWriter.append(header);
            }
            String result = "" + iteration + "," + totalTimeRound + "," + bestScore + "\n";
            //System.out.println("Results iteration:" + iteration);
            System.out.println("Total time iteration: " + totalTimeRound);
            System.out.println("Best Score: " + bestScore);
            System.out.println("Best order: " + toStringOrder(bestOrder));
            System.out.println("Best partial order: " + toStringOrder(bestPartialOrder));
            System.out.println("------------------------------------------------------");
            csvWriter.append(result);

            csvWriter.flush();
            csvWriter.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("Results of iteration saved at: " + saveFilePath);

    }

    /**
     * Selects the best nodes to expand. The nodes are selected with the UCT equation.
     * @return List of selected nodes to be expanded
     */
    private List<TreeNode> selectNode(){//(TreeNode node){
        /*
        while(!node.isTerminal()){
            if(node.isFullyExpanded())
                node = getBestChild(node, EXPLORATION_CONSTANT);
            else{
                return node;
            }
        }

        return null;
*/
        // Creating the arraylist of the selected nodes.
        List<TreeNode> selection = new ArrayList<>();
        for (int i = 0; i < NUM_SELECTION; i++) {

            // Getting a peek of the best node
            TreeNode selectNode = selectionQueue.peek();
            if(selectNode == null)
                break;

            // Checking if the parent of the best node has already been expanded at least once
            if (selectNode.getParent() == null || selectNode.getParent().isExpanded())
                selection.add(selectionQueue.poll());
            else {
                // Parent has not been fully expanded, adding it for expansion
                selectionQueue.remove(selectNode.getParent());
                selection.add(selectNode.getParent());
            }
        }

        return  selection;
    }

    /**
     * Expands the nodes in the list of selected nodes. The nodes are expanded by creating n child for each selected node.
     * @param selection List of selected nodes
     * @return List of expanded nodes.
     */
    public List<TreeNode> expand(List<TreeNode> selection){

        List<TreeNode> expansion = new ArrayList<>();

        for (TreeNode node : selection) {

            int nExpansion = 0;

            //1. Get all possible actions
            List<Node> actions = node.getState().getPossibleActions();
            //2. Get actions already taken for this node
            Set<Node> childrenActions = node.getChildrenAction();
            for (Node action: actions) {

                // Checking if the number of expansion for this node is greater than the limit
                if(nExpansion >= NUM_EXPAND)
                    break;

                //3. Check if the actions has already been taken
                if (!childrenActions.contains(action)){
                    // 4. Expand the tree by creating a new node and connecting it to the tree.
                    TreeNode newNode = new TreeNode(node.getState().takeAction(action), node);
                    node.addChild(newNode);
                    node.setExpanded(true);
                    // 5. Check if there are more actions to be expanded in this node, and if not, change the isFullyExpanded value
                    if(node.getChildrenAction().size() == actions.size())
                        node.setFullyExpanded(true);

                    // 7. Adding the expanded node to the list and queue
                    expansion.add(newNode);
                    //selectionQueue.add(newNode);
                    nExpansion++;

                    //return newNode;
                }
            }
        }
        return expansion;
    }

    /**
     * Random policy rollout. Given a state with a partial order, we generate a random order that starts off with the initial order
     * @param state State that provides the partial order for a final randomly generated order.
     * @return
     */
    public double rollout(State state){
        // Generating candidates and shuffling
        double scoreSum = 0;

        for (int i = 0; i < NUM_ROLLOUTS; i++) {
            // Creating a total order with the partial order of the state.
            List<Node> order = state.getOrder();
            List<Node> candidates = problem.getVariables();
            candidates = candidates.stream().filter(node -> !order.contains(node)).collect(Collectors.toList());
            Collections.shuffle(candidates);

            // Creating order for HC
            List<Node> finalOrder = new ArrayList<>(order);
            finalOrder.addAll(candidates);

            HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, finalOrder, cache);
            double score = hc.search();
            scoreSum+= score;
            if(score > bestScore){
                bestScore = score;
                bestOrder = finalOrder;
                bestPartialOrder = order;
                bestDag = hc.getGraph();
            }
        }
        scoreSum = scoreSum / NUM_ROLLOUTS;

        // Updating best score, order and graph

        return scoreSum;
    }

    /**
     * Backpropagates the reward and visits of the nodes where a rollout has been done.
     * @param node Node that
     * @param reward
     */
    public void backPropagate(TreeNode node, double reward){
        // REVISAR!!!!
        TreeNode currentNode = node;
        while (currentNode != null){
            // Add one visit and total reward to the currentNode
            currentNode.incrementOneVisit();
            currentNode.addReward(reward);
            selectionQueue.remove(currentNode);
            if((!currentNode.isFullyExpanded()) && !(currentNode.isTerminal())) {
                selectionQueue.add(currentNode);
            }
            // Update currentNode to its parent.
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

    public double getBestScore(){
        return bestScore;
    }

    public Graph getBestDag() {
        return bestDag;
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public static String toStringOrder(List<Node> order){
        StringBuilder result = new StringBuilder();
        for (Node o: order) {
            result.append(o).append(" < ");
        }
        return result.toString();
    }

}
