package org.albacete.simd.experiments;

import edu.cmu.tetrad.algcomparison.algorithm.mixed.Mgm;
import edu.cmu.tetrad.algcomparison.independence.BDeuTest;
import edu.cmu.tetrad.algcomparison.independence.ChiSquare;
import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.*;
import edu.pitt.csb.mgm.MGM;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.mctsbn.HillClimbingEvaluator;
import org.albacete.simd.mctsbn.MCTSBN;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BNLearningAlgorithmsComparision {
    public static void main(String[] args) throws Exception {
        // 1. Configuration
        String networkFolder = "./res/networks/";
        String net_name = "pathfinder";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";

        DataSet ds = Utils.readData(bbdd_path);
        BDeuScore bdeu = new BDeuScore(ds);

        IndependenceTest chi = new IndTestChiSquare(DataUtils.getDiscreteDataSet(ds), 0.1);
        IndependenceTest bdeu_test = new IndTestScore(bdeu);

        Problem problem = new Problem(ds);
        MlBayesIm controlBayesianNetwork = readOriginalBayesianNetwork(net_path);

        double score;
        double shd;
        double init;
        Dag_n dag;
        Fges fges = new Fges(bdeu);
        GraphSearch alg;


        // PC - chi
        init = System.currentTimeMillis();
        alg = new Pc(chi);
        dag = new Dag_n(Utils.removeInconsistencies(alg.search()));
        printResults("PC - chi", fges, controlBayesianNetwork, init, dag);



        // PcMax - bdeu
        init = System.currentTimeMillis();
        alg = new PcStableMax(bdeu_test);
        dag = new Dag_n(Utils.removeInconsistencies(alg.search()));
        printResults("PCMax - bdeu", fges, controlBayesianNetwork, init, dag);

        // PC - bdeu
        init = System.currentTimeMillis();
        alg = new Pc(bdeu_test);
        dag = new Dag_n(Utils.removeInconsistencies(alg.search()));
        printResults("PC - bdeu", fges, controlBayesianNetwork, init, dag);

        // CPC - bdeu
        init = System.currentTimeMillis();
        alg = new Cpc(bdeu_test);
        dag = new Dag_n(Utils.removeInconsistencies(alg.search()));
        printResults("CPC - bdeu", fges, controlBayesianNetwork, init, dag);

        // fGES
        init = System.currentTimeMillis();
        alg = new Fges(bdeu);
        dag = new Dag_n(Utils.removeInconsistencies(alg.search()));
        printResults("fGES", fges, controlBayesianNetwork, init, dag);


    }

    private static void printResults(String alg, Fges fges, MlBayesIm controlBayesianNetwork, double init, Dag_n dag) {
        double score = fges.scoreDag(dag);
        double shd = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), dag);
        System.out.println("--------------- " + alg + " ---------------" +
                "\n BDeu: " + score +
                "\n SMHD: " + shd +
                "\n Time: " + (System.currentTimeMillis() - init) / 1000.0);
    }

    private static MlBayesIm readOriginalBayesianNetwork(String netPath) throws Exception {
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