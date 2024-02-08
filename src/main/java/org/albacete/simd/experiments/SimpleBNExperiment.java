package org.albacete.simd.experiments;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.search.BDeuScore;
import edu.cmu.tetrad.search.Fges;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.utils.Utils;

import java.io.IOException;
import org.albacete.simd.algorithms.bnbuilders.Circular_GES;
import org.albacete.simd.algorithms.bnbuilders.Empty;
import org.albacete.simd.algorithms.bnbuilders.Fges_BNBuilder;
import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.clustering.RandomClustering;

public class SimpleBNExperiment {


    public static void main(String[] args) throws Exception{
               // 1. Configuration
        /*String net_name = "andes";
        String networkFolder = "./res/networks/" + net_name + "/";
        String datasetFolder = "./res/datasets/" + net_name + "/";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = datasetFolder  + net_name + "00.csv";
        DataSet ds = Utils.readData(bbdd_path);
        Map<String,String> paramsMap = new HashMap<>();
        paramsMap.put(ExperimentBNBuilder.KEYS[0], "cges");
        paramsMap.put(ExperimentBNBuilder.KEYS[1], net_name);
        paramsMap.put(ExperimentBNBuilder.KEYS[2], net_path);
        paramsMap.put(ExperimentBNBuilder.KEYS[3], bbdd_path);
        paramsMap.put(ExperimentBNBuilder.KEYS[4], "HierarchicalClustering");
        paramsMap.put(ExperimentBNBuilder.KEYS[5], "4");
        paramsMap.put(ExperimentBNBuilder.KEYS[6], "c2");
        paramsMap.put(ExperimentBNBuilder.KEYS[7], "BEST_BROADCASTING");
        */
        System.out.println("Starting experiment...");
        String algParmString = "algName pges";
        String netNameParamString = "netName andes";
        String clusteringNameString = "clusteringName HierarchicalClustering";
        String numberOfRealThreadsString = "numberOfRealThreads 16";
        //String convergenceString = "convergence c2";
        //String broadcastingString = "broadcasting BEST_BROADCASTING";
        String maxIterationsString = "maxIterations 10000";
        //String randomParamString = "";//"seed 103";
        String databasePathString = "databasePath /home/jorlabs/projects/ParallelBNs/res/datasets/andes/andes8.csv";
        String netPathString = "netPath /home/jorlabs/projects/ParallelBNs/res/networks/andes/andes.xbif";

        //String paramString = "algName cges netName andes clusteringName HierarchicalClustering numberOfRealThreads 8 convergence c2 broadcasting PAIR_BROADCASTING seed 103 databasePath /home/jorlabs/projects/cges/res/datasets/andes/andes08.csv netPath /home/jorlabs/projects/cges/res/networks/andes/andes.xbif";
        String paramString = algParmString + " " + netNameParamString + " " + clusteringNameString + " " + numberOfRealThreadsString + " " +  databasePathString + " " + netPathString + " " + maxIterationsString;
        String[] parameters = paramString.split(" ");

        // 2. Setting Algorithm
        //Clustering clustering = new HierarchicalClustering();
        //CGES algorithm = new CGES(ds, clustering, 4, 100000, "c2", CGES.Broadcasting.PAIR_BROADCASTING);

        //String expString = "algName pges numberOfRealThreads 16 netPath /home/jorlabs/projects/ParallelBNs/res/networks/andes/andes.xbif databasePath /home/jorlabs/projects/ParallelBNs/res/large_datasets/andes/andes_5000.csv netName andes";
        //String expString = "algName pc netPath /home/jorlabs/projects/ParallelBNs/res/networks/andes/andes.xbif databasePath /home/jorlabs/projects/ParallelBNs/res/datasets/andes/andes8.csv netName andes";
        String expString = "algName pc netName alarm netPath /home/jorlabs/projects/ParallelBNs/res/networks/alarm/alarm.xbif databasePath /home/jorlabs/projects/ParallelBNs/res/datasets/alarm/alarm2.csv netName alarm";
        String[] expParameters = expString.split(" ");
        //2. Create experiment environment
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(expParameters);

        // 4. Launch Experiment
        System.out.println("Setting verbose");
        Utils.setVerbose(true);
        System.out.println("Running experiment...");
        experiment.runExperiment();
        experiment.printResults();
        String savePath = "results/pruebas/" + experiment.getSaveFileName(1);//String savePath = "results/prueba.txt";

        // 5. Save Experiment
        //System.out.println("Number of times broadcasting fusion is used: " + CircularProcess.fusionWinCounter);
        System.out.println("Saving at: " + savePath);
        experiment.saveExperiment(savePath);

    }
}
