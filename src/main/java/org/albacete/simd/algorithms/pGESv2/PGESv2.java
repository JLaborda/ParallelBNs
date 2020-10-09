package org.albacete.simd.algorithms.pGESv2;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import consensusBN.ConsensusUnion;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.TupleNode;
import org.albacete.simd.utils.Utils;

import java.util.*;


// TAMBIÉN HAY QUE QUITAR LAS DEPENDENCIAS DE TETRAD NORMAL (4.4)

/**
 * @author Jorge Daniel Laborda
 * @version 0.1
 * Main class. This class contains the methods and variables used to run the parallel BN algorithm
 */
public class PGESv2
{
    /**
     * {@link DataSet DataSet}DataSet containing the values of the variables of the problem in hand.
     */
    //private final DataSet data;
    private Problem problem;
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
     * They can either be {@link FESThread ThFES} or {@link BESThread ThBES} threads.
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

    private boolean fesFlag = false;

    private boolean besFlag = false;


    /**
     * Constructor of Main that uses a DataSet containing the data.
     * @param data Dataset containing the data of the problem.
     * @param nThreads Number of threads used in the problem.
     */
    public PGESv2(DataSet data, int nThreads){
        this.problem = new Problem(data);
        initialize(nThreads);
    }

    public PGESv2(DataSet data, int nThreads, int maxIterations, int nFESItInterleaving){
        this(data, nThreads);
        this.maxIterations = maxIterations;
        this.nFESItInterleaving = nFESItInterleaving;
    }

    /**
     * Constructor of Main that uses the path to the csv file.
     * @param path path to the csv file
     * @param nThreads number of threads of the problem
     */
    public PGESv2(String path, int nThreads){
        this(Utils.readData(path),nThreads);
    }


    public PGESv2(String path, int nThreads, int maxIterations, int nFESItInterleaving){
        this(Utils.readData(path),nThreads, maxIterations, nFESItInterleaving);
    }



    /**
     * Initializes the general parameters of the class.
     * @param nThreads number of threads used in the problem.
     */
    @SuppressWarnings("unchecked")
    private void initialize(int nThreads){
        this.nThreads = nThreads;
        this.gesThreads = new FESThread[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList[this.nThreads];

        //The total number of arcs of a graph is n*(n-1)/2, where n is the number of nodes in the graph.
        this.listOfArcs = new TupleNode[this.problem.getData().getNumColumns() * (this.problem.getData().getNumColumns() -1) / 2];
    }

    /**
     * Calculates the amount of possible arcs between the variables of the dataset and stores it.
     */
    public void calculateArcs(){
        this.listOfArcs = Utils.calculateArcs(this.problem.getData());
    }
    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem
     */
    public void splitArcs(){
        this.subSets = Utils.split(listOfArcs, nThreads, seed);
    }



    /**
     * Configures the FES stage by initializing the graph and fes lists. It also initializes
     */
    private void fesConfig(){
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();

        // Rebuilding hashIndex
        //problem.buildIndexing(currentGraph);

        // Creating each ThFES runnable
        if (this.currentGraph == null) {
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new FESThread(this.problem, this.subSets[i], this.nFESItInterleaving);
            }
        }
        else{
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new FESThread(this.problem, this.currentGraph, this.subSets[i], this.nFESItInterleaving);
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
            Dag gdag = Utils.removeInconsistencies(g);

            // Adding the new dag to the graph list
            this.graphs.add(gdag);

            //Debug
            //System.out.println("Graph of Thread " + (i +1) + ": \n" + gdag);

        }


    }

    public boolean checkWorkingStatus() throws InterruptedException {
        for (GESThread g: gesThreads) {
           if (g.getFlag() ){
               return true;
           }
        }
        return false;
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

        //Working Status
        this.fesFlag = checkWorkingStatus();

        //Printing localScoreMap
        /*
        for (GESThread thread: gesThreads) {
            System.out.println("---------------------------");
            System.out.println("ThFES " + thread.getId());
            System.out.println("LocalScoreMap:");
            System.out.println(thread.getLocalScoreCache());
            System.out.println("---------------------------");
        }
         */

    }

    /**
     * Configures the {@link #besStage() besStage}. It first replaces the {@link GESThread gesThreads} with
     * {@link BESThread ThBES} threads. Then it resets the execution flag for each thread and creates a new
     * {@link Thread Thread} with the previous constructed gesThreads.
     */
    private void besConfig(){
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();
        this.gesThreads = new GESThread[this.nThreads];

        // Rebuilding hashIndex
        //problem.buildIndexing(currentGraph);

        // Rearranging the subsets, so that the BES stage only deletes edges of the current graph.
        ArrayList<TupleNode>[] subsets_BES = Utils.split(this.currentGraph.getEdges(), this.nThreads, this.seed);
        for (int i = 0; i < this.nThreads; i++) {
            this.gesThreads[i] = new BESThread(this.problem, this.currentGraph, subsets_BES[i]);
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            // Resetting the  search flag
            this.gesThreads[i].resetFlag();
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }

    /**
     * Executes a BES algorithm for each subset of edges. This is done by creating a {@link BESThread ThBES} thread for each
     * subset of arcs, and then executing them all in parallel.
     * @throws InterruptedException Interruption caused by an external interruption.
     */
    public void besStage() throws InterruptedException {
        // Configuring the bes stage
        besConfig();

        // Running threads
        runThreads();

        // Checking working status
        besFlag = checkWorkingStatus();

        //Printing localScoreMap
        /*
        for (GESThread thread: gesThreads) {
            System.out.println("---------------------------");
            System.out.println("ThBES " + thread.getId());
            System.out.println("LocalScoreMap:");
            System.out.println(thread.getLocalScoreCache());
            System.out.println("---------------------------");
        }
         */

    }


    /**
     * Joins the Dags of the FES stage.
     * @return Dag with the fusion consensus of the graphs of the previous stage
     */
    public Dag fusionFES(){

        // Applying ConsensusUnion fusion
        ConsensusUnion fusion = new ConsensusUnion(this.graphs);
        Graph fusionGraph = fusion.union();

        // Getting Scores
        double fusionScore = GESThread.scoreGraph(fusionGraph, problem);
        double currentScore = GESThread.scoreGraph(this.currentGraph, problem);

        System.out.println("Fusion Score: " + fusionScore);
        System.out.println("Current Score: " + currentScore);



        // Checking if the score has improved
        if (fusionScore > currentScore) {
            this.currentGraph = fusionGraph;
            return (Dag) this.currentGraph;
        }

        System.out.println("FES to obtain the fusion: ");
 

        ArrayList<TupleNode> candidates = new ArrayList<TupleNode>();
        
        
        for (Edge e: fusionGraph.getEdges()){
            if(this.currentGraph.getEdge(e.getNode1(), e.getNode2())!=null || this.currentGraph.getEdge(e.getNode2(),e.getNode1())!=null ) continue;
            candidates.add(new TupleNode(e.getNode1(),e.getNode2()));
        }

        
        FESThread fuse = new FESThread(this.problem,this.currentGraph,candidates,candidates.size());
        
        fuse.run();
        
        try {
			this.currentGraph = fuse.getCurrentGraph();
			System.out.println("Score Fusion: "+ FESThread.scoreGraph(this.currentGraph, problem));
			this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
			System.out.println("Score Fusion sin inconsistencias: "+ FESThread.scoreGraph(this.currentGraph, problem));
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

        return (Dag) this.currentGraph;
    }


    /**

     * Joins the Dags of either the FES or BES stage.

     * @return Dag with the fusion consensus of the graphs of the previous stage

     */

    public Dag fusionIntersection(){



        ArrayList<Node> order = new ArrayList<Node>(this.currentGraph.getTierOrdering());



        for(Dag g: this.graphs) {

            for(Edge e:g.getEdges()) {

                if((order.indexOf(e.getNode1()) < order.indexOf(e.getNode2())) && (e.getEndpoint1()== Endpoint.TAIL && e.getEndpoint2()==Endpoint.ARROW)) continue;

                if((order.indexOf(e.getNode1()) > order.indexOf(e.getNode2())) && (e.getEndpoint1()== Endpoint.ARROW && e.getEndpoint2()==Endpoint.TAIL)) continue;

                if(e.getEndpoint1()==Endpoint.TAIL) e.setEndpoint1(Endpoint.ARROW); else e.setEndpoint1(Endpoint.TAIL);

                if(e.getEndpoint2()==Endpoint.TAIL) e.setEndpoint2(Endpoint.ARROW); else e.setEndpoint2(Endpoint.TAIL);

            }

        }

        Graph graph = new EdgeListGraph(this.currentGraph);
        // Looping over each edge of the currentGraph and checking if it has been deleted in any of the resulting graphs of the BES stage.
        // If it has been deleted, then it is removed from the currentGraph.
        for(Edge e: graph.getEdges()) {

            for(Dag g: this.graphs)

                if(!g.containsEdge(e)) {

                    graph.removeEdge(e);

                    break;

                }



        }

        return new Dag(graph);

    }

    /**
     * Convergence function that checks if the previous graph and the current graph are equal or not.
     * @return true if there is convergence, false if not.
     */
    private boolean convergence() {
        // Checking Iterations
        if (it >= this.maxIterations)
            return true;

        // Checking working status
        if(!fesFlag && !besFlag){
            return true;
        }

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
     * then, the {@link #fesStage() fesStage} is executed. Once it has finished, a {@link #fusionFES() fusion} is done, joining
     * all of the DAGs obtained in the previous stage. The next step is the {@link #besStage() besStage}, in each subset,
     * a {@link BESThread ThBES} runs a BES algorithm, deleting any misplaced edge from the previous steps. Once all of the threads
     * have finished, then another {@link #fusionFES() fusion} is done. Finally, we check if there has been a convergence, and
     * repeat the process.
     */
    public void search(){

        // Initial Configuration: Cases
        //GESThread.setProblem(this.data);

        // 1. Calculating Edges
        this.listOfArcs = Utils.calculateArcs(this.problem.getData());

        do {
            System.out.println("============================");
            System.out.println("Iteration: " + (it));
            System.out.println("============================");

            // 2 Random Repartitioning
            this.subSets = Utils.split(listOfArcs, nThreads, seed);

            //System.out.println("----------------------------");
            //System.out.println("Splits: ");
            //int i = 1;
            //for( ArrayList<TupleNode> s : subSets){
            //    System.out.println("Split " + i);
            //    i++;
            //    for(TupleNode t : s){
            //        System.out.println(t);
            //    }
            //}
            System.out.println("----------------------------");
            System.out.println("FES STAGE");
            // 3. FES
            try {
                fesStage();
            } catch (InterruptedException e) {
                System.err.println("Error in FES Stage");
                e.printStackTrace();
            }

            // Printing
            //System.out.println("Results of FES: ");
            //i = 1;
            //for (Dag dag : this.graphs) {
            //    System.out.println("Thread " + i);
            //    System.out.println(dag);
            //   i++;
            //}

            // 4. Fusion
            this.currentGraph = fusionFES();
            System.out.println("----------------------------");
            System.out.println("FES-Fusion Graph");
            //System.out.println(this.currentGraph);
            System.out.println("----------------------------");


            // 5. BES
            try {
                System.out.println("BES STAGE");
                besStage();
            } catch (InterruptedException e) {
                System.err.println("Error in BES Stage");
                e.printStackTrace();
            }
            
            this.currentGraph = fusionBES();
            
            // Printing
            //System.out.println("Results of BES: ");

            //i = 1;
            //for (Dag dag : this.graphs) {
            //    System.out.println("Thread " + i);
            //    System.out.println(dag);
            //    i++;
            //}
            // 6. Intersection Fusion
            System.out.println("----------------------------");
            System.out.println("BES-Fusion Graph");

            // Results of the iteration
            System.out.println("Graph ITERATION " + "("+ it + ")");
            System.out.println(this.currentGraph);
            System.out.println("----------------------------");

            // 7. Checking convergence and preparing configurations for the next iteration
        }while(!convergence());
    }

    private Graph fusionBES() {
    
        Dag fusionGraph = this.fusionIntersection();

        // Getting Scores
        double fusionScore = GESThread.scoreGraph(fusionGraph, problem);
        double currentScore = GESThread.scoreGraph(this.currentGraph, problem);

        System.out.println("Fusion Score: " + fusionScore);
        System.out.println("Current Score: " + currentScore);


        // Checking if the score has improved
        if (fusionScore > currentScore) {
            this.currentGraph = fusionGraph;
            return (Dag) this.currentGraph;
        }

        System.out.println("BES to obtain the fusion: ");

        ArrayList<TupleNode> candidates = new ArrayList<TupleNode>();
        
        for (Edge e: this.currentGraph.getEdges()){
            if(fusionGraph.getEdge(e.getNode1(), e.getNode2())==null && fusionGraph.getEdge(e.getNode2(),e.getNode1())==null ) {
            	candidates.add(new TupleNode(e.getNode1(),e.getNode2()));
            }
        }

     
        
        BESThread fuse = new BESThread(this.problem,this.currentGraph,candidates);
        
        fuse.run();
        
        try {
			this.currentGraph = fuse.getCurrentGraph();
			System.out.println("Resultado del BES de la fusion: "+ BESThread.scoreGraph(this.currentGraph, problem));
			this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} 

        return (Dag) this.currentGraph;
	}

	/**
     * Sets the seed for the random generator.
     * @param seed seed used for the random number generator.
     */
    public void setSeed(long seed) {
        this.seed = seed;
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
        return problem.getData();
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
     * Sets the maximum number of iterations for each {@link FESThread ThFES}.
     * @param nFESItInterleaving maximum number of iterations used in each {@link FESThread ThFES}.
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

    public Problem getProblem() {
        return problem;
    }

    /**
     * Example of the algorithm running for the cancer problem.
     * @param args not used
     */
    public static void main(String[] args){
        // 1. Read Data
        String path = "src/test/resources/alarm.xbif_.csv";
        DataSet ds = Utils.readData(path);
 
        // 2. Configuring algorithm
        PGESv2 pGESv2 = new PGESv2(ds, 2);
        int maxIteration = 15;
        pGESv2.setMaxIterations(maxIteration);
        pGESv2.setNFESItInterleaving(5);

        // 3. Running Algorithm
        pGESv2.search();

        // 4. Printing out the results
        System.out.println("Number of Iterations: " + pGESv2.getIterations());
        System.out.println("Resulting Graph: " + pGESv2.getCurrentGraph());


    }

}
