package org.albacete.simd.experiments;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

        /*We are checking the following hyperparameters:
        * Threads: [1, 2, 4, 8, 16]
        * Interleaving: [5,10,15]
        *
        * We are going to experiment over */


public abstract class Experiment {

    protected String net_path;
    protected String bbdd_path;
    protected String net_name;
    protected String bbdd_name;
    protected String test_path;
    protected DataSet test_dataset;

    protected int nThreads;
    protected int nItInterleaving;
    protected int maxIterations = 15;
    //protected static HashMap<String, HashMap<String,String>> map;

    protected int shd = Integer.MAX_VALUE;
    protected double score;
    protected double [] dfmm;
    protected long elapsedTime;
    protected int nIterations;
    protected double LLscore;


    protected String log = "";
    protected String algName = "";

    public Experiment(String net_path, String bbdd_path, int nThreads, int maxIterations, int nItInterleaving) {
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
        this.maxIterations = maxIterations;
        this.nItInterleaving = nItInterleaving;
    }

    public Experiment(String net_path, String bbdd_path,  int nThreads, int nItInterleaving) {
        this(net_path, bbdd_path, nThreads, 15, nItInterleaving);
    }

    public Experiment(String net_path, String bbdd_path, int nThreads, int maxIterations, int nItInterleaving, long partition_seed) {
        this(net_path, bbdd_path, nThreads, maxIterations, nItInterleaving);
        Utils.setSeed(partition_seed);
    }






        public abstract void runExperiment();
//    {
//        try {
//            System.out.println("Starting Experiment:");
//            System.out.println("-----------------------------------------");
//            System.out.println("\tNet Name: " + net_name);
//            System.out.println("\tBBDD Name: " + bbdd_name);
//            //System.out.println("\tFusion Consensus: " + fusion_consensus);
//            System.out.println("\tnThreads: " + nThreads);
//            System.out.println("\tnItInterleaving: " + nItInterleaving);
//            System.out.println("-----------------------------------------");
//
//            System.out.println("Net_path: " + net_path);
//            System.out.println("BBDD_path: " + bbdd_path);
//
//            long startTime = System.currentTimeMillis();
//            BIFReader bf = new BIFReader();
//            bf.processFile(this.net_path);
//            BayesNet bn = (BayesNet) bf;
//            System.out.println("Numero de variables: "+bn.getNrOfNodes());
//            MlBayesIm bn2 = new MlBayesIm(bn);
//            DataReader reader = new DataReader();
//            reader.setDelimiter(DelimiterType.COMMA);
//            reader.setMaxIntegralDiscrete(100);
//
//            // Running Experiment
//            DataSet dataSet = reader.parseTabular(new File(this.bbdd_path));
//            this.alg = new PGESv2(dataSet,this.nThreads);
//            this.alg.setMaxIterations(this.maxIterations);
//            this.alg.setNFESItInterleaving(this.nItInterleaving);
//
//            // Search is executed
//            alg.search();
//
//            // Measuring time
//            long endTime = System.currentTimeMillis();
//
//            // Metrics
//            this.elapsedTime = endTime - startTime;
//            //System.out.println("Original DAG:");
//            //System.out.println(bn2.getDag());
//            //System.out.println("Total Nodes Original DAG:");
//            //System.out.println(bn2.getDag().getNodes().size());
//
//            /*
//            List<Node> nodes_original = bn2.getDag().getNodes();
//            List<Node> nodes_created = alg.getCurrentGraph().getNodes();
//
//            boolean cond = true;
//            for(Node node_original : nodes_original){
//                if (!nodes_created.contains(node_original)){
//                    cond = false;
//                }
//            }
//            */
//
//            // System.out.println(cond);
//
//
//
//            this.shd = Utils.compare(bn2.getDag(),(Dag) alg.getCurrentGraph());
//            this.dfmm = Utils.avgMarkovBlanquetdif(bn2.getDag(), (Dag) alg.getCurrentGraph());
//            this.nIterations = alg.getIterations();
//            this.score = GESThread.scoreGraph(alg.getCurrentGraph(), alg.getProblem());
//
//            //printResults();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }


    public abstract void printResults();

    public void saveExperiment()
    {
        try {
            // Saving paths
            //String path_iters = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusion_consensus + "_iteratation_results.csv";
            String className = this.getClass().getSimpleName();
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
            csvWriter_global.append("LL Score");
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

            String row = this.shd + "," + this.LLscore + "," + this.score + "," + this.dfmm[0] + "," + this.dfmm[1] + "," + this.dfmm[2] + "," + this.nIterations + ","  + elapsedTime/1000 + "\n";//this.elapsedTime + "\n";
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
    /*
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
    */


    public double[] getDfmm() {
        return dfmm;
    }

    public double getScore() {
        return score;
    }

    public int getShd() {
        return shd;
    }

    public long getElapsedTimeMiliseconds() {
        return elapsedTime;
    }

    public int getnIterations() {
        return nIterations;
    }

    public int getnItInterleaving() {
        return nItInterleaving;
    }

    public String getAlgName() {
        return algName;
    }

    @Override
    public String toString() {
        return "-----------------------\nExperiment " + algName + "\n-----------------------\nNet Name: " + net_name + "\tDatabase: " + bbdd_name + "\tThreads: " + nThreads + "\tInterleaving: " + nItInterleaving + "\tMax. Iterations: " + maxIterations;
    }
}

