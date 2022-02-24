package org.albacete.simd.experiments;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.algorithms.bnbuilders.HillClimbingSearch;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.utils.Utils;

import java.io.IOException;

public class SimpleBNExperiment {


    public static void main(String[] args){
        // 1. Configuration
        String networkFolder = "./res/networks/";
        String net_name = "alarm";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50001_.csv";
        DataSet ds = Utils.readData(bbdd_path);
        String test_path = networkFolder + "BBDD/tests/" + net_name + "_test.csv";

        // 2. Algorithm
        BNBuilder algorithm = new GES_BNBuilder(bbdd_path);//new PGESwithStages(ds, 8, 30, 5);

        // Experiment
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(algorithm, net_name, net_path, bbdd_path, test_path);//new ExperimentBNBuilder(algorithm, net_path, bbdd_path, test_path, 42);

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
