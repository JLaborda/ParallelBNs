package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.PGESv2;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentPGES extends Experiment{


    private PGESv2 algorithm;

    public ExperimentPGES(String net_path, String bbdd_path, int nThreads, int maxIterations, int nItInterleaving) {
        super(net_path,bbdd_path,nThreads,maxIterations,nItInterleaving);
        this.algName = "pges";

    }

    public ExperimentPGES(String net_path, String bbdd_path, int nThreads, int nItInterleaving) {
        this(net_path, bbdd_path,  nThreads, 15, nItInterleaving);
    }

    public ExperimentPGES(String net_path, String bbdd_path, String test_path, int nThreads, int maxIterations, int nItInterleaving) {
        super(net_path,bbdd_path,nThreads,maxIterations,nItInterleaving);
        this.algName = "pges";
        this.test_path = test_path;
        this.test_dataset  = Utils.readData(test_path);
    }

    public ExperimentPGES(String net_path, String bbdd_path, String test_path, int nThreads, int nItInterleaving) {
        this(net_path, bbdd_path,test_path,  nThreads, 15, nItInterleaving);
    }

    public ExperimentPGES(String net_path, String bbdd_path, String test_path, int nThreads, int maxIterations, int nItInterleaving, long seed) {
        super(net_path,bbdd_path, nThreads,maxIterations,nItInterleaving, seed);
        this.algName = "pges";
        this.test_path = test_path;
        this.test_dataset  = Utils.readData(test_path);
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
            this.algorithm = new PGESv2(dataSet,this.nThreads);
            this.algorithm.setMaxIterations(this.maxIterations);
            this.algorithm.setNFESItInterleaving(this.nItInterleaving);

            // Search is executed
            algorithm.search();

            // Measuring time
            long endTime = System.currentTimeMillis();

            // Metrics
            this.elapsedTime = endTime - startTime;
            //System.out.println("Original DAG:");
            //System.out.println(bn2.getDag());
            //System.out.println("Total Nodes Original DAG:");
            //System.out.println(bn2.getDag().getNodes().size());

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


            //Metrics
            this.shd = Utils.SHD(bn2.getDag(),(Dag) algorithm.getCurrentGraph());
            this.dfmm = Utils.avgMarkovBlanquetdif(bn2.getDag(), (Dag) algorithm.getCurrentGraph());
            this.nIterations = algorithm.getIterations();
            this.score = GESThread.scoreGraph(algorithm.getCurrentGraph(), algorithm.getProblem());
            this.LLscore = Utils.LL((Dag)algorithm.getCurrentGraph(), test_dataset);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void printResults() {
        // Report
        System.out.println(this);
        System.out.println("Resulting DAG:");
        System.out.println(algorithm.getCurrentGraph());
        System.out.println("Total Nodes of Resulting DAG");
        System.out.println(algorithm.getCurrentGraph().getNodes().size());
        System.out.println("-------------------------\nMetrics: ");

        System.out.println("SHD: "+shd);
        System.out.println("LLScore: " + this.LLscore);
        System.out.println("Final BDeu: " +this.score);
        System.out.println("Total execution time (s): " + elapsedTime/1000);
        System.out.println("Total number of Iterations: " + this.nIterations);
        System.out.println("dfMM: "+ dfmm[0]);
        System.out.println("dfMM plus: "+ dfmm[1]);
        System.out.println("dfMM minus: "+ dfmm[2]);

    }

    /*
    @Override
    public void saveExperiment() {
        try {
            // Saving paths
            //String path_iters = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusion_consensus + "_iteratation_results.csv";
            String path_global = "experiments/" + this.net_name + "/" + this.algName + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving +  "_global_results.csv";
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
*/
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
    public static HashMap<String, HashMap<String, String>> hashNetworks(List<String> net_paths, List<String> bbdd_paths){

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
                        //Pattern pattern = Pattern.compile("/(.*)\\.");
                        Pattern pattern = Pattern.compile("/(\\w+)\\..*");
                        Matcher matcher = pattern.matcher(net_path);

                        if (matcher.find()) {
                            //System.out.println("Match!");
                            String net_name = matcher.group(1);
                            //System.out.println("Net name: " + net_name);
                            //System.out.println("BBDD Path: " + bbdd_path);
                            if (bbdd_path.contains(net_name)){
                                aux.put(net_path, bbdd_path);
                            }
                        }

                    }
                }
            }
            result.put(bbdd_number, aux);

        }
        return result;
    }
}
