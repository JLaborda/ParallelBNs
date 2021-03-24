package org.albacete.simd.experiments;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class MainExperiment{

    public static final String EXPERIMENTS_FOLDER = "/parallelbns/results/cluster/";//"/home/jorlabs/projects/ParallelBNs-git/ParallelBNs/experiments/galgo/";


    // Caos con los argunmentos
    public static void main(String[] args) {
        // Reading arguments
        String netName = args[0].toLowerCase();
        String algorithmName = args[1].toLowerCase();
        String netPath = args[2];
        String bbddPath = args[3];
        String testPath = args[4];
        int nThreads = 0;
        int nInterleaving = Integer.parseInt(args[5]);
        int maxIterations = 0;
        int seed = 0;
        // HC
        if (args.length == 7){
            maxIterations = Integer.parseInt(args[6]);
        }
        // Rest of experiments
        if(args.length == 9){
            nThreads = Integer.parseInt(args[6]);
            maxIterations = Integer.parseInt(args[7]);
            seed = Integer.parseInt(args[8]);
        }

        System.out.print("PARAMS: " );
        for(String arg: args){
            System.out.print(arg + ", ");
        }
        System.out.println();

        Experiment experiment = null;
        switch (algorithmName) {
            case "ges":
                experiment = new ExperimentGES(netPath, bbddPath, testPath, nInterleaving);
                break;
            case "pges":
                experiment = new ExperimentPGES(netPath, bbddPath, testPath, nThreads, maxIterations, nInterleaving, seed);
                break;
            case "hc":
                experiment = new ExperimentHC(netPath, bbddPath, testPath, maxIterations, nInterleaving);
                break;
            case "phc":
                experiment = new ExperimentPHC(netPath, bbddPath, testPath,  nThreads, maxIterations, nInterleaving, seed);
                break;
            case "pfhcbes":
                experiment = new ExperimentPFHCBES(netPath, bbddPath, testPath,  nThreads, maxIterations, nInterleaving, seed);
                break;
            default:
                System.out.println("Experiment is not registered... Exiting program");
                System.exit(-1);
                break;
        }

        // Running experiment
        experiment.runExperiment();
        //experiment.saveExperiment();
        try{
            String results = experiment.getResults();
            String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + netName + ".csv";
            File file = new File(savePath);
            FileWriter csvWriter = new FileWriter(savePath, true);
            if(file.length() == 0) {
                String header = "algorithm, network, bbdd, thread, interleaving, seed, SHD, LL Score, BDeu Score, dfMM, dfMM plus, Total iterations, Total time(s)\n";
                csvWriter.append(header);
            }
            csvWriter.append(results);
            csvWriter.flush();
            csvWriter.close();
            System.out.println("Results saved at: " + savePath);
        }catch (IOException e) {
            e.printStackTrace();
        }

    }
}
