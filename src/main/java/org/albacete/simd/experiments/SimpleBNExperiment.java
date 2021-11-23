package org.albacete.simd.experiments;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.algorithms.bnbuilders.HillClimbingSearch;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.utils.Utils;

import java.io.IOException;

public class SimpleBNExperiment {


    public static void main(String[] args){
        // 1. Configuration
        String networkFolder = "res/networks/";
        String net_path = networkFolder + "alarm.xbif";
        String bbdd_path = networkFolder + "BBDD/alarm.xbif50001_.csv";
        DataSet ds = Utils.readData(bbdd_path);
        String test_path = networkFolder + "BBDD/tests/alarm_test.csv";

        // 2. Algorithm
        BNBuilder algorithm = new HillClimbingSearch(ds);

        // Experiment
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(algorithm, net_path, bbdd_path, test_path, 42);

        System.out.println("Alg Name: " + experiment.getAlgName());
        experiment.runExperiment();
        experiment.printResults();
        String savePath = "results/prueba.txt";
        try {
            experiment.saveExperiment(savePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
