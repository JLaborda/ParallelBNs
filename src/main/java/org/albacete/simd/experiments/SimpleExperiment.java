package org.albacete.simd.experiments;

import java.io.FileNotFoundException;

public class SimpleExperiment {

    public static void main(String[] args) throws FileNotFoundException {
        String networkFolder = "/home/jdls/developer/projects/ParallelBNs/res/networks/";
        Experiment experiment = new ExperimentGES( networkFolder + "alarm.xbif",
                networkFolder + "BBDD/alarm.xbif50001_.csv",
                networkFolder + "BBDD/tests/alarm_test.csv");
        experiment.runExperiment();
        experiment.printResults();
        experiment.saveExperiment();
    }
}
