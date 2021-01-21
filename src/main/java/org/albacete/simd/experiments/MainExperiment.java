package org.albacete.simd.experiments;

public class MainExperiment{

    public static void main(String[] args) {
        // Reading arguments
        String algorithmName = args[0].toLowerCase();
        String netPath = args[1];
        String bbddPath = args[2];
        int nThreads = 0;
        int nInterleaving = 0;
        int maxIterations = 0;
        if(args.length > 3){
            nThreads = Integer.parseInt(args[3]);
            nInterleaving = Integer.parseInt(args[4]);
            maxIterations = Integer.parseInt(args[5]);
        }
        Experiment experiment = null;
        switch (algorithmName) {
            case "ges":
                experiment = new ExperimentGES(netPath, bbddPath);
                break;
            case "pges":
                experiment = new ExperimentPGES(netPath, bbddPath, nThreads, maxIterations, nInterleaving);
                break;
            case "hc":
                experiment = new ExperimentHC(netPath, bbddPath, nThreads, maxIterations, nInterleaving);
                break;
            case "phc":
                experiment = new ExperimentPHC(netPath, bbddPath, nThreads, maxIterations, nInterleaving);
                break;
            case "pfhcbes":
                experiment = new ExperimentPFHCBES(netPath, bbddPath, nThreads, maxIterations, nInterleaving);
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
