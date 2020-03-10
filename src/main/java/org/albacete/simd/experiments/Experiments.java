package org.albacete.simd.experiments;

import consensusBN.ConsensusBES;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.Main;
import org.albacete.simd.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

        /*We are checking the following hyperparameters:
        * Threads: [1, 2, 4, 8, 16]
        * Interleaving: [5,10,15]
        *
        * We are going to experiment over */


public class Experiments {

    private String net_path;
    private String bbdd_path;
    private String net_name;
    private String bbdd_name;
    private int nThreads;
    //private String fusion_consensus;
    private int nItInterleaving;
    private int maxIterations = 15;
    private Main alg;
    private static HashMap<String, HashMap<String,String>> map;

    private int shd;
    private double score;
    private double [] dfmm;
    private long elapsedTime;
    private int nIterations;


    public Experiments(String net_path, String bbdd_path,  int nThreads, int nItInterleaving) {
        this.net_path = net_path;
        this.bbdd_path = bbdd_path;
        Pattern pattern = Pattern.compile("/(.*)\\.");
        Matcher matcher = pattern.matcher(this.net_path);
        if (matcher.find()) {
            System.out.println("Match!");
            System.out.println(matcher.group(1));
            net_name = matcher.group(1);
        }

        pattern = Pattern.compile(".*/(.*).csv");
        matcher = pattern.matcher(this.bbdd_path);
        if (matcher.find()) {
            System.out.println("Match!");
            System.out.println(matcher.group(1));
            bbdd_name = matcher.group(1);
        }


        this.nThreads = nThreads;
        this.nItInterleaving = nItInterleaving;
    }





    public void runExperiment() {
        try {
            System.out.println("Starting Experiment:");
            System.out.println("-----------------------------------------");
            System.out.println("\tNet Name: " + net_name);
            System.out.println("\tBBDD Name: " + bbdd_name);
            //System.out.println("\tFusion Consensus: " + fusion_consensus);
            System.out.println("\tnThreads: " + nThreads);
            System.out.println("\tnItInterleaving: " + nItInterleaving);
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
            this.alg = new Main(dataSet,this.nThreads);
            this.alg.setMaxIterations(this.maxIterations);
            this.alg.setNFESItInterleaving(this.nItInterleaving);

            // Search is executed
            alg.search();

            // Measuring time
            long endTime = System.currentTimeMillis();

            // Metrics
            this.elapsedTime = endTime - startTime;
            this.shd = Utils.compare(bn2.getDag(),(Dag) alg.getCurrentGraph());
            this.dfmm = Utils.avgMarkovBlanquetdif(bn2.getDag(), (Dag) alg.getCurrentGraph());
            this.nIterations = alg.getIterations();
            this.score = Utils.scoreGraph(alg.getCurrentGraph(), dataSet); //alg.getFinalScore();

            // Report
            System.out.println("SHD: "+shd);
            System.out.println("Final BDeu: " +this.score);
            System.out.println("Total execution time (s): " + elapsedTime/1000);
            System.out.println("Total number of Iterations: " + this.nIterations);
            System.out.println("dfMM: "+ dfmm[0]);
            System.out.println("dfMM plus: "+ dfmm[1]);
            System.out.println("dfMM minus: "+ dfmm[2]);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //TODO: save experiments
    public void saveExperiment() {
        try {
            // Saving paths
            //String path_iters = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusion_consensus + "_iteratation_results.csv";
            String path_global = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving +  "_global_results.csv";

            // Files
            //File file_iters = new File(path_iters);
            //file_iters.getParentFile().mkdirs();
            File file_global = new File(path_global);
            file_global.getParentFile().mkdirs();

            // File Writers
            //FileWriter csvWriter_iters = new FileWriter(file_iters);
            FileWriter csvWriter_global = new FileWriter(file_global);

            // Iterations results header
            /*
            csvWriter_iters.append("Iteration");
            csvWriter_iters.append(",");
            csvWriter_iters.append("Score_Threads");
            csvWriter_iters.append(",");
            csvWriter_iters.append("Score_Fusion");
            csvWriter_iters.append(",");
            csvWriter_iters.append("Score_Delta");
            csvWriter_iters.append(",");
            csvWriter_iters.append("Time_Iteration(ms)");
            csvWriter_iters.append(",");
            csvWriter_iters.append("Time_Fusion(ms)");
            csvWriter_iters.append(",");
            csvWriter_iters.append("Time_Delta(ms)");
            csvWriter_iters.append("\n");

            // Getting results
            ArrayList<Double> score_threads = this.alg.getScores_threads();
            ArrayList<Double> score_fusion = this.alg.getScores_fusion();
            ArrayList<Double> score_delta = this.alg.getScores_delta();
            ArrayList<Long> time_iterations = this.alg.getTimes_iterations();
            ArrayList<Long> time_fusion = this.alg.getTimes_fusion();
            ArrayList<Long> time_delta = this.alg.getTimes_delta();

            // All of the arrays should have the same size.
            for(int i=0; i< score_threads.size(); i++) {
                String row = (i+1) + "," + score_threads.get(i) + "," + score_fusion.get(i) + "," + score_delta.get(i) + "," + time_iterations.get(i) + "," + time_fusion.get(i) + "," + time_delta.get(i) + "\n";
                csvWriter_iters.append(row);
            }
            csvWriter_iters.flush();
            csvWriter_iters.close();
            */

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
            csvWriter_global.append("Total iterations");
            csvWriter_global.append(",");
            csvWriter_global.append("Total time(s)");
            csvWriter_global.append("\n");

            String row = this.shd + "," + this.score + "," + this.dfmm[0] + "," + this.dfmm[1] + "," + this.dfmm[2] + "," + this.nIterations + ","  + elapsedTime/1000 + "\n";//this.elapsedTime + "\n";
            csvWriter_global.append(row);

            csvWriter_global.flush();
            csvWriter_global.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static ArrayList<String> getNetworkPaths(String netFolder){
        // Getting networks

        File f = new File(netFolder);
        ArrayList<String> net_paths = new ArrayList<String>(Arrays.asList(f.list()));
        net_paths.removeIf(s -> !s.contains(".xbif"));
        ListIterator<String> iter = net_paths.listIterator();
        while(iter.hasNext()) {
            iter.set(netFolder + iter.next());
        }

        return net_paths;

    }

    public static ArrayList<String> getBBDDPaths(String bbddFolder){
        // Getting BBDD

        File f = new File(bbddFolder);
        ArrayList<String> bbdd_paths = new ArrayList<String>(Arrays.asList(f.list()));

        ListIterator<String> iter = bbdd_paths.listIterator();
        while(iter.hasNext()) {
            iter.set(bbddFolder + iter.next());
        }
        return bbdd_paths;
    }

    //public static HashMap<String, ArrayList<String>> hashNetworks(ArrayList<String> net_paths, ArrayList<String> bbdd_paths){
    public static HashMap<String, HashMap<String, String>> hashNetworks(ArrayList<String> net_paths, ArrayList<String> bbdd_paths){

        HashMap<String, HashMap<String,String>> result = new HashMap<String,HashMap<String,String>>();

        ArrayList<String> bbdd_numbers = new ArrayList<String>();

        for(String bbdd: bbdd_paths) {
            Pattern pattern =Pattern.compile("(xbif.*).csv");
            Matcher matcher = pattern.matcher(bbdd);
            if(matcher.find()) {
                bbdd_numbers.add(matcher.group(1));
            }
        }

        for(String bbdd_number : bbdd_numbers) {
            HashMap<String, String> aux = new HashMap<String, String>();


            for(String bbdd_path: bbdd_paths) {
                if(bbdd_path.contains(bbdd_number)) {
                    for(String net_path: net_paths) {
                        Pattern pattern = Pattern.compile("/(.*)\\.");
                        Matcher matcher = pattern.matcher(net_path);
                        String net_name = null;
                        if (matcher.find()) {
                            //System.out.println("Match!");
                            net_name = matcher.group(1);
                        }
                        else
                            continue;
                        if (bbdd_path.contains(net_name)){
                            aux.put(net_path, bbdd_path);
                        }
                    }
                }
            }
            result.put(bbdd_number, aux);

        }
        return result;
    }

    public static void runAllExperiments(){
        // Hiperparameters
        int [] nThreads = new int[] {1,2,4,8};
        int [] nInterleavings = new int[] {5,10,15};

        // Running Single Experiment
        //Experiments experiment = new Experiments("networks/win95pts.xbif", "networks/BBDD/win95pts.xbif_.csv", fusions[0], nThreads[0], nInterleavings[0]);
        //experiment.runExperiment();
        //Saving Experiment
        //experiment.saveExperiment();



        for(int j=0; j<nThreads.length; j++) {
            int nThread = nThreads[j];
            for(int k=0; k<nInterleavings.length; k++) {
                int nInterleaving = nInterleavings[k];
                for(String key1 : map.keySet()) {
                    HashMap<String,String> aux = map.get(key1);
                    for (String net_path: aux.keySet()) {
                        System.out.println("***********************************");
                        System.out.println("***********NEW EXPERIMENT**********");
                        System.out.println("***********************************");
                        String bbdd_path = aux.get(net_path);
                        System.out.println("Net_Path: " + net_path);
                        System.out.println("BBDD_Path: " + bbdd_path);

                        // Running Experiment
                        Experiments experiment = new Experiments(net_path, bbdd_path, nThread, nInterleaving);
                        experiment.runExperiment();
                        //Saving Experiment
                        experiment.saveExperiment();

                    }
                }
            }
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

        //Problems with:
		/*
		 * -----------------------------------------
		Net Name: barley
		BBDD Name: barley.xbif_
		Fusion Consensus: ConsensusBES
		nThreads: 2
		nItInterleaving: 15
		-----------------------------------------
		Net_path: networks/barley.xbif
		BBDD_path: networks/BBDD/barley.xbif_.csv
		 * */


        // Running Single Experiment
        Experiments experiment = new Experiments("res/networks/win95pts.xbif", "res/networks/BBDD/win95pts.xbif_.csv", 2, 5);
        experiment.runExperiment();
        //Saving Experiment
        experiment.saveExperiment();


        // Running all the experiments
        //runAllExperiments();

    }




}

