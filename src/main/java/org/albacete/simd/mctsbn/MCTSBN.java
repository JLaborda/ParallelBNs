package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.mctsbn.HillClimbingEvaluator.Pair;
import org.albacete.simd.threads.GESThread;

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
    public static final double EXPLORATION_CONSTANT = 1 * Math.sqrt(2); //1.0 / Math.sqrt(2);

    private static final int NUM_ROLLOUTS = 1;

    private static final int NUM_SELECTION = 1;

    private static final int NUM_EXPAND = 1;

    /**
     * Problem of the search
     */
    private final Problem problem;

    private TreeNode root;

    private double bestScore = Double.NEGATIVE_INFINITY;
    private List<Node> bestOrder = new ArrayList<>();
    private List<Node> bestPartialOrder = new ArrayList<>();
    private Graph bestDag = null;

    private boolean convergence = false;

    private final ConcurrentHashMap<String,Double> cache;
    
    private final Pair[] bestBDeuForNode;

    private HashSet<TreeNode> selectionSet = new HashSet<>();

    private final String saveFilePath = "prueba-mctsbn.csv";

    public MCTSBN(Problem problem, int iterationLimit){
        this.problem = problem;
        this.cache = problem.getLocalScoreCache();
        this.ITERATION_LIMIT = iterationLimit;
        bestBDeuForNode = new Pair[problem.getVariables().size()];
    }


    public Dag search(State initialState){
        //1. Set Root
        this.root = new TreeNode(initialState, null);
        selectionSet.add(root);
        
        //1.5 Add PGES order
        for (int i = 4; i < 4; i++) {
            initializeWithPGES(root,i);
        }

        System.out.println("\n\nSTARTING MCTSBN\n------------------------------------------------------");
        
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
        
        //System.out.println(queueToString());
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
    
    private void initializeWithPGES(TreeNode root, int nThreads) {        
        // Execute PGES to obtain a good order
        double init = System.currentTimeMillis();
        Clustering hierarchicalClustering = new HierarchicalClustering();
        BNBuilder algorithm = new PGESwithStages(problem, hierarchicalClustering, nThreads, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        algorithm.search();
        List<Node> orderPGES = algorithm.getCurrentDag().getCausalOrdering();
        
        System.out.println("\n\nFINISHED PGES (" + ((System.currentTimeMillis() - init)/1000.0) + " s). BDeu: " + GESThread.scoreGraph(algorithm.getCurrentDag(), problem));
        
        System.out.println("\nCACHE: " + this.cache.size());
        
        // Evaluate the order with HC
        init = System.currentTimeMillis();
        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, cache);
        hc.setOrder(orderPGES);
        hc.search();
        
        if (hc.getScore() > this.bestScore) {
            this.bestDag = hc.getGraph();
            this.bestScore = hc.getScore();
            this.bestOrder = orderPGES;
        }
        
        System.out.println("\nCACHE: " + this.cache.size());

        System.out.println("\n\nFINISHED HC (" + ((System.currentTimeMillis() - init)/1000.0) + " s). BDeu: " + bestScore);

        // Expand the tree to the generated node
        root.incrementOneVisit();
        root.addReward(bestScore);
        for (Node node : orderPGES) {
            TreeNode newNode = new TreeNode(root.getState().takeAction(node), root);
            root.addChild(newNode);
            newNode.incrementOneVisit();
            newNode.addReward(bestScore);
            newNode.setExpanded(true);
            this.selectionSet.add(newNode);
            root = newNode;
        }
        root.setFullyExpanded(true);
        this.selectionSet.remove(root);
    }

    /**
     * Executes one round of the selection, expansion, rollout and backpropagation iterations.
     */
    private void executeRound(){
        //1. Selection and Expansions
        //TreeNode selectedNode = selectNode(root);
        List<TreeNode> selectedNodes = selectNode();
        if(selectedNodes.isEmpty()) {
            convergence = true;
            return;
        }
        // 2. Expand selected node
        //TreeNode expandedNode = expand(selectedNode);
        List<TreeNode> expandedNodes = expand(selectedNodes);

        //3. Rollout and Backpropagation
        expandedNodes.parallelStream().forEach(expandedNode -> {
            double reward = inverseRollout(expandedNode.getState());
            backPropagate(expandedNode, reward);
        });
        /*for (TreeNode expandedNode: expandedNodes) {
            double reward = rollout(expandedNode.getState());
            //4. Backpropagation
            backPropagate(expandedNode, reward);
        }*/
        
        updateUCTList();
    }

    private void saveRound(int iteration, double totalTimeRound) {

        /*File file = new File(saveFilePath);
        BufferedWriter csvWriter = null;
        try {
            csvWriter = new BufferedWriter(new FileWriter(saveFilePath, true));

            //FileWriter csvWriter = new FileWriter(savePath, true);
            if (file.length() == 0) {
                String header = "iterations,time(s),score\n";
                csvWriter.append(header);
            }
            String result = "" + iteration + "," + totalTimeRound + "," + bestScore + "\n";*/
            //System.out.println("Results iteration:" + iteration);
            System.out.println("Total time iteration: " + totalTimeRound);
            System.out.println("Best Score: " + bestScore);
            System.out.println("Best order: " + toStringOrder(bestOrder));
            List<Integer> order = new ArrayList<>();
            for (Node node : bestOrder) {
                order.add(problem.getHashIndices().get(node));
            }
            System.out.println("  " + bestScore + "\t-> " + order);
            System.out.println("Best partial order: " + toStringOrder(bestPartialOrder));
            System.out.println("------------------------------------------------------");
            /*csvWriter.append(result);

            csvWriter.flush();
            csvWriter.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }*/
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

            // Getting the best node
            if (this.selectionSet.size() <= 0) break;
            TreeNode selectNode = Collections.max(this.selectionSet);

            // Checking if the parent of the best node has already been expanded at least once
            if (selectNode.getParent() == null || selectNode.getParent().isExpanded()) {
                this.selectionSet.remove(selectNode);
                selection.add(selectNode);
            }
            else {
                // Parent has not been fully expanded, adding it for expansion
                this.selectionSet.remove(selectNode.getParent());
                selection.add(selectNode.getParent());
            }
            
            System.out.println("SELECTED: " + printUCB(selectNode) + "   " + selectNode.getState().getNode() + " -> " + problem.getHashIndices().get(selectNode.getState().getNode()));
        }
        
        /*for (TreeNode tn : selectionQueue) {
            System.out.println(printUCB(tn));
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {}
        }*/

        return  selection;
    }
    
    private double printUCB(TreeNode o) {
        if(o.getParent() == null){
            return Double.MAX_VALUE;
        }
        return  ((o.getTotalReward() / o.getNumVisits()) - Problem.emptyGraphScore ) / Problem.nInstances +
                    MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(o.getParent().getNumVisits()) / o.getNumVisits());
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
                    selectionSet.add(newNode);
                    nExpansion++;

                    //return newNode;
                }
            }
        }
        return expansion;
    }
    
    /**
     * @param state State that provides the partial order for a final randomly generated order.
     * @return
     */
    public double inverseRollout(State state){
        double score;

        // Creating a total order with the partial order of the state.
        List<Node> order = state.getOrder();
        List<Node> finalOrder = new ArrayList<>(problem.getVariables().size());
        finalOrder.addAll(order);

        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, cache);
        
        // Creating the candidates set
        HashSet<Integer> candidates = new HashSet<>(problem.getVariables().size());
        for (Node node : problem.getVariables()) {
            if (!order.contains(node)) {
                candidates.add(problem.getHashIndices().get(node));
            }
        }
        
        // We calculate the best parents for each node, and append the best to the order
        while (!candidates.isEmpty()){
            ArrayList<Pair> evaluations = new ArrayList<>();
            for (Integer node : candidates) {
                evaluations.add(hc.evaluate(node, candidates));
            }

            // Add the best node to the head of the order
            Pair best = Collections.max(evaluations);
            finalOrder.add(0, problem.getNode(best.node));
            candidates.remove(best.node);
            
            // Save the Pair
            if (bestBDeuForNode[best.node] == null || 
                    bestBDeuForNode[best.node].bdeu < best.bdeu) {
                bestBDeuForNode[best.node] = best;
            }
        }
        
        hc.setOrder(finalOrder);
        score = hc.search();

        checkBestScore(score, finalOrder, order, hc);

        return score;
    }

    synchronized private void checkBestScore(double score, List<Node> finalOrder, List<Node> order, HillClimbingEvaluator hc) {
        if(score > bestScore){
            bestScore = score;
            bestOrder = finalOrder;
            bestPartialOrder = order;
            bestDag = hc.getGraph();
        }
        //System.out.println("    Score: " + score);
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

            HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, cache);
            hc.setOrder(finalOrder);
            
            double score = hc.search();
            scoreSum+= score;
            
            // Updating best score, order and graph
            checkBestScore(score, finalOrder, order, hc);
        }
        scoreSum = scoreSum / NUM_ROLLOUTS;

        return scoreSum;
    }

    /**
     * Backpropagates the reward and visits of the nodes where a rollout has been done.
     * @param node Node that
     * @param reward
     */
    synchronized public void backPropagate(TreeNode node, double reward){
        // REVISAR!!!!
        TreeNode currentNode = node;
        while (currentNode != null){
            // Add one visit and total reward to the currentNode
            currentNode.incrementOneVisit();
            currentNode.addReward(reward);
            
            if(!currentNode.isFullyExpanded()) {
                selectionSet.add(currentNode);
            }

            //System.out.println(currentNode.getTotalReward() + ", visitas: " + currentNode.getNumVisits() + "   " + currentNode.getState().getNode() + " -> " + problem.getHashIndices().get(currentNode.getState().getNode()));

            // Update currentNode to its parent.
            currentNode = currentNode.getParent();
        }
    }
    
    public void updateUCTList() {
        for (TreeNode tn : selectionSet){
            tn.updateUCT();
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
    
    public String queueToString(){
        String res = "";
        res += ("\n\nEN COLA: \n");
        for (TreeNode tn : selectionSet) {
            double explotationScore = ((tn.getTotalReward() / tn.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances;
            double explorationScore = 0;
            if (tn.getParent() != null)
                explorationScore = MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(tn.getParent().getNumVisits()) / tn.getNumVisits());
            res += (tn.getState().getNode().getName() + "\t\t" + tn.getNumVisits() + "   " + tn.getUCTSCore() + "   " + explotationScore + "   " + explorationScore + "\n");
        }

        return res;
    }

}
