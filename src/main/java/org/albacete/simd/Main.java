package org.albacete.simd;

import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;
import consensusBN.ConsensusUnion;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Jorge Daniel Laborda
 * @version 0.1
 * Main class. This class contains the methods and variables used to run the parallel BN algorithm
 */
public class Main
{
    /**
     * {@link DataSet DataSet}DataSet containing the values of the variables of the problem in hand.
     */
    private final DataSet data;

    /**
     * The number of threads the algorithm is going to use.
     */
    private int nThreads = 1;

    /**
     * Seed for the random number generator.
     */
    private long seed = 42;

    /**
     * Number of iterations allowed inside the FES stage. This is a hyperparameter used in experimentation.
     */
    private int nFESItInterleaving = 5;

    /**
     * The maximum number of iterations allowed for the algorithm.
     */
    private int maxIterations = 15;

    /**
     * The {@link GESThread GESThread} array that will be executed in each stage.
     * They can either be {@link ThFES ThFES} or {@link ThBES ThBES} threads.
     */
    private GESThread[] gesThreads = null;

    /**
     * The {@link Thread Thread} array that encapsulate the {@link GESThread GESThread} for each stage.
     */
    private Thread[] threads = null;

    /**
     * Subset of {@link TupleNode TupleNodes}. Each subset will be assigned to {@link GESThread GESThread}
     */
    private ArrayList<TupleNode>[] subSets = null;

    /**
     * {@link ArrayList ArrayList} of graphs. This contains the list of {@link Graph graphs} created for each stage,
     * just before the fusion is done.
     */
    private ArrayList<Dag> graphs = null;

    /**
     * {@link Graph Graph} containing the current bayesian network that has been constructed so far.
     */
    private Graph currentGraph = null;

    /**
     * Iteration counter. It stores the current iteration of the algorithm.
     */
    private int it = 1;

    /**
     * {@link TupleNode TupleNode} array containing the possible list of edges of the resulting bayesian network.
     */
    private TupleNode[] listOfArcs;

    /**
     * {@link Random Random} generator. It is instantiated with a seed number for reproducibility.
     */
    Random random = new Random(seed);



    /**
     * Constructor of Main that uses a DataSet containing the data.
     * @param data Dataset containing the data of the problem.
     * @param nThreads Number of threads used in the problem.
     */
    public Main(DataSet data, int nThreads){
        this.data = data;
        initialize(nThreads);
    }

    /**
     * Constructor of Main that uses the path to the csv file.
     * @param path path to the csv file
     * @param nThreads number of threads of the problem
     */
    public Main(String path, int nThreads){
        this.data = Utils.readData(path);
        initialize(nThreads);
    }


    /**
     * Initializes the general parameters of the class.
     * @param nThreads number of threads used in the problem.
     */
    @SuppressWarnings("unchecked")
    private void initialize(int nThreads){
        this.nThreads = nThreads;
        this.gesThreads = new ThFES[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList[this.nThreads];

        //The total number of arcs of a graph is n*(n-1)/2, where n is the number of nodes in the graph.
        this.listOfArcs = new TupleNode[this.data.getNumColumns() * (this.data.getNumColumns() -1) / 2];
    }

    /**
     * Calculates the amount of possible arcs between the variables of the dataset and stores it.
     */
    public void calculateArcs(){
        this.listOfArcs = Utils.calculateArcs(this.data);
    }
    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem
     */
    public void splitArcs(){

        this.subSets = Utils.split(listOfArcs, nThreads, seed);

    }

    /**
     * Transforms a graph to a DAG, and removes any possible inconsistency found throughout its structure.
     * @param g Graph to be transformed.
     * @return Resulting DAG of the inserted graph.
     */
    private Dag removeInconsistencies(Graph g){
        // Transforming the current graph into a DAG
        SearchGraphUtils.pdagToDag(g);

        // Checking Consistency
        Node nodeT, nodeH;
        for (Edge e : g.getEdges()){
            if(!e.isDirected()) continue;
            Endpoint endpoint1 = e.getEndpoint1();
            if (endpoint1.equals(Endpoint.ARROW)){
                nodeT = e.getNode1();
                nodeH = e.getNode2();
            }else{
                nodeT = e.getNode2();
                nodeH = e.getNode1();
            }
            if(g.existsDirectedPathFromTo(nodeT, nodeH)) g.removeEdge(e);
        }
        // Adding graph from each thread to the graphs array
        return new Dag(g);

    }

    /**
     * Configures the FES stage by initializing the graph and fes lists. It also initializes
     */
    private void fesConfig(){
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();

        // Creating each ThFES runnable
        if (this.currentGraph == null) {
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new ThFES(this.data, this.subSets[i], this.nFESItInterleaving);
            }
        }
        else{
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new ThFES(this.data, this.currentGraph, this.subSets[i], this.nFESItInterleaving);
            }
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            // Resetting the search flag
            this.gesThreads[i].resetFlag();
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }

    /**
     * Executing the threads for the corresponding stage ({@link #fesStage() fesStage} or {@link #besStage() besStage})
     * @throws InterruptedException Exception caused by an external interruption.
     */
    private void runThreads() throws InterruptedException {
        // Starting the threads
        for (Thread thread: this.threads) {
            thread.start();
        }

        // Getting results
        double score_threads = 0;
        for(int i = 0 ; i< this.nThreads; i++){
            // Joining threads and getting currentGraph
            threads[i].join();
            Graph g = gesThreads[i].getCurrentGraph();

            // Thread Score
            score_threads = score_threads + gesThreads[i].getScoreBDeu();

            // Removing Inconsistencies and transforming it to a DAG
            Dag gdag = removeInconsistencies(g);

            // Adding the new dag to the graph list
            this.graphs.add(gdag);

            //Debug
            //System.out.println("Graph of Thread " + (i +1) + ": \n" + gdag);

        }


    }

    /**
     * Runs the FES Stage, where each thread runs a FES algorithm for its corresponding subset of edges.
     * @throws InterruptedException Exception caused by an external interruption.
     */
    public void fesStage() throws InterruptedException {

        // Configuring the fes stage
        fesConfig();

        // Running threads
        runThreads();

    }

    /**
     * Configures the {@link #besStage() besStage}. It first replaces the {@link GESThread gesThreads} with
     * {@link ThBES ThBES} threads. Then it resets the execution flag for each thread and creates a new
     * {@link Thread Thread} with the previous constructed gesThreads.
     */
    private void besConfig(){
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();
        this.gesThreads = new GESThread[this.nThreads];

        // Rearranging the subsets, so that the BES stage only deletes edges of the current graph.
        ArrayList<TupleNode>[] subsets_BES = Utils.split(this.currentGraph.getEdges(), this.nThreads, this.seed);
        for (int i = 0; i < this.nThreads; i++) {
            this.gesThreads[i] = new ThBES(this.data, this.currentGraph, subsets_BES[i]);
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            // Resetting the  search flag
            this.gesThreads[i].resetFlag();
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }

    /**
     * Executes a BES algorithm for each subset of edges. This is done by creating a {@link ThBES ThBES} thread for each
     * subset of arcs, and then executing them all in parallel.
     * @throws InterruptedException Interruption caused by an external interruption.
     */
    public void besStage() throws InterruptedException {
        // Configuring the fes stage
        besConfig();

        // Running threads
        runThreads();
    }


    /**
     * Joins the Dags of either the FES or BES stage.
     * @return Dag with the fusion consensus of the graphs of the previous stage
     */
    public Dag fusion(){
        ConsensusUnion fusion = new ConsensusUnion(this.graphs);
        this.currentGraph = fusion.union();
        return (Dag) this.currentGraph;
    }


    /**
     * Convergence function that checks if the previous graph and the current graph are equal or not.
     * @return true if there is convergence, false if not.
     */
    private boolean convergence() {
        // Checking Iterations
        if (it >= this.maxIterations)
            return true;

        // Checking that the threads have done something
        // BUG: This will only get the results of the besThreads, making it impossible to know if the fesThreads have actually done something
        /*for(int i=0; i<this.nThreads; i++) {
            if (this.gesThreads[i].getFlag()) {
                it++;
                return false;
            }
        }*/
        it++;
        return false;

    }

    /**
     * Executes the algorithm. It has 7 steps. The first step is to calculate the arcs ({@link Utils calculateArcs} calculateArcs),
     * next, it enters a loop where at the start of each iteration a random repartition is done ({@link #splitArcs()} splitArcs),
     * then, the {@link #fesStage() fesStage} is executed. Once it has finished, a {@link #fusion() fusion} is done, joining
     * all of the DAGs obtained in the previous stage. The next step is the {@link #besStage() besStage}, in each subset,
     * a {@link ThBES ThBES} runs a BES algorithm, deleting any misplaced edge from the previous steps. Once all of the threads
     * have finished, then another {@link #fusion() fusion} is done. Finally, we check if there has been a convergence, and
     * repeat the process.
     */
    public void search(){


        // 1. Calculating Edges
        this.listOfArcs = Utils.calculateArcs(this.data);

        do {
            System.out.println("-----------------------");
            System.out.println("Iteration: " + (it));

            // 2 Random Repartitioning
            this.subSets = Utils.split(listOfArcs, nThreads, seed);

            // 3. FES
            try {
                fesStage();
            } catch (InterruptedException e) {
                System.err.println("Error in FES Stage");
                e.printStackTrace();
            }

            // 4. Fusion
            fusion();
            //System.out.println("FES-Fusion Graph");
            //System.out.println(this.currentGraph);

            // 5. BES
            try {
                besStage();
            } catch (InterruptedException e) {
                System.err.println("Error in BES Stage");
                e.printStackTrace();
            }
            // Printing
            System.out.println("Results of BES: ");
            for (Dag dag : this.graphs) {
                System.out.println(dag);
            }

            // 6. Fusion
            fusion();
            System.out.println("Final Graph " + "("+ it + ")");
            System.out.println(this.currentGraph);

            // 7. Checking convergence and preparing configurations for the next iteration
        }while(!convergence());
    }

    /**
     * Sets the seed for the random generator.
     * @param seed seed used for the random number generator.
     */
    public void setSeed(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * Gets the used seed for the random number generator.
     * @return seed used in the random number generator
     */
    public long getSeed(){
        return this.seed;
    }

    /**
     * Gets the list of possible edges of the problem
     * @return array of {@link TupleNode TupleNode} representing all the possible edges of the problem.
     */
    public TupleNode[] getListOfArcs() {
        return listOfArcs;
    }

    /**
     * Gets the current subsets of edges.
     * @return array of ArrayList of {@link TupleNode TupleNode} containing the edges of each subset.
     */
    public ArrayList<TupleNode>[] getSubSets() {
        return subSets;
    }

    /**
     * Gets the {@link DataSet DataSet} of the problem.
     * @return {@link DataSet DataSet} with the data of the problem.
     */
    public DataSet getData() {
        return data;
    }

    /**
     * Gets the maximum number of iterations.
     * @return number of maximum iterations.
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Sets the maximum number of iterations
     * @param maxIterations new value of the maximum number of iterations
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Sets the maximum number of iterations for each {@link ThFES ThFES}.
     * @param nFESItInterleaving maximum number of iterations used in each {@link ThFES ThFES}.
     */
    public void setNFESItInterleaving(int nFESItInterleaving) {
        this.nFESItInterleaving = nFESItInterleaving;
    }

    /**
     * Gets the current list of graphs.
     * @return ArrayList of the current Dags created in a previous stage.
     */
    public ArrayList<Dag> getGraphs(){
        return this.graphs;
    }

    /**
     * Gets the {@link #currentGraph currentGraph} constructed so far.
     * @return Dag of the currentGraph.
     */
    public Graph getCurrentGraph(){
        return this.currentGraph;
    }

    /**
     * Gets the current iteration number.
     * @return iteration the algorithm is in.
     */
    public int getIterations(){
        return it;
    }


    /**
     * Example of the algorithm running for the cancer problem.
     * @param args not used
     */
    public static void main(String[] args){
        // 1. Read Data
        String path = "src/test/resources/cancer.xbif_.csv";

        // 2. Configuring algorithm
        Main main = new Main(path, 2);
        int maxIteration = 15;
        main.setMaxIterations(maxIteration);
        main.setNFESItInterleaving(5);

        // 3. Running Algorithm
        main.search();

        // 4. Printing out the results
        System.out.println("Number of Iterations: " + main.getIterations());
        System.out.println("Resulting Graph: " + main.getCurrentGraph());

    }

}
