package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Graph;
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
import org.albacete.simd.utils.Utils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

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
    public static final double EXPLORATION_CONSTANT = 2 * Math.sqrt(2); //1.0 / Math.sqrt(2);
    
    public static final double EXPLOITATION_CONSTANT = 50;
    
    public static final double A_STAR_CONSTANT = 0;
    
    private static final int NUM_ROLLOUTS = 1;

    private static final int NUM_SELECTION = 1;

    private static final int NUM_EXPAND = 1;
    
    private static final int RANDOM_SELECTIONS = 3;

    /**
     * Problem of the search
     */
    private final Problem problem;
    
    private final ArrayList<Integer> allVars;
    
    private final HillClimbingEvaluator hc;

    private TreeNode root;

    private double bestScore = Double.NEGATIVE_INFINITY;
    private List<Integer> bestOrder = new ArrayList<>();
    private List<Integer> bestPartialOrder = new ArrayList<>();
    private Graph bestDag = null;

    private boolean convergence = false;

    private final ConcurrentHashMap<String,Double> cache;

    private final HashSet<TreeNode> selectionSet = new HashSet<>();

    private final String saveFilePath = "prueba-mctsbn.csv";
    
    private final Random random = new Random();
    
    private double mean;
    private double standardDeviation;
    
    private final ArrayList<ArrayList> orderSet = new ArrayList<>();

    public MCTSBN(Problem problem, int iterationLimit){
        this.problem = problem;
        this.cache = problem.getLocalScoreCache();
        this.ITERATION_LIMIT = iterationLimit;
        this.hc = new HillClimbingEvaluator(problem, cache);
        this.allVars = hc.nodeToIntegerList(problem.getVariables());
    }


    public Dag_n search(State initialState){
        //1. Set Root
        this.root = new TreeNode(initialState, null);

        System.out.println("\n\nSTARTING warmup\n------------------------------------------------------");

        //1.5 Add PGES order
        initializeWithPGES(4);

        //1.5. Create a node for each variable (totally expand root). Implicit warmup
        // allVars.size()-1 if we do initializeWithPGES
        ArrayList<TreeNode> selection = new ArrayList<>();
        selection.add(this.root);
        
        double[] rewards = new double[allVars.size()];
        for (int i = 0; i < allVars.size()-1; i++) {
            // 2. Expand selected node
            TreeNode expandedNode = expand(selection).get(0);

            //3. Rollout and Backpropagation
            double reward = rollout(expandedNode.getState());
            rewards[i] = reward;
            backPropagate(expandedNode, reward);
        }
        this.root.setFullyExpanded(true);
        
        // Train the normalizer with the mean and sd of the scores of all vars
        normalize_fit(rewards);
        
        // Convert the bdeus obtained
        root.setTotalReward(normalize_predict(root.getTotalReward()));
        for (TreeNode tn : root.getChildren()) {
            double reward = normalize_predict(tn.getTotalReward());
            tn.setTotalReward(reward);
        }
        
        
        // Update the UCT's
        updateUCTList();

        
        
        System.out.println(Arrays.toString(hc.bestBDeuForNode));
        System.out.println(this);
        

        
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
        System.out.println(this);
        // return Best Dag
        return new Dag_n(bestDag);
    }


    public Dag_n search(){
        State initialState = new State(-1, new ArrayList<>(), allVars, problem, 0, hc);
        return this.search(initialState);
    }

    /**
     * Executes one round of the selection, expansion, rollout and backpropagation iterations.
     */
    private void executeRound(){
        //1. Selection and Expansions
        List<TreeNode> selectedNodes = selectNode();
        if(selectedNodes.isEmpty()) {
            convergence = true;
            return;
        }
        // 2. Expand selected node
        List<TreeNode> expandedNodes = expand(selectedNodes);

        //3. Rollout and Backpropagation
        expandedNodes.parallelStream().forEach(expandedNode -> {
            double reward = rollout(expandedNode.getState());
            backPropagate(expandedNode, normalize_predict(reward));
        });

        updateUCTList();
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
            
            System.out.println("SELECTED: " + printUCB(selectNode) + "   " + selectNode.getState().getNode());
        }
        
        /*for (TreeNode tn : selectionQueue) {
            System.out.println(printUCB(tn));
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {}
        }*/

        return selection;
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
            List<Integer> actions = node.getState().getPossibleActionsbyOrder(getRandomOrder());
            //2. Get actions already taken for this node
            Set<Integer> childrenActions = node.getChildrenAction();
            for (Integer action: actions) {

                // Checking if the number of expansion for this node is greater than the limit
                if(nExpansion >= NUM_EXPAND)
                    break;

                //3. Check if the actions has already been taken
                if (!childrenActions.contains(action)){
                    // 4. Expand the tree by creating a new node and connecting it to the tree.
                    TreeNode newNode = new TreeNode(node.getState().takeAction(action), node);

                    // 5. Check if there are more actions to be expanded in this node, and if not, change the isFullyExpanded value
                    if(node.getChildrenAction().size() == actions.size())
                        node.setFullyExpanded(true);

                    // 7. Adding the expanded node to the list and queue
                    expansion.add(newNode);
                    selectionSet.add(newNode);
                    nExpansion++;
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
        // Creating a total order with the partial order of the state.
        List<Integer> order = state.getOrder();
        List<Integer> finalOrder = new ArrayList<>(problem.getVariables().size());
        finalOrder.addAll(order);
        
        // Getting the score of the variables that we know
        //double score = state.getLocalScore();

        // Creating the candidates set
        HashSet<Integer> candidates = new HashSet<>(problem.getVariables().size());
        for (Integer node : allVars) {
            if (!order.contains(node)) {
                candidates.add(node);
            }
        }
        
        // We calculate the best parents for each node, and append the best to the order
        while (!candidates.isEmpty()){
            ArrayList<Pair> evaluations = new ArrayList<>();
            HashSet<Integer> copyCandidates = new HashSet<>(candidates);
            
            // Calculate values for each variable
            for (Integer node : candidates) {
                copyCandidates.remove(node);
                evaluations.add(hc.evaluate(node, copyCandidates));
                copyCandidates.add(node);
            }
            
            // Sort the list, and random choose from the first RANDOM_SELECTIONS values
            Collections.sort(evaluations);
            
            int selections = RANDOM_SELECTIONS;
            if (RANDOM_SELECTIONS > evaluations.size()) selections = evaluations.size();
            Pair selected = evaluations.get(random.nextInt(selections));

            // Add the best node to the head of the order
            //Pair selected = Collections.max(evaluations);
            
            finalOrder.add(0, selected.node);
            candidates.remove(selected.node);
            
            //score += selected.bdeu;
        }
        
        hc.setOrder(finalOrder);
        double score = hc.search();

        checkBestScore(score, finalOrder, order);

        return score;
    }

    synchronized private void checkBestScore(double score, List<Integer> finalOrder, List<Integer> order) {
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
            List<Integer> order = state.getOrder();
            
            // Random order
            /*List<Integer> candidates = new ArrayList(allVars);
            candidates = candidates.stream().filter(node -> !order.contains(node)).collect(Collectors.toList());
            Collections.shuffle(candidates);*/
            
            // Pseudorandom order by PGES
            List<Integer> candidates = getRandomOrder();
            candidates = candidates.stream().filter(node -> !order.contains(node)).collect(Collectors.toList());
            
            // Creating order for HC
            List<Integer> finalOrder = new ArrayList<>(order);
            finalOrder.addAll(candidates);

            hc.setOrder(finalOrder);
            
            double score = hc.search();
            scoreSum+= score;
            
            // Updating best score, order and graph
            checkBestScore(score, finalOrder, order);
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
        currentNode.decrementOneVisit();
        while (currentNode != null){
            // Add one visit and total reward to the currentNode
            currentNode.incrementOneVisit();
            currentNode.addReward(reward);
            
            if(!currentNode.isFullyExpanded()) {
                selectionSet.add(currentNode);
            }

            // Update currentNode to its parent.
            currentNode = currentNode.getParent();
        }
    }
    
    private ArrayList<Integer> getRandomOrder() {
        return new ArrayList(orderSet.get(random.nextInt(orderSet.size())));
    }
    
    private void initializeWithPGES(int nThreads) {        
        // Execute PGES to obtain a good order
        double init = System.currentTimeMillis();
        Clustering hierarchicalClustering = new HierarchicalClustering();
        BNBuilder algorithm = new PGESwithStages(problem, hierarchicalClustering, nThreads, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        algorithm.search();

        // Create the set with some orders to use in rollout
        Dag_n currentDag = algorithm.getCurrentDag();
        for (int i = 0; i < 10000; i++) {
            orderSet.add(hc.nodeToIntegerList(currentDag.getTopologicalOrder()));
        }

        System.out.println("\n\nFINISHED PGES (" + ((System.currentTimeMillis() - init)/1000.0) + " s). BDeu: " + GESThread.scoreGraph(algorithm.getCurrentDag(), problem));
    }
    
    /**
     * Normalize (standardize) the sample, so it is has a mean of 0 and a standard deviation of 1.
     *
     * @param sample Sample to normalize.
     * @return normalized (standardized) sample.
     * @since 2.2
     */
    private void normalize_fit(double[] sample) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        // Add the data from the series to stats
        for (int i = 0; i < sample.length; i++) {
            stats.addValue(sample[i]);
        }

        // Compute mean and standard deviation
        mean = stats.getMean();
        standardDeviation = stats.getStandardDeviation();
    }
    
    private double normalize_predict(double sample) {
        return (sample - mean) / standardDeviation;
    }

    
    private double printUCB(TreeNode o) {
        if(o.getParent() == null){
            return Double.MAX_VALUE;
        }
        return  ((o.getTotalReward() / o.getNumVisits()) - Problem.emptyGraphScore ) / Problem.nInstances +
                    MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(o.getParent().getNumVisits()) / o.getNumVisits());
    }
    
    public void updateUCTList() {
        double best = Arrays.stream(hc.bestBDeuForNode).sum();
        for (TreeNode tn : selectionSet){
            tn.updateUCT(best);
        }
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
            //System.out.println("Total time iteration: " + totalTimeRound);
            //System.out.println("Best Score: " + bestScore);
            System.out.println("Best order:      " + bestScore + "\t-> " + bestOrder);
            System.out.println("Best order: " + toStringOrder(bestOrder));
            //System.out.println("Best partial order: " + toStringOrder(bestPartialOrder));
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

    public List<Integer> getBestOrder(){
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

    public String toStringOrder(List<Integer> order){
        StringBuilder result = new StringBuilder();
        for (Integer o: order) {
            result.append(problem.getVarNames()[o]).append(" < ");
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
            res += ("N"+tn.getState().getNode() + "\t\t" + tn.getNumVisits() + "   " + tn.getUCTScore() + "   " + explotationScore + "   " + explorationScore + "\n");
        }

        return res;
    }

}
