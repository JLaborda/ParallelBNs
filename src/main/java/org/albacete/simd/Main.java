package org.albacete.simd;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
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
    final DataSet data;
    int nThreads = 1;
    int nItInterleaving = 0;
    int maxIterations = 15;
    DataSet[] samples = null;
    ThGES[] search = null;
    Thread[] threads = null;
    ArrayList<Node>[] subSets = null;
    ArrayList<Dag> graphs = null;
    Graph currentGraph = null;
    Graph previousGraph = null;
    Scorer scorer = null;
    int it = 1;

    TupleNode[] listOfArcs;



    long totalTimeIterations;


    String fusionConsensus = "HeuristicConsensusMVoting";
    String net_path = null;
    String bbdd_path = null;
    String net_name = null;
    String bbdd_name = null;
    MlBayesIm bn2 = null;
    FileWriter csvWriter_iters;
    FileWriter csvWriter_global;


    ArrayList<Long> times_iterations = new ArrayList<>();
    ArrayList<Long> times_fusion = new ArrayList<>();
    ArrayList<Long> times_delta = new ArrayList<>();
    ArrayList<Double> scores_threads = new ArrayList<>();
    ArrayList<Double> scores_fusion = new ArrayList<>();
    ArrayList<Double> scores_delta = new ArrayList<>();


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
                //3. Storing pairs
                this.listOfArcs[index] = new TupleNode(var_A,var_B);
                index++;
            }
        }
    }

}
