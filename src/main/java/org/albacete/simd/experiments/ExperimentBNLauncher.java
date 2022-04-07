package org.albacete.simd.experiments;

import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.clustering.RandomClustering;
import org.albacete.simd.framework.BNBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentBNLauncher {

    /*
    public static final String[] algorithmNames = {"ges", "pges", "hc", "phc", "pfhcbes"};
    public static final String[] netNames = {"alarm"};//, "andes", "barley", "cancer", "child", "earthquake", "hailfinder",
            //"hepar2", "insurance", "link", "mildew", "munin", "pigs", "water", "win95pts"};
    public static final String[] bbddEndings = {".xbif_.csv", ".xbif50001_.csv", ".xbif50002_.csv", ".xbif50003_.csv",
            ".xbif50004_.csv", ".xbif50005_.csv", ".xbif50006_.csv", ".xbif50007_.csv", ".xbif50008_.csv",
            ".xbif50009_.csv", ".xbif50001246_.csv"};
    public static final int [] interleavings = {5, 10, 15};
    public static final int [] seeds = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29};
    public static Map<String, String> networkPaths = new HashMap<>();
    //public static Map<String, List<String>> bbddPaths = new HashMap<>();
    public static Map<String, String> testPaths = new HashMap<>();
    public static Map<Integer, List<Object>> parameters = new HashMap<>();
    */

    /*
    public static final String[] bbddEndings = {".xbif_.csv", ".xbif50001_.csv", ".xbif50002_.csv", ".xbif50003_.csv",
            ".xbif50004_.csv", ".xbif50005_.csv", ".xbif50006_.csv", ".xbif50007_.csv", ".xbif50008_.csv",
            ".xbif50009_.csv", ".xbif50001246_.csv"};
    */
    public static final int MAXITERATIONS = 100;
    //public static final String PARAMS_FILE = "/res/params/hyperparams.txt";

    public static void main(String[] args) throws Exception {
        // Reading arguments
        int index = Integer.parseInt(args[0]);
        String paramsFile = args[1];

        // Reading parameters
        List<Object> parameters = readParameters(paramsFile, index);

        // Running Experiment
        runExperiment(parameters);
    }


    public static List<Object> readParameters(String paramsFile, int index) throws Exception {
        String params [];
        List<Object> parameters = null;
        try (BufferedReader br = new BufferedReader(new FileReader(paramsFile))) {
            //Reading index line
            String line;
            for (int i = 0; i < index; i++)
                br.readLine();
            line = br.readLine();

            // Printing params
            System.out.println(line);

            //Splitting params
            params = line.split(" ");

            //Getting params from line
            String alg = params[0];
            switch(alg) {
                case "ges":
                    return readParametersControl(params);
                case "pges":
                    return readParametersPGES(params);
                default:
                    throw new Exception("Algorithm not PGES or GES...\n value of alg: " + alg);
            }
        }
        catch(FileNotFoundException e){
          System.out.println(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(parameters == null)
            throw new Exception("Parameters not read! Index: "  + index + "ParamsFile: " + paramsFile);

        return parameters;
    }

    public static List<Object> readParametersControl(String [] params){
        String alg = params[0];
        String netName = params[1];
        String netPath = params[2];
        String bbddPath = params[3];
        String testPath = params[4];

        List<Object> parameters = new ArrayList<>();
        parameters.add(alg);
        parameters.add(netName);
        parameters.add(netPath);
        parameters.add(bbddPath);
        parameters.add(testPath);

        return parameters;

    }

    public static List<Object> readParametersPGES(String [] params){
        String alg = params[0];
        String netName = params[1];
        String netPath = params[2];
        String bbddPath = params[3];
        String testPath = params[4];
        //int threads = Integer.parseInt(params[5]);
        int interleaving = Integer.parseInt(params[5]);
        int seed = Integer.parseInt(params[6]);

        //Adding params into an ArrayList
        List<Object> parameters = new ArrayList<>();
        parameters.add(alg);
        parameters.add(netName);
        parameters.add(netPath);
        parameters.add(bbddPath);
        parameters.add(testPath);
        //parameters.add(threads);
        parameters.add(interleaving);
        parameters.add(seed);

        return parameters;

    }

    public static void runExperiment(List<Object> parameters) throws Exception {
        BNBuilder experimentAlgorithm;
        String alg = (String) parameters.get(0);
        String netName, netPath, bbddPath, testPath;
        int nThreads, nInterleaving, seed;
        ExperimentBNBuilder experiment;
        Clustering clustering;
        switch (alg){
            case "pges":
                netName = (String) parameters.get(1);
                netPath = (String) parameters.get(2);
                bbddPath = (String) parameters.get(3);
                testPath = (String) parameters.get(4);
                nThreads = Runtime.getRuntime().availableProcessors();
                nInterleaving = (Integer) parameters.get(5);
                seed = (Integer) parameters.get(6);
                clustering = new RandomClustering(seed);
                experimentAlgorithm = new PGESwithStages(bbddPath, clustering, nThreads, MAXITERATIONS, nInterleaving);
                experiment = new ExperimentBNBuilder(experimentAlgorithm, netName, netPath, bbddPath, testPath, seed);
                break;
            case "hpges":
                netName = (String) parameters.get(1);
                netPath = (String) parameters.get(2);
                bbddPath = (String) parameters.get(3);
                testPath = (String) parameters.get(4);
                nThreads = Runtime.getRuntime().availableProcessors();
                nInterleaving = (Integer) parameters.get(5);
                //int seed = (Integer) parameters.get(6);
                clustering = new HierarchicalClustering();
                experimentAlgorithm = new PGESwithStages(bbddPath, clustering, nThreads, MAXITERATIONS, nInterleaving);
                experiment = new ExperimentBNBuilder(experimentAlgorithm, netName, netPath, bbddPath, testPath);
                break;
            case "ges":
                netName = (String) parameters.get(1);
                netPath = (String) parameters.get(2);
                bbddPath = (String) parameters.get(3);
                testPath = (String) parameters.get(4);
                experimentAlgorithm = new GES_BNBuilder(bbddPath);
                experiment = new ExperimentBNBuilder(experimentAlgorithm, netName, netPath, bbddPath, testPath);
                break;
            default:
                throw new Exception("Error... Algoritmo incorrecto: " + alg);
        }


        // Running experiment
        experiment.runExperiment();

        String results = experiment.getResults();

        String EXPERIMENTS_FOLDER = "/results/";
        String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + netName + "_" + experiment.bbdd_name + "_t" + experiment.nThreads +
                "_i" + experiment.nItInterleaving + "_s" + experiment.seed + ".csv";
        try {
            //Experiment.saveExperiment(savePath, resultsGES);
            Experiment.saveExperiment(savePath, results);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving results at: " + savePath);
        }

    }
}
