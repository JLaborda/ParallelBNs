package org.albacete.simd.experiments;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RunExperiments {

    public static final String networksFolder = "res/networks/";
    public static final String bbddFolder = networksFolder + "BBDD/";
    public static final String endingNetwork = ".xbif";
    public static final String endingbbdd10k = "10k.csv";
    public static final String endingbbdd50k = "50k.csv";

    public static void runAllExperiments(int[] nThreads, int[] nInterleavings, HashMap<String, HashMap<String,String>> map) {
        for(int j=0; j<nThreads.length; j++) {
            int nThread = nThreads[j];
            for(int k=0; k<nInterleavings.length; k++) {
                int nInterleaving = nInterleavings[k];
                for(String key1 : map.keySet()) {
                    HashMap<String,String> aux = map.get(key1);
                    for (String net_path: aux.keySet()) {
                        System.out.println("***********************************");
                        System.out.println("***********NEW EXPERIMENT**********");
                        System.out.println("***********************************");
                        String bbdd_path = aux.get(net_path);
                        System.out.println("Net_Path: " + net_path);
                        System.out.println("BBDD_Path: " + bbdd_path);

                        // Running Experiment
                        Experiment experiment = new ExperimentPGES(net_path, bbdd_path, nThread, 15, nInterleaving);
                        experiment.runExperiment();
                        //Saving Experiment
                        experiment.saveExperiment();

                    }
                }
            }
        }
    }

    public static void runExperiments(List<Experiment> experiments){
        for(Experiment experiment : experiments){
            System.out.println(experiment);
            experiment.runExperiment();
            experiment.printResults();
            experiment.saveExperiment();
        }
    }

    public static void runAndSaveExperiment(Experiment experiment){
        System.out.println(experiment);
        experiment.runExperiment();
        experiment.printResults();
        experiment.saveExperiment();
    }

    public static void createAndRunExperiments(String[] networks, int[] threads, int[] interleavings, int maxIterations ){
        Experiment experiment;
        for(String net : networks){
            String netPath = networksFolder + net + endingNetwork;
            String bbddPath50k = bbddFolder + net + endingbbdd50k;
            String bbddPath10k = bbddFolder + net + endingbbdd10k;
            for(int interleaving: interleavings){
                for(int nThread : threads){
                    //pges
                    experiment = new ExperimentPGES(netPath, bbddPath50k, nThread, maxIterations, interleaving);
                    runAndSaveExperiment(experiment);
                    experiment = new ExperimentPGES(netPath, bbddPath10k, nThread, maxIterations, interleaving);
                    runAndSaveExperiment(experiment);

                    //phc
                    experiment = new ExperimentPHC(netPath, bbddPath50k, nThread, maxIterations, interleaving);
                    runAndSaveExperiment(experiment);
                    experiment = new ExperimentPHC(netPath, bbddPath10k, nThread, maxIterations, interleaving);
                    runAndSaveExperiment(experiment);

                    //pfhcbes
                    experiment = new ExperimentPFHCBES(netPath, bbddPath50k, nThread, maxIterations, interleaving);
                    runAndSaveExperiment(experiment);
                    experiment = new ExperimentPFHCBES(netPath, bbddPath10k, nThread, maxIterations, interleaving);
                    runAndSaveExperiment(experiment);
                }

                // hc
                experiment = new ExperimentHC(netPath, bbddPath50k, 0, maxIterations, interleaving);
                runAndSaveExperiment(experiment);
                experiment = new ExperimentHC(netPath, bbddPath10k, 0, maxIterations, interleaving);
                runAndSaveExperiment(experiment);
            }
            //ges
            experiment = new ExperimentGES(netPath, bbddPath50k);
            runAndSaveExperiment(experiment);
            experiment = new ExperimentGES(netPath, bbddPath10k);
            runAndSaveExperiment(experiment);
        }

    }


    public static void main(String[] args) throws FileNotFoundException {

        String net_name = "alarm";
        String fileName = "outputs/output_experiments_" + net_name + ".txt";
        PrintStream fileStream = new PrintStream(fileName);
        System.setOut(fileStream);


        //String[] nets = new String[]{"cancer", "earthquake",
        //"alarm", "barley", "child", "insurance", "mildew", "water"};
        String[] nets = new String[]{net_name};
        int[] threads = new int[]{2, 4, 6, 8};
        int[] interleavings = new int[]{5, 10, 15};
        int maxIt = 25;

        // Creating Experiments
        //List<Experiment> experimentList = createExperiments(nets, threads, interleavings, maxIt);

        // Running Experiments
        // runExperiments(experimentList);

        // Running Experiments
        createAndRunExperiments(nets, threads, interleavings, maxIt);

        Toolkit.getDefaultToolkit().beep();

    }
}
