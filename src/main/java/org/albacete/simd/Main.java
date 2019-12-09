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
import java.util.ArrayList;

/**
 * Hello world!
 *
 */
public class Main
{
    DataSet data;
    int nThreads = 1;
    int nItInterleaving = 0;
    int maxIterations = 15;
    DataSet[] samples = null;
    ThGES[] search = null;
    Thread[] threads = null;
    ArrayList[] subSets = null;
    ArrayList<Dag> graphs = null;
    Graph currentGraph = null;
    Graph previousGraph = null;
    Scorer scorer = null;
    int it = 1;


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




    public Main(DataSet data, int nThreads){
        this.data = data;
        initialize(nThreads);
    }

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

    private void initialize(int nThreads){
        this.nThreads = nThreads;
        this.samples = new DataSet[this.nThreads];
        this.search = new ThGES[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList[this.nThreads];
    }

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
    }
}
