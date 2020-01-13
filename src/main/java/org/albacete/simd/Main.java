package org.albacete.simd;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;
import consensusBN.ConsensusUnion;

import org.albacete.simd.pGES.Scorer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Main class. This class contains the methods and variables used to run the parallel BN algorithm
 */
public class Main
{
    private final DataSet data;
    private int nThreads = 1;
    private long seed = 42;
    private int nFESItInterleaving = 5;
    private int maxIterations = 15;
    private DataSet[] samples = null;
    private GESThread[] gesThreads = null;
    private Thread[] threads = null;
    private ArrayList<TupleNode>[] subSets = null;
    private ArrayList<Dag> graphs = null;
    private Graph currentGraph = null;
    private Graph previousGraph = null;
    private Scorer scorer = null;
    private int it = 1;

    private TupleNode[] listOfArcs;



    private long totalTimeIterations;

    // We need to use the union fusion.
    private String fusionConsensus = "HeuristicConsensusMVoting";
    private String net_path = null;
    private String bbdd_path = null;
    private String net_name = null;
    private String bbdd_name = null;
    private MlBayesIm bn2 = null;
    private FileWriter csvWriter_iters;
    private FileWriter csvWriter_global;


    private ArrayList<Long> times_iterations = new ArrayList<>();
    private ArrayList<Long> times_fusion = new ArrayList<>();
    private ArrayList<Long> times_delta = new ArrayList<>();
    private ArrayList<Double> scores_threads = new ArrayList<>();
    private ArrayList<Double> scores_fusion = new ArrayList<>();
    private ArrayList<Double> scores_delta = new ArrayList<>();


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
        this.data = readData(path);
        initialize(nThreads);
    }

    /**
     * Stores the data from a csv as a DataSet object.
     * @param path
     * Path to the csv file.
     * @return DataSet containing the data from the csv file.
     */
    public static DataSet readData(String path){
        // Initial Configuration
        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);
        DataSet dataSet = null;
        // Reading data
        try {
            dataSet = reader.parseTabular(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataSet;
    }

    /**
     * Initializes the general parameters of the class.
     * @param nThreads number of threads used in the problem.
     */
    @SuppressWarnings("unchecked")
    private void initialize(int nThreads){
        this.nThreads = nThreads;
        this.samples = new DataSet[this.nThreads];
        this.gesThreads = new ThFES[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList[this.nThreads];
        // Number of arcs is n*(n-1)/2
        this.listOfArcs = new TupleNode[this.data.getNumColumns() * (this.data.getNumColumns() -1) / 2];
    }

    /**
     * Calculates the amount of possible arcs between the variables of the dataset and stores it.
     */
    public void calculateArcs(){
        //1. Get edges (variables)
        List<Node> variables = data.getVariables();
        int index = 0;
        //2. Iterate over variables and save pairs
        for(int i=0; i<data.getNumColumns()-1; i++){
            for(int j=i+1; j<data.getNumColumns(); j++){
                // Getting pair of variables
                Node var_A = variables.get(i);
                Node var_B = variables.get(j);
                //3. Storing both pairs
                // Maybe we can use Edge object
                this.listOfArcs[index] = new TupleNode(var_A,var_B);
                index++;
                //this.listOfArcs[index] = new TupleNode(var_B,var_A);
                //index++;
            }
        }
    }

    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem
     */
    public void splitArcs(){
        // Shuffling arcs
        List<TupleNode> shuffledArcs = Arrays.asList(listOfArcs);
        Random random = new Random(seed);
        Collections.shuffle(shuffledArcs, random);

        // Splitting Arcs into subsets
        int n = 0;
        for(int s = 0; s< subSets.length-1; s++){
            ArrayList<TupleNode> sub = new ArrayList<>();
            for(int i = 0; i < Math.floorDiv(shuffledArcs.size(),this.nThreads) ; i++){
                sub.add(shuffledArcs.get(n));
                n++;
            }
            this.subSets[s] = sub;
        }

        // Adding leftovers
        ArrayList<TupleNode> sub = new ArrayList<>();
        for(int i = n; i < shuffledArcs.size(); i++ ){
            sub.add(shuffledArcs.get(i));
        }
        this.subSets[this.subSets.length-1] = sub;

        // Debugging
        for (int i = 0; i < subSets.length; i++){
            System.out.println("Subset " + i);
            for( TupleNode tuple : subSets[i]){
                System.out.print(tuple + ", ");
            }
            System.out.print("\n");
        }

    }

    /**
     * Transforms a graph to a DAG, and removes any possible inconsistency found throughout its stucture.
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

        // Creating ThFES runnables
        if (this.currentGraph == null) {
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new ThFES(this.data, this.subSets[i], this.nFESItInterleaving);
            }
        }
        else{
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new ThFES(this.data,this.currentGraph, this.subSets[i], this.nFESItInterleaving);
            }
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            //Graph g = this.search[i].search();
            this.gesThreads[i].resetFlag(); 				// Reseting flag search
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }

    private void runThreads() throws InterruptedException {
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

            System.out.println("Graph of Thread " + i + ": \n" + gdag);

        }


    }

    /**
     * Runs the FES Stage, where threads run a FES algorithm for each subset of edges.
     * @throws InterruptedException Exception caused by interruction.
     */
    public void fesStage() throws InterruptedException {

        // Configuring the fes stage
        fesConfig();

        // Running threads
        runThreads();

    }

    private void besConfig(){
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();
        this.gesThreads = new GESThread[this.nThreads];

        for (int i = 0; i < this.nThreads; i++) {
            this.gesThreads[i] = new ThBES(this.data, this.currentGraph, this.subSets[i], this.nFESItInterleaving);
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            //Graph g = this.search[i].search();
            this.gesThreads[i].resetFlag(); 				// Reseting flag search
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }

    public void besStage() throws InterruptedException {
        // Configuring the fes stage
        besConfig();

        // Running threads
        runThreads();

    }


    /**
     * Joins the Dags of the FES and BES stages.
     * @return Dag with the fusion consensus of the graphs of the previous stage
     */
    public Dag fusion(){
        ConsensusUnion fusion = new ConsensusUnion(this.graphs);
        this.currentGraph = fusion.union();
        return (Dag) this.currentGraph;
    }


    /**
     * Main steps of the algorithm
     */
    public void search(){
        // 1. Calculating Edges
        calculateArcs();
        splitArcs();

        // 2. FES
        try {
            fesStage();
        } catch (InterruptedException e) {
            System.err.println("Error in FES Stage");
            e.printStackTrace();
        }

        // 3. Fusion
        fusion();
        System.out.println(this.currentGraph);

        // 4. BES
        try{
            besStage();
        } catch (InterruptedException e){
            System.err.println("Error in BES Stage");
            e.printStackTrace();
        }
        System.out.println("Results of BES: ");
        for(Dag dag : this.graphs){
            System.out.println(dag);
        }

        // 5. Fusion

        // Iterate
    }


    //*********** SETTERS AND GETTERS *************

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public long getSeed(){
        return this.seed;
    }

    public TupleNode[] getListOfArcs() {
        return listOfArcs;
    }

    public ArrayList<TupleNode>[] getSubSets() {
        return subSets;
    }

    public DataSet getData() {
        return data;
    }


    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public ArrayList<Dag> getGraphs(){
        return this.graphs;
    }


    public static void main(String[] args){
        // 1. Read Data
        String path = "src/test/resources/cancer.xbif_.csv";
        int maxIteration = 15;
        Main main = new Main(path, 2);
        main.setMaxIterations(maxIteration);

        // Running Algorithm
        main.search();

    }

}
