package org.albacete.simd.experiments;

import java.io.IOException;

public class SimpleExperiment {

    public static void main(String[] args) throws IOException {
        String networkFolder = "/home/jdls/developer/projects/ParallelBNs/res/networks/";
        Experiment experiment = new ExperimentPGES( networkFolder + "andes.xbif",
                networkFolder + "BBDD/andes.xbif50001_.csv",
                networkFolder + "BBDD/tests/andes_test.csv",
                8,
                1000,
                15,
                30
                );
        experiment.runExperiment();
        experiment.printResults();
        String results = experiment.getResults();
        String savePath = "/home/jdls/developer/projects/ParallelBNs/results/prueba.txt";
        Experiment.saveExperiment(savePath, results);
    }
}
