package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.File;

public class ExperimentPGES2 extends Experiment {

    private BNBuilder algorithm;


    public ExperimentPGES2(String net_path, String bbdd_path, String test_path, int nThreads, int maxIterations, int nItInterleaving, long partition_seed) {
        super(net_path, bbdd_path, test_path, nThreads, maxIterations, nItInterleaving, partition_seed);
        this.algName="pges";
    }

    private BayesNet bn;
    private MlBayesIm bn2;

    @Override
    public void runExperiment(){
        try {
            System.out.println("Starting PGES2 Experiment:");
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
            bn = (BayesNet) bf;
            System.out.println("Numero de variables: "+bn.getNrOfNodes());
            bn2 = new MlBayesIm(bn);
            DataReader reader = new DataReader();
            reader.setDelimiter(DelimiterType.COMMA);
            reader.setMaxIntegralDiscrete(100);

            // Running Experiment
            DataSet dataSet = reader.parseTabular(new File(this.bbdd_path));
            this.algorithm = new PGESwithStages(dataSet, nThreads, maxIterations, nItInterleaving);

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
            this.shd = Utils.SHD(bn2.getDag(),(Dag) algorithm.getCurrentGraph());
            this.dfmm = Utils.avgMarkovBlanquetdif(bn2.getDag(), (Dag) algorithm.getCurrentGraph());
            this.nIterations = algorithm.getIterations();
            this.score = GESThread.scoreGraph(algorithm.getCurrentGraph(), algorithm.getProblem());
            this.LLscore = Utils.LL((Dag)algorithm.getCurrentGraph(), test_dataset);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

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
        return algorithm.getCurrentGraph().getNumNodes();
    }
    
    @Override
    public int getNewNarcs(){
        return algorithm.getCurrentGraph().getNumEdges();
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
    public void printResults(){
        //try {
        // Report
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
        //}
        //catch(InterruptedException e){
        //    e.printStackTrace();
        //}

    }

}
