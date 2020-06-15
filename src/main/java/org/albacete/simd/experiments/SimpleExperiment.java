package org.albacete.simd.experiments;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class SimpleExperiment {

    public static void main(String[] args) throws FileNotFoundException {
        String fileName = "outputs/output_cancer_ges.txt";
        PrintStream fileStream = new PrintStream(fileName);
        System.setOut(fileStream);
        Experiment experiment = new ExperimentGES("res/networks/cancer.xbif", "res/networks/BBDD/cancer.xbif_.csv");
        experiment.runExperiment();
        experiment.printResults();
    }
}
