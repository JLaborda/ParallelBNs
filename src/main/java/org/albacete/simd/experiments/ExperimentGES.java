package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.GES.GES;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ExperimentGES extends Experiment{




    private int maxIterations = 15;
    private GES alg;
    private static HashMap<String, HashMap<String,String>> map;

    private int shd = Integer.MAX_VALUE;
    private double score;
    private double [] dfmm;
    private long elapsedTime;
    private int nIterations;


    public ExperimentGES(String net_path, String bbdd_path) {
        super(net_path, bbdd_path, 0, 0);
    }




    @Override
    public void runExperiment() {
        try {
            System.out.println("Starting GES Experiment:");
            System.out.println("-----------------------------------------");
            System.out.println("\tNet Name: " + net_name);
            System.out.println("\tBBDD Name: " + bbdd_name);
            //System.out.println("\tFusion Consensus: " + fusion_consensus);
            System.out.println("-----------------------------------------");

            System.out.println("Net_path: " + net_path);
            System.out.println("BBDD_path: " + bbdd_path);

            long startTime = System.currentTimeMillis();
            BIFReader bf = new BIFReader();
            bf.processFile(this.net_path);
            BayesNet bn = (BayesNet) bf;
            System.out.println("Numero de variables: "+bn.getNrOfNodes());
            MlBayesIm bn2 = new MlBayesIm(bn);
            DataReader reader = new DataReader();
            reader.setDelimiter(DelimiterType.COMMA);
            reader.setMaxIntegralDiscrete(100);

            // Running Experiment
            DataSet dataSet = reader.parseTabular(new File(this.bbdd_path));
            this.alg = new GES(dataSet);

            // Search is executed
            alg.search();

            // Measuring time
            long endTime = System.currentTimeMillis();

            // Metrics
            this.elapsedTime = endTime - startTime;
            System.out.println("Original DAG:");
            System.out.println(bn2.getDag());
            System.out.println("Total Nodes Original DAG:");
            System.out.println(bn2.getDag().getNodes().size());

        /*
        List<Node> nodes_original = bn2.getDag().getNodes();
        List<Node> nodes_created = alg.getCurrentGraph().getNodes();

        boolean cond = true;
        for(Node node_original : nodes_original){
            if (!nodes_created.contains(node_original)){
                cond = false;
            }
        }
        */

            // System.out.println(cond);



            this.shd = Utils.compare(bn2.getDag(),(Dag) alg.getCurrentGraph());
            this.dfmm = Utils.avgMarkovBlanquetdif(bn2.getDag(), (Dag) alg.getCurrentGraph());
            //this.nIterations = alg.getIterations();
            this.score = GESThread.scoreGraph(alg.getCurrentGraph(), alg.getProblem()); //alg.getFinalScore();

            printResults();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Override
    public void printResults(){
        try {
            // Report
            System.out.println("Current DAG:");
            System.out.println(alg.getCurrentGraph());
            System.out.println("Total Nodes Current DAG");
            System.out.println(alg.getCurrentGraph().getNodes().size());
            System.out.println("-------------------------\nMetrics: ");

            System.out.println("SHD: " + shd);
            System.out.println("Final BDeu: " + this.score);
            System.out.println("Total execution time (s): " + elapsedTime / 1000);
            System.out.println("Total number of Iterations: " + this.nIterations);
            System.out.println("dfMM: " + dfmm[0]);
            System.out.println("dfMM plus: " + dfmm[1]);
            System.out.println("dfMM minus: " + dfmm[2]);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }

    }

    @Override
    public void saveExperiment() {
        try {
            // Saving paths
            //String path_iters = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusion_consensus + "_iteratation_results.csv";
            String path_global = "experiments/" + this.net_name + "/" + this.bbdd_name + "_ges" + "_global_results.csv";

            // Files
            //File file_iters = new File(path_iters);
            //file_iters.getParentFile().mkdirs();
            File file_global = new File(path_global);
            file_global.getParentFile().mkdirs();

            // File Writers
            //FileWriter csvWriter_iters = new FileWriter(file_iters);
            FileWriter csvWriter_global = new FileWriter(file_global);



            // Saving global results
            csvWriter_global.append("SHD");
            csvWriter_global.append(",");
            csvWriter_global.append("BDeu Score");
            csvWriter_global.append(",");
            csvWriter_global.append("dfMM");
            csvWriter_global.append(",");
            csvWriter_global.append("dfMM plus");
            csvWriter_global.append(",");
            csvWriter_global.append("dfMM minus");
            csvWriter_global.append(",");
            csvWriter_global.append("Total time(s)");
            csvWriter_global.append("\n");

            String row = this.shd + "," + this.score + "," + this.dfmm[0] + "," + this.dfmm[1] + "," + this.dfmm[2] +  ","  + elapsedTime/1000 + "\n";//this.elapsedTime + "\n";
            csvWriter_global.append(row);

            csvWriter_global.flush();
            csvWriter_global.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }






    public static void main(String[] args) {
        String netFolder = "res/networks/";
        String bbddFolder = "res/networks/BBDD/";
        ArrayList<String> net_paths = getNetworkPaths(netFolder);
        ArrayList<String> bbdd_paths = getBBDDPaths(bbddFolder);



        System.out.println("net_names: " + net_paths);
        System.out.println("bbdd_names: " + bbdd_paths);

        map =  hashNetworks(net_paths, bbdd_paths);

        System.out.println("Values in Map:");
        for (String key: map.keySet()) {
            HashMap<String, String> aux = map.get(key);
            for(String k : aux.keySet()) {
                System.out.println("BBDD_Number: " + key);
                System.out.println("Key: " + k);
                System.out.println("Value: " + aux.get(k));
                System.out.println("******************");

            }
        }


        // Running Single Experiment
        //Experiments experiment = new Experiments("res/networks/win95pts.xbif", "res/networks/BBDD/win95pts.xbif_.csv", 2, 5);
        //org.albacete.simd.experiments.Experiment experiment = new org.albacete.simd.experiments.Experiment("res/networks/alarm.xbif", "res/networks/BBDD/alarm.xbif_.csv", 2, 5);
        ExperimentGES experiment = new ExperimentGES("res/networks/alarm.xbif", "res/networks/BBDD/alarm.xbif_.csv");

        experiment.runExperiment();
        //Saving Experiment
        experiment.saveExperiment();


        // Running all the experiments
        //runAllExperiments();

    }




}



