package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Dag_n;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;


public class MainMCTSBN {

    public static void main(String[] args) {
        String networkFolder = "./res/networks/";
        String net_name = "alarm";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        String netPath = networkFolder + net_name + ".xbif";


        Problem problem = new Problem(bbdd_path);

        MCTSBN mctsbn = new MCTSBN(problem, 1000);
        long startTime = System.currentTimeMillis();
        addEndHook(mctsbn,startTime, netPath);

        Dag result = mctsbn.search();
        long endTime = System.currentTimeMillis();
        double score = GESThread.scoreGraph(result, problem);

        System.out.println("MCTSBN FINISHED!");
        //System.out.println("Total time: " + (endTime - startTime)*1.0 / 1000);
        //System.out.println("Score: " + score);
        //System.out.println("Best Order");
        //System.out.println(toStringOrder(mctsbn.getBestOrder()));
        //System.out.println("Best Dag: ");
        //System.out.println(result);
    }


    public static void addEndHook(MCTSBN mctsbn, long startTime, String netPath){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run(){
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                long endTime = System.currentTimeMillis();
                //save data here
                System.out.println("\n\n-------------------------------------");
                System.out.println("User shutdown...");
                System.out.println("-------------------------------------");
                System.out.println("Total time: " + (endTime - startTime)*1.0 / 1000);
                System.out.println("Score: " + mctsbn.getBestScore());
                System.out.println("Best Order");
                System.out.println(mctsbn.getBestOrder());
                System.out.println("Best Dag: ");
                System.out.println(new Dag(mctsbn.getBestDag()));
                //System.out.println("Tree Structure: ");
                //System.out.println(mctsbn);

                MlBayesIm controlBayesianNetwork;
                try {
                    controlBayesianNetwork = readOriginalBayesianNetwork(netPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                double shd = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(mctsbn.getBestDag()));

                System.out.println("SHD: " + shd);
            }
        });
    }

    public static MlBayesIm readOriginalBayesianNetwork(String netPath) throws Exception {
        BIFReader bayesianReader = new BIFReader();
        bayesianReader.processFile(netPath);
        BayesNet bayesianNet = bayesianReader;
        System.out.println("Numero de variables: " + bayesianNet.getNrOfNodes());

        //Transforming the BayesNet into a BayesPm
        BayesPm bayesPm = Utils.transformBayesNetToBayesPm(bayesianNet);
        MlBayesIm bn2 = new MlBayesIm(bayesPm);

        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);
        return bn2;
    }

}