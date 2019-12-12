package org.albacete.simd;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.pGES.Scorer;
import org.albacete.simd.pGES.ThGES;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Main class. This class contains the methods and variables used to run the parallel BN algorithm
 */
@SuppressWarnings("SpellCheckingInspection")
public class Main
{
    private final DataSet data;
    private int nThreads = 1;
    private long seed = 42;
    private int nItInterleaving = 0;
    private int maxIterations = 15;
    private DataSet[] samples = null;
    private ThGES[] search = null;
    private Thread[] threads = null;
    private ArrayList<TupleNode>[] subSets = null;
    private ArrayList<Dag> graphs = null;
    private Graph currentGraph = null;
    private Graph previousGraph = null;
    private Scorer scorer = null;
    private int it = 1;

    private TupleNode[] listOfArcs;



    private long totalTimeIterations;


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
    public DataSet readData(String path){
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
        this.search = new ThGES[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList[this.nThreads];
        // Number of arcs is n*(n-1)/2
        this.listOfArcs = new TupleNode[this.data.getNumColumns() * (this.data.getNumColumns() -1)];
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
                this.listOfArcs[index] = new TupleNode(var_B,var_A);
                index++;
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

    public static void main(String[] args){
        // 1. Read Data
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 2);

        // 2. Split Arcs
        main.calculateArcs();
        main.splitArcs();

        // Print
    }

}
