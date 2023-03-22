package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.algcomparison.statistic.SHD;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.BDeuScore;
import org.albacete.simd.utils.Problem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.experiments.ExperimentBNBuilder;
import org.albacete.simd.framework.BNBuilder;
import static org.albacete.simd.mctsbn.MainMCTSBN.readOriginalBayesianNetwork;
import org.albacete.simd.utils.Utils;

public class HillClimbingEvaluatorTest {

    @Test
    public void scoreTest(){
        String networkFolder = "./res/networks/";
        String net_name = "alarm";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";

        int seed = 11231231;
        Problem problem = new Problem(bbdd_path);
        List<Node> order = randomOrder(problem, seed);


        int child = problem.getHashIndices().get(order.get(5));
        int candidate = problem.getHashIndices().get(order.get(4));


        Set<Integer> parents = new HashSet<>();
        Integer[] parentsArr = new Integer[parents.size()];
        parents.toArray(parentsArr);
        Arrays.sort(parentsArr);


        ConcurrentHashMap<String,Double> cache = new ConcurrentHashMap<>();
        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, order, cache);

        // OPERATION ADD
        double scoreAdd = hc.getAdditionScore(child, candidate, new HashSet<>(parents), parentsArr);

        // Update Graph
        parents.add(candidate);
        parentsArr = new Integer[parents.size()];
        parents.toArray(parentsArr);
        Arrays.sort(parentsArr);

        // OPERATION DELETE
        double scoreDelete = hc.getDeleteScore(child, candidate, new HashSet<>(parents), parentsArr);

        Assert.assertEquals(scoreAdd, -scoreDelete, 0.0000001);
    }


    @Test
    public void bestOrderTest(){ String networkFolder = "./res/networks/";
        String net_name = "earthquake";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        Problem problem = new Problem(bbdd_path);

        List<Node> badOrder = new ArrayList<>();
        badOrder.add(problem.getNode("JohnCalls"));
        badOrder.add(problem.getNode("MaryCalls"));
        badOrder.add(problem.getNode("Alarm"));
        badOrder.add(problem.getNode("Earthquake"));
        badOrder.add(problem.getNode("Burglary"));

        List<Node> optimalOrder = new ArrayList<>();
        optimalOrder.add(problem.getNode("Burglary"));
        optimalOrder.add(problem.getNode("Earthquake"));
        optimalOrder.add(problem.getNode("Alarm"));
        optimalOrder.add(problem.getNode("MaryCalls"));
        optimalOrder.add(problem.getNode("JohnCalls"));


        ConcurrentHashMap<String,Double> cache1 = new ConcurrentHashMap<>();
        ConcurrentHashMap<String,Double> cache2 = new ConcurrentHashMap<>();
        HillClimbingEvaluator hcBadOrder = new HillClimbingEvaluator(problem, badOrder, cache1);
        HillClimbingEvaluator hcOptimalOrder = new HillClimbingEvaluator(problem, optimalOrder, cache2);

        double badScore = hcBadOrder.search();
        double optimalScore = hcOptimalOrder.search();

        System.out.println("Bad Score: " + badScore);
        System.out.println("Optimal Score: " + optimalScore);

        Assert.assertTrue(optimalScore > badScore);

    }
    
    
    @Test
    public void bestOrderBigTest(){ String networkFolder = "./res/networks/";
        String net_name = "water";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        String net_path = networkFolder + net_name + ".xbif";
        Problem problem = new Problem(bbdd_path);
        
        String[] endings = {"_12_00","_12_15","_12_30","_12_45"};
        
        List<Node> badOrder = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            badOrder.add(problem.getNode("C_NI" + endings[i]));
            badOrder.add(problem.getNode("CKNI" + endings[i]));
            badOrder.add(problem.getNode("CBODD" + endings[i]));
            badOrder.add(problem.getNode("CNOD" + endings[i]));
            badOrder.add(problem.getNode("CBODN" + endings[i]));
            badOrder.add(problem.getNode("CNON" + endings[i]));
            badOrder.add(problem.getNode("CKNN" + endings[i]));
            badOrder.add(problem.getNode("CKND" + endings[i]));
        }
        List<Node> optimalOrder = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            optimalOrder.add(problem.getNode("C_NI" + endings[i]));
            optimalOrder.add(problem.getNode("CKNI" + endings[i]));
            optimalOrder.add(problem.getNode("CBODD" + endings[i]));
            optimalOrder.add(problem.getNode("CNOD" + endings[i]));
            optimalOrder.add(problem.getNode("CBODN" + endings[i]));
            optimalOrder.add(problem.getNode("CNON" + endings[i]));
            optimalOrder.add(problem.getNode("CKNN" + endings[i]));
            optimalOrder.add(problem.getNode("CKND" + endings[i]));
        }
        
        ConcurrentHashMap<String,Double> cache1 = new ConcurrentHashMap<>();
        ConcurrentHashMap<String,Double> cache2 = new ConcurrentHashMap<>();
        HillClimbingEvaluator hcBadOrder = new HillClimbingEvaluator(problem, badOrder, cache1);
        HillClimbingEvaluator hcOptimalOrder = new HillClimbingEvaluator(problem, optimalOrder, cache2);
        
        MlBayesIm controlBayesianNetwork = null;
        try {
            controlBayesianNetwork = readOriginalBayesianNetwork(net_path);
        } catch (Exception e) {}
        double badScore = hcBadOrder.search();
        double optimalScore = hcOptimalOrder.search();
        
        double shdOptimal = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(hcOptimalOrder.getGraph()));
        double shdBad = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(hcBadOrder.getGraph()));
        System.out.println("Bad Score:     " + badScore + ", \t SHD: " + shdBad);
        System.out.println("Optimal Score: " + optimalScore + ", \t SHD: " + shdOptimal);
        
        
        Assert.assertTrue(optimalScore > badScore);
    }
    
    
    @Test
    public void bestOrderGESTest(){ String networkFolder = "./res/networks/";
        String net_name = "water";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        String net_path = networkFolder + net_name + ".xbif";
        String test_path = networkFolder + "BBDD/tests/" + net_name + "_test.csv";
        Problem problem = new Problem(bbdd_path);
        String[] endings = {"_12_00","_12_15","_12_30","_12_45"};
        
        BNBuilder algorithm = new GES_BNBuilder(problem.getData(), true);
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(algorithm, net_name, net_path, bbdd_path, test_path);//new ExperimentBNBuilder(algorithm, net_path, bbdd_path, test_path, 42);
        System.out.println("Alg Name: " + experiment.getAlgName());
        experiment.runExperiment();
        experiment.printResults();
        List<Node> gesOrder = algorithm.getCurrentDag().getTopologicalOrder();
        System.out.println("\n\nOrder GES: " + gesOrder + "\n\n");
        List<Node> optimalOrder = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            optimalOrder.add(problem.getNode("C_NI" + endings[i]));
            optimalOrder.add(problem.getNode("CKNI" + endings[i]));
            optimalOrder.add(problem.getNode("CBODD" + endings[i]));
            optimalOrder.add(problem.getNode("CNOD" + endings[i]));
            optimalOrder.add(problem.getNode("CBODN" + endings[i]));
            optimalOrder.add(problem.getNode("CNON" + endings[i]));
            optimalOrder.add(problem.getNode("CKNN" + endings[i]));
            optimalOrder.add(problem.getNode("CKND" + endings[i]));
        }
        
        ConcurrentHashMap<String,Double> cache1 = new ConcurrentHashMap<>();
        ConcurrentHashMap<String,Double> cache2 = new ConcurrentHashMap<>();
        HillClimbingEvaluator hcGesOrder = new HillClimbingEvaluator(problem, gesOrder, cache1);
        HillClimbingEvaluator hcOptimalOrder = new HillClimbingEvaluator(problem, optimalOrder, cache2);
        
        MlBayesIm controlBayesianNetwork = null;
        try {
            controlBayesianNetwork = readOriginalBayesianNetwork(net_path);
        } catch (Exception e) {}
        double gesScore = hcGesOrder.search();
        double optimalScore = hcOptimalOrder.search();
        
        double shdOptimal = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(hcOptimalOrder.getGraph()));
        double shdBad = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(hcGesOrder.getGraph()));
        System.out.println("\n\nGES Score:     " + experiment.getBdeuScore() + ", \t SHD: " + experiment.getStructuralHamiltonDistanceValue() + ".0");
        System.out.println("HC GES Score:  " + gesScore + ", \t SHD: " + shdBad);
        System.out.println("Optimal Score: " + optimalScore + ", \t SHD: " + shdOptimal + "\n");
        
        
        Assert.assertTrue(gesScore + 0.000000001 >= experiment.getBdeuScore());
    }
    
    @Test
    public void bestOrderGESPathfinderTest(){ String networkFolder = "./res/networks/";
        String net_name = "pathfinder";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        String net_path = networkFolder + net_name + ".xbif";
        String test_path = networkFolder + "BBDD/tests/" + net_name + "_test.csv";
        Problem problem = new Problem(bbdd_path);
        
        BNBuilder algorithm = new GES_BNBuilder(problem.getData(), true);
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(algorithm, net_name, net_path, bbdd_path, test_path);//new ExperimentBNBuilder(algorithm, net_path, bbdd_path, test_path, 42);
        System.out.println("Alg Name: " + experiment.getAlgName());
        experiment.runExperiment();
        experiment.printResults();
        List<Node> gesOrder = algorithm.getCurrentDag().getTopologicalOrder();
        System.out.println("\n\nOrder GES: " + gesOrder + "\n\n");
        
        List<Node> optimalOrder = new ArrayList<>();
        optimalOrder.add(problem.getNode("Fault"));
        for (int i = 1; i <= 108; i++) {
            optimalOrder.add(problem.getNode("F" + i));
        }
        
        List<Node> badOrder = new ArrayList<>();
        for (int i = 1; i <= 108; i++) {
            badOrder.add(problem.getNode("F" + i));
        }
        badOrder.add(problem.getNode("Fault"));
        
        ConcurrentHashMap<String,Double> cache = new ConcurrentHashMap<>();
        HillClimbingEvaluator hcGesOrder = new HillClimbingEvaluator(problem, gesOrder, cache);
        HillClimbingEvaluator hcOptimalOrder = new HillClimbingEvaluator(problem, optimalOrder, cache);
        HillClimbingEvaluator hcBadOrder = new HillClimbingEvaluator(problem, badOrder, cache);
        
        MlBayesIm controlBayesianNetwork = null;
        try {
            controlBayesianNetwork = readOriginalBayesianNetwork(net_path);
        } catch (Exception e) {}
        
        System.out.println("\nSearch GES order: ");
        long startTime = System.currentTimeMillis();
        double gesScore = hcGesOrder.search();
        System.out.println((System.currentTimeMillis()-startTime)/1000.0 + " seconds");
        
        System.out.println("\nSearch OPTIMAL order: ");
        startTime = System.currentTimeMillis();
        double optimalScore = hcOptimalOrder.search();
        System.out.println((System.currentTimeMillis()-startTime)/1000.0 + " seconds");
        
        System.out.println("\nSearch BAD order: ");
        startTime = System.currentTimeMillis();
        double badScore = hcBadOrder.search();
        System.out.println((System.currentTimeMillis()-startTime)/1000.0 + " seconds");
        
        double mhdGES = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(hcGesOrder.getGraph()));
        double mhdOptimal = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(hcOptimalOrder.getGraph()));
        double mhdBad = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),new Dag_n(hcBadOrder.getGraph()));
        
        SHD shdTetrad = new SHD();
        System.out.println("\nCalculating SHD GES");
        double shdGESReal = shdTetrad.getValue(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), new Dag_n(algorithm.getCurrentDag()), problem.getData());
        System.out.println("\n\nCalculating SHD GES HC");
        double shdGES = shdTetrad.getValue(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), new Dag_n(hcGesOrder.getGraph()), problem.getData());
        System.out.println("\n\nCalculating SHD HC OPTIMAL");
        double shdOptimal = shdTetrad.getValue(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), new Dag_n(hcOptimalOrder.getGraph()), problem.getData());
        System.out.println("\n\nCalculating SHD HC BAD");
        double shdBad = shdTetrad.getValue(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), new Dag_n(hcBadOrder.getGraph()), problem.getData());
  
        
        
        System.out.println("\n\nGES Score:     " + experiment.getBdeuScore() + ", \t MHD: " + experiment.getStructuralHamiltonDistanceValue() + ".0" + ", \t SHD: " + shdGESReal);
        System.out.println("HC GES Score:  " + gesScore + ", \t MHD: " + mhdGES + ", \t SHD: " + shdGES);
        System.out.println("Bad Score:     " + badScore + ", \t MHD: " + mhdBad + ", \t SHD: " + shdBad);
        System.out.println("Optimal Score: " + optimalScore + ", \t MHD: " + mhdOptimal + ", \t SHD: " + shdOptimal + "\n");
        
        
        Assert.assertTrue(gesScore + 0.000000001 >= experiment.getBdeuScore());
    }

    public static List<Node> randomOrder(Problem problem, int seed){
        List<Node> randomOrder = new ArrayList<>(problem.getVariables());
        Random random = new Random(seed);
        Collections.shuffle(randomOrder, random);
        return randomOrder;
    }

}
