package org.albacete.simd.experiments;

import java.io.IOException;

public class SimpleExperiment {

    public static void main(String[] args) throws IOException {
        String networkFolder = "res/networks/";
        Experiment experiment = new ExperimentPGES( networkFolder + "alarm.xbif",
                networkFolder + "BBDD/alarm.xbif50001_.csv",
                networkFolder + "BBDD/tests/alarm_test.csv",
                4,
                1000,
                5,
                42,
                4
                );
        experiment.runExperiment();
        experiment.printResults();
        String results = experiment.getResults();
        //String savePath = "/home/jdls/developer/projects/ParallelBNs/results/prueba.txt";
        //Experiment.saveExperiment(savePath, results);
    }
}
