package org.albacete.simd.algorithms.pHCS;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.threads.*;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.TupleNode;
import org.albacete.simd.utils.Utils;

import java.util.ArrayList;

public class ParallelHillClimbingSearch {

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
     * The maximum number of iterations allowed for the algorithm.
     */
    private int maxIterations = 15;

    /**
     * The {@link GESThread GESThread} array that will be executed in each stage.
     * They can either be {@link org.albacete.simd.threads.ForwardHillClimbingThread ForwardHillClimbingThread}, {@link org.albacete.simd.threads.BackwardsHillClimbingThread BackwardsHillClimbingThread} threads.
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

    private boolean fhcFlag = false;

    private boolean bhcFlag = false;



    public ParallelHillClimbingSearch(DataSet data, int nThreads){
        this.problem = new Problem(data);
        initialize(nThreads);
    }

    public ParallelHillClimbingSearch(String path, int nThreads){
        this(Utils.readData(path),nThreads);
    }


    /**
     * Initializes the general parameters of the class.
     * @param nThreads number of threads used in the problem.
     */
    @SuppressWarnings("unchecked")
    private void initialize(int nThreads){
        this.nThreads = nThreads;
        this.gesThreads = new GESThread[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList[this.nThreads];

        //The total number of arcs of a graph is n*(n-1)/2, where n is the number of nodes in the graph.
        this.listOfArcs = new TupleNode[this.problem.getData().getNumColumns() * (this.problem.getData().getNumColumns() -1) / 2];
    }





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
            System.out.println("FHC STAGE");
            System.out.println("----------------------------");
            // 3. FHC
            try {
                fhcStage();
            } catch (InterruptedException e) {
                System.err.println("Error in FHC Stage");
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
            System.out.println("----------------------------");
            System.out.println("FHC-Fusion Graph");
            System.out.println("----------------------------");
            this.currentGraph = fusionFHC();


            // 5. BHC
            try {
                System.out.println("----------------------------");
                System.out.println("BHC STAGE");
                System.out.println("----------------------------");
                bhcStage();
            } catch (InterruptedException e) {
                System.err.println("Error in BHC Stage");
                e.printStackTrace();
            }
            // 6. BHC Fusion
            System.out.println("----------------------------");
            System.out.println("BHC-Fusion Graph");
            this.currentGraph = fusionBHC();
            System.out.println("----------------------------");

            // Printing
            //System.out.println("Results of BES: ");

            //i = 1;
            //for (Dag dag : this.graphs) {
            //    System.out.println("Thread " + i);
            //    System.out.println(dag);
            //    i++;
            //}


            // Results of the iteration
            System.out.println("----------------------------");
            System.out.println(" PRINTING GRAPH OF ITERATION " + "("+ it + ")");
            System.out.println(this.currentGraph);
            System.out.println("----------------------------");

            // 7. Checking convergence and preparing configurations for the next iteration
        }while(!convergence());
    }

    private void fhcConfig(){
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();

        // Rebuilding hashIndex
        //problem.buildIndexing(currentGraph);

        // Creating each ThFES runnable
        if (this.currentGraph == null) {
            for (int i = 0; i < this.nThreads; i++) {
                //System.out.println("Index: " + i);
                this.gesThreads[i] = new ForwardHillClimbingThread(this.problem,this.subSets[i], this.maxIterations);
            }
        }
        else{
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new ForwardHillClimbingThread(this.problem, this.currentGraph, this.subSets[i], this.maxIterations);
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
     * Configures the {@link #bhcStage() besStage}. It first replaces the {@link GESThread gesThreads} with
     * {@link BESThread ThBES} threads. Then it resets the execution flag for each thread and creates a new
     * {@link Thread Thread} with the previous constructed gesThreads.
     */
    private void bhcConfig(){
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();
        this.gesThreads = new GESThread[this.nThreads];

        // Rebuilding hashIndex
        //problem.buildIndexing(currentGraph);

        // Rearranging the subsets, so that the BES stage only deletes edges of the current graph.
        ArrayList<TupleNode>[] subsets_BHC = Utils.split(this.currentGraph.getEdges(), this.nThreads, this.seed);
        for (int i = 0; i < this.nThreads; i++) {
            this.gesThreads[i] = new BackwardsHillClimbingThread(this.problem, this.currentGraph, subsets_BHC[i]);
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            // Resetting the  search flag
            this.gesThreads[i].resetFlag();
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }


    /**
     * Executing the threads for the corresponding stage ({@link #fhcStage() fhcStage} or {@link #bhcStage() bhcStage})
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


    private void fhcStage() throws InterruptedException{
        //Configuring FHC
        fhcConfig();

        // Running threads
        runThreads();

        // Working Status
        this.fhcFlag = checkWorkingStatus();

    }

    private void bhcStage() throws InterruptedException{
        // Configuring the bes stage
        bhcConfig();

        // Running threads
        runThreads();

        // Checking working status
        this.bhcFlag = checkWorkingStatus();

    }

    private Dag fusionFHC(){
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

        System.out.println("FHC to obtain the fusion: ");


        ArrayList<TupleNode> candidates = new ArrayList<TupleNode>();


        for (Edge e: fusionGraph.getEdges()){
            if(this.currentGraph.getEdge(e.getNode1(), e.getNode2())!=null || this.currentGraph.getEdge(e.getNode2(),e.getNode1())!=null ) continue;
            candidates.add(new TupleNode(e.getNode1(),e.getNode2()));
        }


        //FESThread fuse = new FESThread(this.problem,this.currentGraph,candidates,candidates.size());
        ForwardHillClimbingThread fuse = new ForwardHillClimbingThread(problem, this.currentGraph, candidates, candidates.size());

        fuse.run();

        try {
            this.currentGraph = fuse.getCurrentGraph();
            System.out.println("Score Fusion: "+ ForwardHillClimbingThread.scoreGraph(this.currentGraph, problem));
            this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
            System.out.println("Score Fusion sin inconsistencias: "+ ForwardHillClimbingThread.scoreGraph(this.currentGraph, problem));
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        return (Dag) this.currentGraph;
    }

    public Dag fusionIntersection(){
        ArrayList<Node> order = new ArrayList<Node>(this.currentGraph.getTierOrdering());
        for(Dag g: this.graphs) {
            for(Edge e:g.getEdges()) {
                if((order.indexOf(e.getNode1()) < order.indexOf(e.getNode2())) && (e.getEndpoint1()== Endpoint.TAIL && e.getEndpoint2()==Endpoint.ARROW))
                    continue;

                if((order.indexOf(e.getNode1()) > order.indexOf(e.getNode2())) && (e.getEndpoint1()== Endpoint.ARROW && e.getEndpoint2()==Endpoint.TAIL))
                    continue;

                if(e.getEndpoint1()==Endpoint.TAIL)
                    e.setEndpoint1(Endpoint.ARROW);
                else
                    e.setEndpoint1(Endpoint.TAIL);

                if(e.getEndpoint2()==Endpoint.TAIL)
                    e.setEndpoint2(Endpoint.ARROW);
                else
                    e.setEndpoint2(Endpoint.TAIL);

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


    private Graph fusionBHC() {

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

        System.out.println("BHC to obtain the fusion: ");

        ArrayList<TupleNode> candidates = new ArrayList<TupleNode>();

        for (Edge e: this.currentGraph.getEdges()){
            if(fusionGraph.getEdge(e.getNode1(), e.getNode2())==null && fusionGraph.getEdge(e.getNode2(),e.getNode1())==null ) {
                candidates.add(new TupleNode(e.getNode1(),e.getNode2()));
            }
        }
        // Quizás sea mejor poner el BES
        //BESThread fuse = new BESThread(this.problem, this.currentGraph, candidates);
        BackwardsHillClimbingThread fuse = new BackwardsHillClimbingThread(this.problem,this.currentGraph,candidates);

        fuse.run();

        try {
            this.currentGraph = fuse.getCurrentGraph();
            System.out.println("Resultado del BHC de la fusion: "+ BackwardsHillClimbingThread.scoreGraph(this.currentGraph, problem));
            this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

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

        //System.out.println("FHCFlag: " + fhcFlag);
        //System.out.println("BHCFlag: " + bhcFlag);


        // Checking working status
        if(!fhcFlag && !bhcFlag){
            return true;
        }

        it++;
        return false;
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









}
