package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.ParallelFHCBES;
import org.albacete.simd.algorithms.bnbuilders.HillClimbingSearch;
import org.albacete.simd.algorithms.ParallelHillClimbingSearch;
import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.algorithms.bnbuilders.PHC_BNBuilder;
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

/*We are checking the following hyperparameters:
 * Threads: [1, 2, 4, 8, 16]
 * Interleaving: [5,10,15]
 *
 * We are going to experiment over */


public class ExperimentBNBuilder {

    protected BNBuilder algorithm;
    protected String net_path;
    protected String bbdd_path;
    protected String net_name;
    protected String bbdd_name;
    protected String test_path;
    protected DataSet test_dataset;

    protected int nThreads;
    protected int nItInterleaving;
    protected int maxIterations;
    //protected static HashMap<String, HashMap<String,String>> map;

    protected int shd = Integer.MAX_VALUE;
    protected double score;
    protected double [] dfmm;
    protected long elapsedTime;
    protected int nIterations;
    protected double LLscore;


    protected String log = "";
    protected String algName;
    protected long seed = -1;

    public ExperimentBNBuilder(BNBuilder algorithm, String net_name, String net_path, String bbdd_path, String test_path) {
        this.algorithm = algorithm;
        this.net_name = net_name;
        this.net_path = net_path;
        this.bbdd_path = bbdd_path;
        this.test_path = test_path;
        this.test_dataset = Utils.readData(test_path);
        this.algName = algorithm.getClass().getSimpleName();

        Pattern pattern = Pattern.compile(".*/(.*).csv");
        Matcher matcher = pattern.matcher(this.bbdd_path);
        if (matcher.find()) {
            //System.out.println("Match!");
            //System.out.println(matcher.group(1));
            bbdd_name = matcher.group(1);
        }


        this.nThreads = algorithm.getnThreads();
        this.maxIterations = algorithm.getMaxIterations();
        this.nItInterleaving = algorithm.getItInterleaving();
    }

    public ExperimentBNBuilder(BNBuilder algorithm, String net_name, String net_path, String bbdd_path, String test_path, long partition_seed) {
        this(algorithm, net_name, net_path, bbdd_path, test_path);
        this.seed = partition_seed;
        Utils.setSeed(partition_seed);
    }


    public void runExperiment()
    {
        try {
            System.out.println("Starting Experiment:");
            System.out.println("-----------------------------------------");
            System.out.println("\tNet Name: " + net_name);
            System.out.println("\tBBDD Name: " + bbdd_name);
            //System.out.println("\tFusion Consensus: " + fusion_consensus);
            System.out.println("\tnThreads: " + nThreads);
            System.out.println("\tnItInterleaving: " + nItInterleaving);
            System.out.println("-----------------------------------------");

            System.out.println("Net_path: " + net_path);
            System.out.println("BBDD_path: " + bbdd_path);

            long startTime = System.currentTimeMillis();
            BIFReader bf = new BIFReader();
            bf.processFile(this.net_path);
            BayesNet bn = (BayesNet) bf;
            System.out.println("Numero de variables: "+bn.getNrOfNodes());
            MlBayesIm bn2 = new MlBayesIm(bn);
            DataReader reader = new DataReader();
            reader.setDelimiter(DelimiterType.COMMA);
            reader.setMaxIntegralDiscrete(100);

            // Running Experiment
            //DataSet dataSet = reader.parseTabular(new File(this.bbdd_path));
            //this.algorithm = new PGESv2(dataSet,this.nThreads);
            //this.algorithm.setMaxIterations(this.maxIterations);
            //this.algorithm.setNFESItInterleaving(this.nItInterleaving);

            // Search is executed
            this.algorithm.search();

            // Measuring time
            long endTime = System.currentTimeMillis();

            // Metrics
            this.elapsedTime = endTime - startTime;
            //System.out.println("Original DAG:");
            //System.out.println(bn2.getDag());
            //System.out.println("Total Nodes Original DAG:");
            //System.out.println(bn2.getDag().getNodes().size());

            /*
            List<Node> nodes_original = bn2.getDag().getNodes();
            List<Node> nodes_created = alg.getCurrentGraph().getNodes();

            boolean cond = true;
            for(Node node_original : nodes_original){
                if (!nodes_created.contains(node_original)){
                    cond = false;
                }
            }
            */

            // System.out.println(cond);


            //Metrics
            this.shd = Utils.SHD(bn2.getDag(),(Dag) algorithm.getCurrentGraph());
            this.dfmm = Utils.avgMarkovBlanquetdif(bn2.getDag(), (Dag) algorithm.getCurrentGraph());
            this.nIterations = algorithm.getIterations();
            this.score = GESThread.scoreGraph(algorithm.getCurrentGraph(), algorithm.getProblem());
            this.LLscore = Utils.LL((Dag)algorithm.getCurrentGraph(), test_dataset);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void printResults() {
        // Report
        System.out.println(this);
        System.out.println("Resulting DAG:");
        System.out.println(algorithm.getCurrentGraph());
        System.out.println("Total Nodes of Resulting DAG");
        System.out.println(algorithm.getCurrentGraph().getNodes().size());
        System.out.println("-------------------------\nMetrics: ");

        System.out.println("SHD: "+shd);
        System.out.println("LLScore: " + this.LLscore);
        System.out.println("Final BDeu: " +this.score);
        System.out.println("Total execution time (s): " + (double) elapsedTime/1000);
        System.out.println("Total number of Iterations: " + this.nIterations);
        System.out.println("dfMM: "+ dfmm[0]);
        System.out.println("dfMM plus: "+ dfmm[1]);
        System.out.println("dfMM minus: "+ dfmm[2]);

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

    public double[] getDfmm() {
        return dfmm;
    }

    public double getScore() {
        return score;
    }

    public int getShd() {
        return shd;
    }

    public long getElapsedTimeMiliseconds() {
        return elapsedTime;
    }

    public int getnIterations() {
        return nIterations;
    }

    public int getnItInterleaving() {
        return nItInterleaving;
    }

    public String getAlgName() {
        return algName;
    }

    public String getResults(){
        return  this.algName + ","
                + this.net_name + ","
                + this.bbdd_name + ","
                + this.nThreads + ","
                + this.nItInterleaving + ","
                + this.seed + ","
                + this.shd + ","
                + this.LLscore + ","
                + this.score + ","
                + this.dfmm[0] + ","
                + this.dfmm[1] + ","
                + this.dfmm[2] + ","
                + this.nIterations + ","
                + (double) elapsedTime/1000 + "\n";//this.elapsedTime + "\n";
    }

    @Override
    public String toString() {
        return "-----------------------\nExperiment " + algName + "\n-----------------------\nNet Name: " + net_name + "\tDatabase: " + bbdd_name + "\tThreads: " + nThreads + "\tInterleaving: " + nItInterleaving + "\tMax. Iterations: " + maxIterations;
    }

    public static void main(String[] args) {
        //System.out.println("Numero de hilos: " + Thread.getAllStackTraces().keySet().size());
        // Reading arguments
        String netName = args[0].toLowerCase();
        String algorithmName = args[1].toLowerCase();
        String netPath = args[2];
        String bbddPath = args[3];
        String testPath = args[4];
        int nThreads = 0;
        int nInterleaving = 0;
        int maxIterations = 0;
        int seed = -1;
        // HC
        if (args.length >= 7){
            nInterleaving = Integer.parseInt(args[5]);
            maxIterations = Integer.parseInt(args[6]);
        }
        // Rest of experiments
        if(args.length == 9){
            nThreads = Integer.parseInt(args[7]);
            seed = Integer.parseInt(args[8]);
        }


        System.out.println("Len PARAMS: " + args.length);

        System.out.print("PARAMS: " );
        for(String arg: args){
            System.out.print(arg + ", ");
        }
        System.out.println();
        System.out.println("VARIABLES: ");
        System.out.println(netName + ", " + algorithmName + ", " +
                netPath + ", " + bbddPath + ", " + testPath + ", " + nThreads +
                ", " + nInterleaving + ", " +  maxIterations + ", " + seed);
        System.out.println();

        ExperimentBNBuilder experiment;
        BNBuilder algorithm = null;
        switch (algorithmName) {
            case "ges":
                //experiment = new ExperimentGES(netPath, bbddPath, testPath);
                algorithm = new GES_BNBuilder(bbddPath);
                break;
            case "pges":
                algorithm = new PGESwithStages(bbddPath, nThreads, maxIterations, nInterleaving);
                break;
            case "hc":
                algorithm = new HillClimbingSearch(bbddPath);
                break;
            case "phc":
                algorithm = new PHC_BNBuilder(bbddPath,  nThreads, maxIterations, nInterleaving);
                break;
            case "pfhcbes":
                algorithm = new ParallelFHCBES(bbddPath, nThreads, maxIterations, nInterleaving);
                break;
            default:
                System.out.println("Experiment is not registered... Exiting program");
                System.exit(-1);
                break;
        }
        if(seed == -1)
            experiment = new ExperimentBNBuilder(algorithm, netName, netPath, bbddPath, testPath);
        else
            experiment = new ExperimentBNBuilder(algorithm, netName, netPath, bbddPath, testPath, seed);
        // Running experiment
        experiment.runExperiment();
        //experiment.saveExperiment();
        String results = experiment.getResults();
        String EXPERIMENTS_FOLDER = "./results/"; // BOOKMARK: EL ERROR ESTÁ AQUÍ!
        String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + netName + ".csv";
        try {
            Experiment.saveExperiment(savePath, results);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving results at: " + savePath);
        }
    }
}

