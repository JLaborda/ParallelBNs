package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.ParallelFHCBES;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.File;

public class ExperimentPFHCBES extends Experiment {

    private ParallelFHCBES algorithm;


    public ExperimentPFHCBES(String net_path, String bbdd_path, String test_path, int nThreads, int maxIterations, int nItInterleaving, long partition_seed) {
        super(net_path, bbdd_path, test_path, nThreads, maxIterations, nItInterleaving, partition_seed);
        this.algName="pfhcbes";
    }

    @Override
    public void runExperiment(){
        try {
            System.out.println("Starting ParallelFHCBES Experiment:");
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
            BayesNet bn = bf;
            System.out.println("Numero de variables: " + bn.getNrOfNodes());
            //Transforming the BayesNet into a BayesPm
            BayesPm bayesPm = Utils.transformBayesNetToBayesPm(bn);
            MlBayesIm bn2 = new MlBayesIm(bayesPm);
            DataReader reader = new DataReader();
            reader.setDelimiter(DelimiterType.COMMA);
            reader.setMaxIntegralDiscrete(100);

            // Running Experiment
            DataSet dataSet = reader.parseTabular(new File(this.bbdd_path));
            this.algorithm = new ParallelFHCBES(dataSet, nThreads, maxIterations, nItInterleaving);

            // Search is executed
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
            this.shd = Utils.SHD((Dag) bn2.getDag(), (Dag) algorithm.getCurrentGraph());
            this.dfmm = Utils.avgMarkovBlanquetdif((Dag) bn2.getDag(), (Dag) algorithm.getCurrentGraph());
            this.nIterations = algorithm.getIterations();
            this.score = GESThread.scoreGraph(algorithm.getCurrentGraph(), algorithm.getProblem());
            this.LLscore = Utils.LL((Dag) algorithm.getCurrentGraph(), test_dataset);


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
        System.out.println(algorithm.getCurrentGraph());
        System.out.println("Total Nodes Current DAG");
        System.out.println(algorithm.getCurrentGraph().getNodes().size());
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
