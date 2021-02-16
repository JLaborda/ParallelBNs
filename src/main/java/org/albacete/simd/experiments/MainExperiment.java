package org.albacete.simd.experiments;

public class MainExperiment{

    public static void main(String[] args) {
        // Reading arguments
        String algorithmName = args[0].toLowerCase();
        String netPath = args[1];
        String bbddPath = args[2];
        String testPath = args[3];
        int nThreads = 0;
        int nInterleaving = 0;
        int maxIterations = 0;
        int seed = 0;
        if(args.length > 4){
            nThreads = Integer.parseInt(args[4]);
            nInterleaving = Integer.parseInt(args[5]);
            maxIterations = Integer.parseInt(args[6]);
            seed = Integer.parseInt(args[7]);
        }
        else{
            seed = Integer.parseInt(args[4]);
        }
        Experiment experiment = null;
        switch (algorithmName) {
            case "ges":
                experiment = new ExperimentGES(netPath, bbddPath, testPath, seed);
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
        experiment.saveExperiment();
    }
}
