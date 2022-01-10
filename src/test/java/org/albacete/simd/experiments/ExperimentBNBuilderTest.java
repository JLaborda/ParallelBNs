package org.albacete.simd.experiments;

import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.framework.BNBuilder;
import org.albacete.simd.Resources;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ExperimentBNBuilderTest {
    int nThreads = 2;
    int nItInterleaving = 5;
    int seed = 42;
    int maxIterations = 15;

    BNBuilder algorithm = new PGESwithStages(Resources.ALARM_BBDD_PATH, nThreads, maxIterations, nItInterleaving);
    ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH, Resources.ALARM_TEST_PATH, seed);


    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void experimentsConstructorTest(){

        //Asserting
        assertNotNull(exp);
    }

    @Test
    public void runExperimentTest(){
        exp.runExperiment();

        assertNotEquals(0.0, exp.getScore(), 0.000001);
        assertNotNull(exp.getDfmm());
        assertNotEquals(0L,exp.getElapsedTimeMiliseconds());
        assertNotEquals(0,exp.getnIterations());
        assertNotEquals(Integer.MAX_VALUE,exp.getShd());
        assertEquals("PGESwithStages", exp.getAlgName());
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
