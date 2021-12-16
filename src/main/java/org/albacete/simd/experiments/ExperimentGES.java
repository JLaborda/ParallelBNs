package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.GES;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.File;

public class ExperimentGES extends Experiment{


    private GES algorithm;
/*
    public ExperimentGES(String net_path, String bbdd_path, String test_path, int nItInterleaving){
        super(net_path, bbdd_path, test_path, 0, 0,nItInterleaving);
        algName = "ges";
    }
*/
    public ExperimentGES(String net_path, String bbdd_path, String test_path) {
        super(net_path, bbdd_path, test_path, 0, 0, Integer.MAX_VALUE);
        algName = "ges";
    }
    
    private BayesNet bn;
    private MlBayesIm bn2;

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
            bn = (BayesNet) bf;
            System.out.println("Numero de variables: "+bn.getNrOfNodes());
            bn2 = new MlBayesIm(bn);
            DataReader reader = new DataReader();
            reader.setDelimiter(DelimiterType.COMMA);
            reader.setMaxIntegralDiscrete(100);

            // Running Experiment
            DataSet dataSet = reader.parseTabular(new File(this.bbdd_path));
            this.algorithm = new GES(dataSet);

            // Search is executed
            //alg.search();
            algorithm.search();
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



            //Metrics
            this.shd = Utils.SHD(bn2.getDag(),(Dag) algorithm.getCurrentGraph());
            this.dfmm = Utils.avgMarkovBlanquetdif(bn2.getDag(), (Dag) algorithm.getCurrentGraph());
            //this.nIterations = algorithm.getIterations();
            this.score = GESThread.scoreGraph(algorithm.getCurrentGraph(), algorithm.getProblem());
            this.LLscore = Utils.LL((Dag)algorithm.getCurrentGraph(), test_dataset);

            //printResults();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Override
    public void printResults(){
        try {
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
        catch(InterruptedException e){
            e.printStackTrace();
        }

    }
/*
    @Override
    public void saveExperiment() {
        try {
            // Saving paths
            //String path_iters = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusion_consensus + "_iteratation_results.csv";
            String path_global = "experiments/" + this.net_name + "/" + this.algName + "/" + this.bbdd_name  + "_global_results.csv";

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
            e.printStackTrace();
        }

    }
*/
    
    @Override
    public int getNnodes() {
        return bn2.getDag().getNumNodes();
    }
    
    @Override
    public int getNarcs() {
        return bn2.getDag().getNumEdges();
    }
    
    @Override
    public int getNewNnodes(){
        try {
            return algorithm.getCurrentGraph().getNumNodes();
        } catch(InterruptedException e){
            return -1;
        }
    }
    
    @Override
    public int getNewNarcs(){
        try { 
            return algorithm.getCurrentGraph().getNumEdges();
        } catch(InterruptedException e){
            return -1;
        }
    }
    
    @Override
    public int getNparams() {
        int temp = 0;
        for (int i = 0; i < bn.getNrOfNodes(); i++) {
            temp += bn.getCardinality(i);
        }
        return temp;
    }

    @Override
    public String toString() {
        return "-----------------------\nExperiment " + algName + "\n-----------------------\nNet Name: " + net_name + "\tDatabase: " + bbdd_name;
    }


}



