package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Graph;
import me.tongfei.progressbar.ProgressBar;
import org.albacete.simd.utils.Problem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.mctsbn.HillClimbingEvaluator.Pair;
import org.albacete.simd.threads.GESThread;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;

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
    public static double EXPLORATION_CONSTANT = 1 * Math.sqrt(2); //1.0 / Math.sqrt(2);
    
    public static double EXPLOITATION_CONSTANT = 50;
    
    public static double PROBABILITY_SWAP = 0.5;

    public static double NUMBER_SWAPS = 0.5;
    
    public static int NUM_ROLLOUTS = 1;

    public int NUM_SELECTION = 1;

    public static int NUM_EXPAND = 1;

    /**
     * Problem of the search
     */
    private Problem problem;
    
    private final ArrayList<Integer> allVars;

    private final HillClimbingEvaluator hc;

    //private final List<HillClimbingEvaluator> evaluators;

    private TreeNode root;

    private double bestScore = Double.NEGATIVE_INFINITY;
    private List<Integer> bestOrder = new ArrayList<>();
    private Graph bestDag = null;

    private boolean convergence = false;

    //private final SortedSet<TreeNode> selectionSet = Collections.synchronizedSortedSet(new TreeSet<>());
    private final MCTSQueue<TreeNode> candidates;
    private final Random random = new Random(42);

    private double mean;
    private double standardDeviation;

    private Dag_n pgesDag;

    private final ArrayList<ArrayList<Integer>> orderSet = new ArrayList<>();

    public double pgesTime;

    private BufferedWriter csvWriter;
    private String header;
    private boolean isDistributed = true;

    private double timeSpentSavingRoundsSeconds = 0;

    public MCTSBN(Problem problem, int iterationLimit){
        this.problem = problem;
        this.ITERATION_LIMIT = iterationLimit;
        //this.evaluators = HillClimbingEvaluator.createEvaluators(Runtime.getRuntime().availableProcessors(), problem);
        this.hc = new HillClimbingEvaluator(problem);
        this.allVars = problem.nodeToIntegerList(problem.getVariables());
        this.candidates = new MCTSQueue<>(problem.getVariables().size(), TreeNode::compareTo);
    }

    public MCTSBN(Problem problem, int iterationLimit, String netName, String databaseName, int threads, double exploitConstant, double numberSwaps, double probabilitySwap, int selectionConstant){
        this(problem, iterationLimit);
        this.NUM_SELECTION = selectionConstant;
        configSaveFile(iterationLimit, netName, databaseName, threads, exploitConstant, numberSwaps, probabilitySwap);
    }

    private void configSaveFile(int iterationLimit, String netName, String databaseName, int threads, double exploitConstant, double numberSwaps, double probabilitySwap) {
        String projectFolder = "/home/jorlabs/projects/ParallelBNs/";
        String resultFolder = projectFolder + "results/mctsbn-distributed/results-it/";
        String savePath = resultFolder + "experiment_" + netName + "_mcts_" +
                databaseName + "_t" + threads + "_it" + iterationLimit + "_ex" + exploitConstant
                + "_ps" + numberSwaps + "_ns" + probabilitySwap + ".csv";
        // Write results in each round
        File file = new File(savePath);
        try {
            csvWriter = new BufferedWriter(new FileWriter(savePath, true));
            if (file.length() == 0) {
                String header = "algorithm,network,bbdd,threads,itLimit,exploitConst,numSwaps,probSwap,bdeuMCTS,iteration,time\n";
                csvWriter.append(header);
            }
            csvWriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.header = "mcts," + netName + "," + databaseName + "," + threads + "," + iterationLimit + "," + exploitConstant + "," + numberSwaps + "," + probabilitySwap + ",";
    }

    public Dag_n search(State initialState){
        //1. Get initial order
        initialConfiguration(initialState);

        //2. Search loop
        try (ProgressBar pb = new ProgressBar("MCTSBN", ITERATION_LIMIT)){
            for (int i = 0; i < ITERATION_LIMIT; i++) {
                pb.step();
                //System.out.println("Iteration " + i + "...");

                // Executing round
                long startTime = System.currentTimeMillis();
                executeRound();
                long endTime = System.currentTimeMillis();
                // Calculating time of the iteration
                double totalTimeRound = 1.0 * (endTime - startTime) / 1000;

                // Showing tree structure
                //System.out.println("Tree Structure:");
                //System.out.println(this);
                startTime = System.currentTimeMillis();
                saveRound(i, totalTimeRound);
                endTime = System.currentTimeMillis();
                timeSpentSavingRoundsSeconds += 1.0 * (endTime - startTime) / 1000;

                if (convergence) {
                    System.out.println("Convergence has been found. Ending search");
                    pb.stepTo(ITERATION_LIMIT);
                    break;
                }
            }
        }
        if(csvWriter != null) {
            try {
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        //System.out.println(queueToString());
        //System.out.println("Finished...");
        //System.out.println("Tree Structure: ");
        //System.out.println(this);
        // return Best Dag
        return new Dag_n(bestDag);
    }

    private void initialConfiguration(State initialState) {
        System.out.println("\n\nSTARTING warmup\n------------------------------------------------------");

        //1. Set Root
        setRoot(initialState);

        //1.5 Add PGES order
        initializeWithPGES();

        //1.5. Create a node for each variable (totally expand root). Implicit warmup
        // allVars.size()-1 if we do initializeWithPGES
        ArrayList<TreeNode> selection = initializeSelection();

        double[] rewards = gerInitialRewards(selection);
        // Train the normalizer with the mean and sd of the scores of all vars
        normalizeFit(rewards);

        // Normalize the scores of the initial tree
        normalizeTreeScore();

        // Update the UCT's
        updateUCTList();


        //System.out.println(Arrays.toString(hc.bestBDeuForNode));
        System.out.println(this);


        System.out.println("\n\nSTARTING MCTSBN\n------------------------------------------------------");
    }

    private double[] gerInitialRewards(ArrayList<TreeNode> selection) {
        double[] rewards;
        if(!isDistributed) {
            rewards = getInitialRewardsSequential(selection);
        }
        else {
            rewards = getInitialRewardsDistributed(selection);
        }
        return rewards;
    }

    /**
     * Initializes the selection arraylist with only the root node
     * @return selection arraylist with root node
     */
    @NotNull
    private ArrayList<TreeNode> initializeSelection() {
        ArrayList<TreeNode> selection = new ArrayList<>();
        selection.add(this.root);
        return selection;
    }

    /**
     *Breath First Search to apply a normalization of the score to each treenode
     */
    private void normalizeTreeScore() {
        // Breath First Search to apply a normalization to each treenode
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while(!queue.isEmpty()){
            TreeNode node = queue.poll();
            node.setTotalReward(normalize_predict(node.getTotalReward()));
            queue.addAll(node.getChildren());
        }
    }

    /*private double[] getInitialRewardsDistributed(ArrayList<TreeNode> selection) {
        double[] rewards;
        double random_const = PROBABILITY_SWAP;
        PROBABILITY_SWAP = 0;
        rewards = allVars.parallelStream().mapToDouble(selectedNode -> {
            //2. Expand selected node
            TreeNode expandedNode = expand(selection).get(0);
            //3. Rollout and Backpropagation
            double reward = rollout(expandedNode.getState());
            backPropagate(expandedNode, reward);
            return reward;
        }).toArray();
        this.root.setFullyExpanded(true);
        PROBABILITY_SWAP = random_const;
        return rewards;
    }*/
    private double[] getInitialRewardsDistributed(ArrayList<TreeNode> selection) {
        double[] rewards;
        double random_const = PROBABILITY_SWAP;
        PROBABILITY_SWAP = 0;
        rewards = allVars.parallelStream().mapToDouble(selectedNode -> {
            //2. Expand selected node
            TreeNode expandedNode = expand(selection).get(0);
            //3. Rollout and Backpropagation
            double reward = rollout(expandedNode.getState(), new HillClimbingEvaluator(problem));
            backPropagate(expandedNode, reward);
            return reward;
        }).toArray();
        this.root.setFullyExpanded(true);
        PROBABILITY_SWAP = random_const;
        System.out.println("Distributed Rewards: ");
        System.out.println(Arrays.toString(rewards));
        return rewards;
    }


    @NotNull
    private double[] getInitialRewardsSequential(ArrayList<TreeNode> selection) {
        double[] rewards;
        double random_const = PROBABILITY_SWAP;
        PROBABILITY_SWAP = 0;
        rewards = new double[allVars.size()];
        for (int i = 0; i < allVars.size(); i++) {
            // 2. Expand selected node
            TreeNode expandedNode = expand(selection).get(0);

            //3. Rollout and Backpropagation
            double reward = rollout(expandedNode.getState(), hc);
            rewards[i] = reward;
            backPropagate(expandedNode, reward);
        }
        this.root.setFullyExpanded(true);
        PROBABILITY_SWAP = random_const;
        System.out.println("Sequential Rewards: ");
        System.out.println(Arrays.toString(rewards));
        return rewards;
    }

    private void setRoot(State initialState) {
        this.root = new TreeNode(initialState, null);
    }

    private void initializeWithPGES() {
        // Execute PGES to obtain a good order
        double init = System.currentTimeMillis();
        Clustering hierarchicalClustering = new HierarchicalClustering();
        BNBuilder algorithm = new PGESwithStages(problem, hierarchicalClustering, 4, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
        algorithm.search();

        // Create the set with some orders to use in rollout
        Dag_n currentDag = algorithm.getCurrentDag();
        for (int i = 0; i < 10000; i++) {
            orderSet.add(problem.nodeToIntegerList(currentDag.getTopologicalOrder()));
        }

        System.out.println("\n\nFINISHED PGES (" + ((System.currentTimeMillis() - init)/1000.0) + " s). BDeu: " + GESThread.scoreGraph(algorithm.getCurrentDag(), problem));

        this.pgesTime = (System.currentTimeMillis() - init)/1000.0;
        this.pgesDag = currentDag;
    }

    /**
     * Normalize (standardize) the sample, so it is has a mean of 0 and a standard deviation of 1.
     *
     * @param sample Sample to normalize.
     * @return normalized (standardized) sample.
     * @since 2.2
     */
    private void normalizeFit(double[] sample) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        // Add the data from the series to stats
        for (double v : sample) {
            stats.addValue(v);
        }

        // Compute mean and standard deviation
        mean = stats.getMean();
        standardDeviation = stats.getStandardDeviation();
    }

    private double normalize_predict(double sample) {
        return (sample - mean) / standardDeviation;
    }
    public void updateUCTList() {
        //double best = Arrays.stream(hc.bestBDeuForNode).sum(); //best is never used as the bestAStar argument
        for (TreeNode tn : candidates){
            //tn.updateUCT(best);
            tn.updateUCT(0);
        }
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
            double reward = rollout(expandedNode.getState(), new HillClimbingEvaluator(problem));
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

            TreeNode selectNode = selectCandidate();
            if(selectNode == null)
                break;
            selection.add(selectNode);
            //System.out.println("SELECTED: " + printUCB(selectNode) + "   " + selectNode.getState().getNode());
        }
        return selection;
    }

    private TreeNode selectCandidate() {
        if(candidates.isEmpty())
            return null;
        // Getting the best node from the candidates queue.
        TreeNode selectNode = candidates.poll();
        // Checking if the parent of the best node has already been expanded at least once
        if ((selectNode.getParent() != null) && (!selectNode.getParent().isExpanded())) {
            // Updating selectNode to parent node and removing it from the candidates if it is already present.
            candidates.remove(selectNode.getParent());
            selectNode = selectNode.getParent();
        }
        return selectNode;
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
            List<Integer> actions = node.getState().getPossibleActions(getRandomOrder());
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
                    candidates.add(newNode);
                    nExpansion++;
                }
            }
        }
        return expansion;
    }



    /**
     * Expands only one node and generates one node after applying an action generated from the pGES order
     * @param selectedNode Node from the selection phase
     * @return returns a child node after expanding the node.
     */
    public TreeNode expand(TreeNode selectedNode){
        //1. Get all possible actions using as hint a pges order
        List<Integer> actions = selectedNode.getState().getPossibleActions(getRandomOrder());
        //Collections.shuffle(actions);
        //2. Get actions already taken for this node
        Set<Integer> childrenActions = selectedNode.getChildrenAction();
        for (Integer action: actions) {
            //3. Check if the actions has already been taken
            if (!childrenActions.contains(action)){
                // 4. Expand the tree by creating a new node and connecting it to the tree.
                TreeNode newNode = new TreeNode(selectedNode.getState().takeAction(action), selectedNode);

                // 5. Check if there are more actions to be expanded in this node, and if not, change the isFullyExpanded value
                if(selectedNode.getChildrenAction().size() == actions.size())
                    selectedNode.setFullyExpanded(true);

                // 7. Adding the expanded node to the list and queue
                candidates.add(newNode);
                return newNode;
            }
        }
        return null;
    }


    /**
     * @param state State that provides the partial order for a final randomly generated order.
     * @return
     */
    /*
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
    */

    /**
     * Random policy rollout. Given a state with a partial order, we generate a random order that starts off with the initial order
     * @param state State that provides the partial order for a final randomly generated order.
     * @return
     */
    public double rollout(State state, HillClimbingEvaluator evaluator){
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

            for (int j = 0; j < NUMBER_SWAPS * Math.sqrt(finalOrder.size()); j++) {
                if (PROBABILITY_SWAP > 0 && random.nextDouble() <= PROBABILITY_SWAP) {
                    // Randomly swap two elements in the order
                    int index1, index2;
                    do {
                        index1 = random.nextInt(finalOrder.size());
                        index2 = random.nextInt(finalOrder.size());
                    } while(index1 == index2);
                    Collections.swap(finalOrder, index1, index2);
                    //System.out.println("Swapping " + index1 + " and " + index2);
                }
            }

            // BOOKMARK! EL PROBLEMA ES QUE SE COMPARTE EL HC ENTRE TODOS LOS HILOS.
            evaluator.setOrder(finalOrder);
            
            double score = evaluator.search();
            scoreSum+= score;
            
            // Updating best score, order and graph
            checkBestScore(score, finalOrder, evaluator.getGraph());
        }
        scoreSum = scoreSum / NUM_ROLLOUTS;

        return scoreSum;
    }

    synchronized private void checkBestScore(double score, List<Integer> finalOrder, Graph graph) {
        if(score > bestScore){
            bestScore = score;
            bestOrder = finalOrder;
            bestDag = graph;
        }
        //System.out.println("    Score: " + score);
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
                candidates.add(currentNode);
                //addCandidate(currentNode);
            }

            // Update currentNode to its parent.
            currentNode = currentNode.getParent();
        }
    }
    
    private ArrayList<Integer> getRandomOrder() {
        return new ArrayList<>(orderSet.get(random.nextInt(orderSet.size())));
    }

    
    private double printUCB(TreeNode o) {
        if(o.getParent() == null){
            return Double.MAX_VALUE;
        }
        return  ((o.getTotalReward() / o.getNumVisits()) - Problem.emptyGraphScore ) / Problem.nInstances +
                    MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(o.getParent().getNumVisits()) / o.getNumVisits());
    }
    

    
    
    private void saveRound(int iteration, double totalTimeRound) {
        if(csvWriter == null){
            return;
        }
        try {
            //System.out.println("Best order:      " + bestScore + "\t-> " + bestOrder);
            //System.out.println("Best order: " + toStringOrder(bestOrder));
            //System.out.println("------------------------------------------------------");

            String result = (header
                    + bestScore + ","
                    + iteration + ","
                    + totalTimeRound + "\n");
            csvWriter.append(result);
            csvWriter.flush();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
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

    public List<Integer> getBestOrder(){
        return bestOrder;
    }

    public double getBestScore(){
        return bestScore;
    }

    public Graph getBestDag() {
        return bestDag;
    }

    public Dag_n getPGESDag() {
        return pgesDag;
    }


    public boolean isDistributed() {
        return isDistributed;
    }

    public void setDistributed(boolean distributed) {
        isDistributed = distributed;
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
        for (TreeNode tn : candidates) {
            double explotationScore = ((tn.getTotalReward() / tn.getNumVisits()) - Problem.emptyGraphScore) / Problem.nInstances;
            double explorationScore = 0;
            if (tn.getParent() != null)
                explorationScore = MCTSBN.EXPLORATION_CONSTANT * Math.sqrt(Math.log(tn.getParent().getNumVisits()) / tn.getNumVisits());
            res += ("N"+tn.getState().getNode() + "\t\t" + tn.getNumVisits() + "   " + tn.getUCTScore() + "   " + explotationScore + "   " + explorationScore + "\n");
        }

        return res;
    }

    public HillClimbingEvaluator getHc() {
        return hc;
    }

    public boolean getIsDistributed(){
        return isDistributed;
    }
    public void setIsDistributed(boolean isDistributed){
        this.isDistributed = isDistributed;
    }

    public Problem getProblem(){
        return this.problem;
    }

    public void setProblem(Problem problem){
        this.problem = problem;
    }

    public int getNUM_SELECTION() {
        return NUM_SELECTION;
    }

    public void setNUM_SELECTION(int NUM_SELECTION) {
        this.NUM_SELECTION = NUM_SELECTION;
    }

    public double getTimeSpentSavingRoundsSeconds(){
        return this.timeSpentSavingRoundsSeconds;
    }


}
