package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.HillClimbingSearch;
import org.albacete.simd.algorithms.ParallelHillClimbingSearch;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExperimentHC extends Experiment {

    private HillClimbingSearch alg;


    public ExperimentHC(String net_path, String bbdd_path, int nThreads, int maxIterations, int nItInterleaving) {
        super(net_path, bbdd_path, nThreads, maxIterations, nItInterleaving);
        this.algName = "hc";
    }

    public ExperimentHC(String net_path, String bbdd_path, int nThreads, int nItInterleaving) {
        super(net_path, bbdd_path, nThreads, nItInterleaving);
        this.algName = "hc";
    }

    @Override
    public void runExperiment(){
        try {
            System.out.println("Starting Hill Climbing Search Experiment:");
            System.out.println("-----------------------------------------");
            System.out.println("\tNet Name: " + net_name);
            System.out.println("\tBBDD Name: " + bbdd_name);
            //System.out.println("\tFusion Consensus: " + fusion_consensus);
            System.out.println("\tNumber of Threads: " + nThreads);
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
            this.alg = new HillClimbingSearch(dataSet, maxIterations, nItInterleaving);

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
            this.nIterations = alg.getIterations();
            this.score = GESThread.scoreGraph(alg.getCurrentGraph(), alg.getProblem()); //alg.getFinalScore();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Override
    public void printResults(){
        //try {
        // Report
        System.out.println(this);
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
        //}
        //catch(InterruptedException e){
        //    e.printStackTrace();
        //}

    }


}
