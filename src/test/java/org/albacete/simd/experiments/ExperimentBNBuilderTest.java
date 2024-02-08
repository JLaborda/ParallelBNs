package org.albacete.simd.experiments;

import org.albacete.simd.Resources;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.clustering.RandomClustering;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


public class ExperimentBNBuilderTest {

    Map<String,String> paramsMap = new HashMap<>();




    private ExperimentBNBuilder createExperiment(){
        
        int nThreads = 2;
        int nItInterleaving = 5;
        int maxIterations = 15;
        Clustering clustering = new HierarchicalClustering();
        BNBuilder algorithm = new PGESwithStages(Resources.CANCER_BBDD_PATH, clustering, nThreads, maxIterations, nItInterleaving, false, true, true);
        
        paramsMap.put("algName", "pges");
        paramsMap.put("netName", "cancer");
        paramsMap.put("clusteringName", "HierarchicalClustering");
        paramsMap.put("numberOfRealThreads", "2");
        paramsMap.put("databasePath", Resources.CANCER_BBDD_PATH);
        paramsMap.put("netPath", Resources.CANCER_NET_PATH);
        paramsMap.put("maxIterations", "15");
        
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, paramsMap);//"cancer", Resources.CANCER_NET_PATH, Resources.CANCER_BBDD_PATH, Resources.CANCER_TEST_PATH, seed);

        return exp;
    }

    @Before
    public void restartMeans() {
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void experimentsConstructorTest(){
        ExperimentBNBuilder exp = createExperiment();
        //Asserting
        assertNotNull(exp);
    }

    @Test
    public void runExperimentTest(){
        ExperimentBNBuilder exp = createExperiment();
        exp.runExperiment();

        assertNotEquals(0.0, exp.getBdeuScore(), 0.000001);
        assertNotNull(exp.getDifferencesOfMalkovsBlanket());
        assertNotEquals(0L,exp.getElapsedTimeMiliseconds());
        assertNotEquals(0,exp.getNumberOfIterations());
        assertNotEquals(Integer.MAX_VALUE,exp.getStructuralHamiltonDistanceValue());
        assertEquals("pges", exp.getAlgName());
        //String results = "PGESwithStages,res/alarm,alarm.xbif50001_,2,5,42,18,-0.47065998245296453,-56422.320053854455,1.1891891891891893,8.0,36.0,10,3\n";
        //System.out.println(exp.getResults());
//        assertTrue(exp.getResults().contains("PGESwithStages,src/test/res/alarm,alarm.xbif_"));
//
//        System.out.println(exp);
//        String exp_toString = "-----------------------\n" +
//                "Experiment PGESwithStages\n" +
//                "-----------------------\n" +
//                "Net Name: src/test/res/alarm\tDatabase: alarm.xbif_\tThreads: 2\tInterleaving: 5\tMax. Iterations: 15\n";
//        assertEquals(exp_toString, exp.toString());
//        exp.printResults();
    }


    @Test
    public void saveExperimentTest(){
        String savePath = "./testBN.txt";
        File file = new File(savePath);
        ExperimentBNBuilder exp = createExperiment();
        try {
            //Arrange: Creating Experiment and deleting previous file
            Files.deleteIfExists(file.toPath());
            exp.runExperiment();

            //Act: Saving Experiment
            exp.saveExperiment(savePath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Assert: Checking if the file has been saved
        File temp = new File(savePath);
        assertTrue(temp.exists());

        // Deleting again
        try {
            Files.deleteIfExists(temp.toPath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }




}
