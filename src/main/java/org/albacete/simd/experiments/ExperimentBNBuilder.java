package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.ParallelFHCBES;
import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.algorithms.bnbuilders.HillClimbingSearch;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.algorithms.bnbuilders.PHC_BNBuilder;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.clustering.RandomClustering;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.albacete.simd.algorithms.bnbuilders.Circular_GES;

/*We are checking the following hyperparameters:
 * Threads: [1, 2, 4, 8, 16]
 * Interleaving: [5,10,15]
 *
 * We are going to experiment over */


public class ExperimentBNBuilder {

    protected BNBuilder algorithm;
    protected String netPath;
    protected String databasePath;
    protected String netName;
    protected String databaseName;
    protected String testDatabasePath;
    protected DataSet testDataset;

    protected int numberOfThreads;
    protected int interleaving;
    protected int maxIterations;
    //protected static HashMap<String, HashMap<String,String>> map;

    protected int structuralHamiltonDistanceValue = Integer.MAX_VALUE;
    protected double bdeuScore;
    protected double [] differencesOfMalkovsBlanket;
    private long startTime;
    private long endTime;
    protected long elapsedTime;
    protected int numberOfIterations;
    protected double LogLikelihoodScore;


    protected String log = "";
    protected String algName;
    protected long seed = -1;
    private MlBayesIm controlBayesianNetwork;
    private Dag resultingBayesianNetwork;


    public ExperimentBNBuilder(String[] parameters) throws Exception {
        extractParametersForClusterExperiment(parameters);
        createBNBuilder();
    }

    private void extractParametersForClusterExperiment(String[] parameters){
        algName = parameters[0];
        netName = parameters[1];
        netPath = parameters[2];
        databasePath = parameters[3];
        databaseName = getDatabaseNameFromPattern();
        testDatabasePath = parameters[4];

        if(!algName.equals("ges")) {
            numberOfThreads = Runtime.getRuntime().availableProcessors();
            interleaving = Integer.parseInt(parameters[5]);
            seed = Integer.parseInt(parameters[6]);
        }
    }

    private String getDatabaseNameFromPattern(){
        // Matching the end of the csv file to get the name of the database
        Pattern pattern = Pattern.compile(".*/(.*).csv");
        Matcher matcher = pattern.matcher(this.databasePath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void createBNBuilder() throws Exception {
        switch(algName) {
            case "pges":
                Clustering randomClustering = new RandomClustering(seed);
                algorithm = new PGESwithStages(databasePath, randomClustering, numberOfThreads, ExperimentBNLauncher.MAXITERATIONS, interleaving);
                break;
            case "pges_clustering":
                Clustering hierarchicalClusteringPGES = new HierarchicalClustering();
                algorithm = new PGESwithStages(databasePath, hierarchicalClusteringPGES, numberOfThreads, ExperimentBNLauncher.MAXITERATIONS, interleaving);
                break;
            case "ges":
                algorithm = new GES_BNBuilder(databasePath);
                break;
            case "circular_ges":
                Clustering hierarchicalClusteringGES = new HierarchicalClustering();
                algorithm = new Circular_GES(databasePath, hierarchicalClusteringGES, numberOfThreads);
                break;
            default:
                throw new Exception("Error... Algoritmo incorrecto: " + algName);
        }
    }

    public ExperimentBNBuilder(BNBuilder algorithm, String netName, String netPath, String bbddPath, String testDatabasePath) {
        this.algorithm = algorithm;
        this.netName = netName;
        this.netPath = netPath;
        this.databasePath = bbddPath;
        this.testDatabasePath = testDatabasePath;
        this.testDataset = Utils.readData(testDatabasePath);
        this.algName = algorithm.getClass().getSimpleName();

        Pattern pattern = Pattern.compile(".*/(.*).csv");
        Matcher matcher = pattern.matcher(this.databasePath);
        if (matcher.find()) {
            databaseName = matcher.group(1);
        }
        this.numberOfThreads = algorithm.getnThreads();
        this.maxIterations = algorithm.getMaxIterations();
        this.interleaving = algorithm.getItInterleaving();
    }

    public ExperimentBNBuilder(BNBuilder algorithm, String netName, String netPath, String bbddPath, String testDatabasePath, long partition_seed) {
        this(algorithm, netName, netPath, bbddPath, testDatabasePath);
        this.seed = partition_seed;
        Utils.setSeed(partition_seed);
    }


    public void runExperiment()
    {
        try {
            printExperimentInformation();

            controlBayesianNetwork = readOriginalBayesianNetwork();

            // Search is executed
            this.algorithm.search();
            resultingBayesianNetwork =  this.algorithm.getCurrentDag();

            // Metrics
            calcuateMeasurements(controlBayesianNetwork);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printExperimentInformation() {
        System.out.println("Starting Experiment:");
        System.out.println("-----------------------------------------");
        System.out.println("\tNet Name: " + netName);
        System.out.println("\tBBDD Name: " + databaseName);
        //System.out.println("\tFusion Consensus: " + fusion_consensus);
        System.out.println("\tnThreads: " + numberOfThreads);
        System.out.println("\tnItInterleaving: " + interleaving);
        System.out.println("-----------------------------------------");

        System.out.println("Net_path: " + netPath);
        System.out.println("BBDD_path: " + databasePath);
    }

    private MlBayesIm readOriginalBayesianNetwork() throws Exception {
        // Starting timer
        startTime = System.currentTimeMillis();

        BIFReader bayesianReader = new BIFReader();
        bayesianReader.processFile(this.netPath);
        BayesNet bayesianNet = bayesianReader;
        System.out.println("Numero de variables: " + bayesianNet.getNrOfNodes());

        //Transforming the BayesNet into a BayesPm
        BayesPm bayesPm = Utils.transformBayesNetToBayesPm(bayesianNet);
        MlBayesIm bn2 = new MlBayesIm(bayesPm);

        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);
        return bn2;
    }

    private void calcuateMeasurements(MlBayesIm controlBayesianNetwork) {
        // Ending timer
        endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.structuralHamiltonDistanceValue = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
        this.differencesOfMalkovsBlanket = Utils.avgMarkovBlanquetdif(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
        this.numberOfIterations = algorithm.getIterations();
        this.bdeuScore = GESThread.scoreGraph(algorithm.getCurrentDag(), algorithm.getProblem());
        this.LogLikelihoodScore = Utils.LL(algorithm.getCurrentDag(), testDataset);
    }

    public void printResults() {
        System.out.println(this);
        System.out.println("Resulting DAG:");
        System.out.println(algorithm.getCurrentGraph());
        System.out.println("Total Nodes of Resulting DAG");
        System.out.println(algorithm.getCurrentGraph().getNodes().size());
        System.out.println("-------------------------\nMetrics: ");
        System.out.println("SHD: "+ structuralHamiltonDistanceValue);
        System.out.println("LLScore: " + this.LogLikelihoodScore);
        System.out.println("Final BDeu: " +this.bdeuScore);
        System.out.println("Total execution time (s): " + (double) elapsedTime/1000);
        System.out.println("Total number of Iterations: " + this.numberOfIterations);
        System.out.println("differencesOfMalkovsBlanket avg: "+ differencesOfMalkovsBlanket[0]);
        System.out.println("differencesOfMalkovsBlanket plus: "+ differencesOfMalkovsBlanket[1]);
        System.out.println("differencesOfMalkovsBlanket minus: "+ differencesOfMalkovsBlanket[2]);
    }

    public void saveExperiment(String savePath) throws IOException{
        File file = new File(savePath);
        BufferedWriter csvWriter = new BufferedWriter(new FileWriter(savePath, true));
        //FileWriter csvWriter = new FileWriter(savePath, true);
        if(file.length() == 0) {
            String header = "algorithm, network, bbdd, threads, interleaving, seed, SHD, LL Score, BDeu Score, dfMM, dfMM plus, dfMM minus, Total iterations, Total time(s)\n";
            csvWriter.append(header);
        }
        csvWriter.append(this.getResults());

        csvWriter.flush();
        csvWriter.close();
        System.out.println("Results saved at: " + savePath);
    }

    public double[] getDifferencesOfMalkovsBlanket() {
        return differencesOfMalkovsBlanket;
    }

    public double getBdeuScore() {
        return bdeuScore;
    }

    public int getStructuralHamiltonDistanceValue() {
        return structuralHamiltonDistanceValue;
    }

    public long getElapsedTimeMiliseconds() {
        return elapsedTime;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public int getInterleaving() {
        return interleaving;
    }

    public String getAlgName() {
        return algName;
    }

    public String getResults(){
        return  this.algName + ","
                + this.netName + ","
                + this.databaseName + ","
                + this.numberOfThreads + ","
                + this.interleaving + ","
                + this.seed + ","
                + this.structuralHamiltonDistanceValue + ","
                + this.LogLikelihoodScore + ","
                + this.bdeuScore + ","
                + this.differencesOfMalkovsBlanket[0] + ","
                + this.differencesOfMalkovsBlanket[1] + ","
                + this.differencesOfMalkovsBlanket[2] + ","
                + this.numberOfIterations + ","
                + (double) elapsedTime/1000 + "\n";//this.elapsedTime + "\n";
    }

    @Override
    public String toString() {
        return "-----------------------\nExperiment " + algName + "\n-----------------------\nNet Name: " + netName + "\tDatabase: " + databaseName + "\tThreads: " + numberOfThreads + "\tInterleaving: " + interleaving + "\tMax. Iterations: " + maxIterations;
    }
}

