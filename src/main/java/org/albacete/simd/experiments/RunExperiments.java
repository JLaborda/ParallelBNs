
package org.albacete.simd.experiments;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunExperiments {

    public static final String NET_FOLDER = "./res/networks/";
    public static final String BBDD_FOLDER = "./res/networks/BBDD/";
    public static final String TEST_FOLDER = "./res/networks/BBDD/tests/";
    // Redes pequeñas - medianas (20-50 nodos)
    public static final String[] NET_NAMES = {"alarm", "child", "insurance", "water", "hailfinder", "hepar2", "win95pts", "link", "pigs"};/*,
            {"alarm","andes", "barley", "cancer", "child", "earthquake",
    "hailfinder", "hepar2", "insurance", "link", "mildew", "munin", "pigs", "water", "win95pts"};*/
    // barley da problemas (OutOfMemoryError)
    public static final String EXPERIMENTS_FOLDER = "./experiments/MACJorge/";


    public static String createNetworkPath(String net_name){
        // Adding net paths
        return NET_FOLDER + net_name + ".xbif";

    }


    public static List<String>  createBBDDPaths(String net_name){
        List<String> bbdd_paths = new ArrayList<>();
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50001_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50002_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50003_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50004_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50005_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50006_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50007_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50008_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50009_.csv");
        bbdd_paths.add(BBDD_FOLDER + net_name + ".xbif50001246_.csv");
        return bbdd_paths;

    }

    public static String createTestPath(String net_name){
        return TEST_FOLDER + net_name + "_test.csv";
    }



    public static void main(String[] args) throws FileNotFoundException {
        try {
            String savePathBase = EXPERIMENTS_FOLDER  + "total/experiments_";
            // Initial variables
            int[] threads = {1, 2, 4, 6, 8};
            int maxIterations = 100;
            int[] interleavings = {5, 10, 15};
            int[] seeds = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29};

            //StringBuilder result = new StringBuilder(header);
            Experiment experiment;
            for (String netName : NET_NAMES){
                String savePath = savePathBase + netName + ".csv";
                File file = new File(savePath);
                FileWriter csvWriter = new FileWriter(file,true);
                if(file.length() == 0) {
                    String header = "algorithm, network, bbdd, thread, interleaving, seed, SHD, LL Score, BDeu Score, dfMM, dfMM plus, Total iterations, Total time(s)\n";
                    csvWriter.append(header);
                }
                // Creating Experiments
                //List<Experiment> experiments = new ArrayList<Experiment>();
                String netPath = createNetworkPath(netName);
                List<String> bbddPaths = createBBDDPaths(netName);
                String testPath = createTestPath(netName);
                for (String bbddPath : bbddPaths){
                    // GES
                    experiment = new ExperimentGES(netPath, bbddPath, testPath);
                    experiment.runExperiment();
                    experiment.printResults();
                    csvWriter.append(experiment.getResults());
                    csvWriter.flush();
                    for(Integer interleaving : interleavings){

                        //HC
                        experiment = new ExperimentHC(netPath, bbddPath, testPath, maxIterations, interleaving);
                        experiment.runExperiment();
                        experiment.printResults();
                        csvWriter.append(experiment.getResults());
                        csvWriter.flush();


                        for (Integer thread : threads){
                            for (Integer seed: seeds){
                                //PFHCBES
                                experiment = new ExperimentPFHCBES(netPath, bbddPath, testPath, thread, maxIterations, interleaving, seed);
                                experiment.runExperiment();
                                experiment.printResults();
                                csvWriter.append(experiment.getResults());
                                csvWriter.flush();

                                //PGES
                                experiment = new ExperimentPGES(netPath, bbddPath, testPath, thread, maxIterations, interleaving, seed);
                                experiment.runExperiment();
                                experiment.printResults();
                                csvWriter.append(experiment.getResults());
                                //PHC
                                experiment = new ExperimentPHC(netPath, bbddPath, testPath, thread, maxIterations, interleaving, seed);
                                experiment.runExperiment();
                                experiment.printResults();
                                csvWriter.append(experiment.getResults());
                                csvWriter.flush();


                            }
                        }
                    }
                }
                //Saving and closing writer
                csvWriter.flush();
                csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
