package org.albacete.simd.experiments;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class SimpleExperiment {

    public static void main(String[] args) throws FileNotFoundException {
        //String fileName = "outputs/output_cancer_ges.txt";
        //PrintStream fileStream = new PrintStream(fileName);
        //System.setOut(fileStream);
        Experiment experiment = new ExperimentPGES("res/networks/replicates/4Xalarm.xbif", "res/networks/replicates/BBDD/4Xalarm.xbif50000.csv",2, 15);
        experiment.runExperiment();
        experiment.printResults();
        experiment.saveExperiment();
    }
}
