package org.albacete.simd.experiments;

import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
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
    public static final int MAXITERATIONS = 250;
    public static final String PARAMS_FILE = "./res/params/hyperparams.txt";

    public static void main(String[] args) {
        int index = Integer.parseInt(args[0]);
        String netName = args[1];
        
        List<Object> parameters = readParameters(netName, index);

        runExperiment(parameters, netName);

    }

    public static List<Object> readParameters(String netName, int index){
        String params [];
        try (BufferedReader br = new BufferedReader(new FileReader(PARAMS_FILE))) {
            String line;
            for (int i = 0; i < index; i++)
                br.readLine();
            line = br.readLine();
            System.out.println(line);
            params = line.split(" ");
            String ending = params[0];
            String netPath = "./res/networks/" + netName + ".xbif";
            String bbddPath = "./res/networks/BBDD/" + netName + ending;
            String testPath = "./res/networks/BBDD/tests/" + netName + "_test.csv";
            int interleaving = Integer.parseInt(params[1]);
            int seed = Integer.parseInt(params[2]);
            List<Object> parameters = new ArrayList<>();
            parameters.add(netPath);
            parameters.add(bbddPath);
            parameters.add(testPath);
            parameters.add(interleaving);
            parameters.add(seed);
            return parameters;
        }
        catch(FileNotFoundException e){
          System.out.println(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        for(String name: netNames) {
            // Setting networkPaths
            networkPaths.put(name, "./res/networks/" + name + ".xbif");

            // Setting BBDDPaths
            List<String> paths = new ArrayList<>();
            for(String ending: bbddEndings){
                paths.add("./res/networks/BBDD/" + name + ending);
            }
            bbddPaths.put(name, paths);

            // Setting test paths
            testPaths.put(name, "./res/networks/BBDD/tests/" + name + "_test.csv");


        }
        */
        return null;
    }

    public static void runExperiment(List<Object> parameters, String netName){
        String netPath = (String) parameters.get(0);
        String bbddPath = (String) parameters.get(1);
        String testPath = (String) parameters.get(2);
        int nThreads = Runtime.getRuntime().availableProcessors();
        int nInterleaving = (Integer) parameters.get(3);
        int seed = (Integer) parameters.get(4);

        //BNBuilder gesAlg = new GES_BNBuilder(bbddPath);
        BNBuilder pgesAlg = new PGESwithStages(bbddPath, nThreads, MAXITERATIONS, nInterleaving);

        //ExperimentBNBuilder experimentGES = new ExperimentBNBuilder(gesAlg, netPath, bbddPath, testPath, seed);    
        ExperimentBNBuilder experimentPGES = new ExperimentBNBuilder(pgesAlg, netPath, bbddPath, testPath, seed);  
        
        // Running experiment
        //experimentGES.runExperiment();
        experimentPGES.runExperiment();
        //experiment.saveExperiment();
        //String resultsGES = experimentGES.getResults();
        String resultsPGES = experimentPGES.getResults();
        
        String EXPERIMENTS_FOLDER = "./results/"; // BOOKMARK: EL ERROR ESTÁ AQUÍ!
        String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + netName + ".csv";
        try {
            //Experiment.saveExperiment(savePath, resultsGES);
            Experiment.saveExperiment(savePath, resultsPGES);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving results at: " + savePath);
        }

    }
}
