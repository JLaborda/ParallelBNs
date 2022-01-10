package org.albacete.simd.experiments;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.algorithms.bnbuilders.HillClimbingSearch;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.utils.Utils;

import java.io.IOException;

public class SimpleBNExperiment {


    public static void main(String[] args){
        // 1. Configuration
        String networkFolder = "res/networks/";
        String net_path = networkFolder + "munin.xbif";
        String bbdd_path = networkFolder + "BBDD/munin.xbif50001_.csv";
        DataSet ds = Utils.readData(bbdd_path);
        String test_path = networkFolder + "BBDD/tests/munin_test.csv";

        // 2. Algorithm
        BNBuilder algorithm = new PGESwithStages(ds, 8, 30, 5);

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
