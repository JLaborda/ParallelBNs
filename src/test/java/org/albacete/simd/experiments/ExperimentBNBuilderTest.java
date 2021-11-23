package org.albacete.simd.experiments;

import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.framework.BNBuilder;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ExperimentBNBuilderTest {
    String net_path = "./res/networks/alarm.xbif";
    String bbdd_path = "./res/networks/BBDD/alarm.xbif50001_.csv";
    String test_path = "./res/networks/BBDD/tests/alarm_test.csv";
    int nThreads = 2;
    int nItInterleaving = 5;
    int seed = 42;
    int maxIterations = 15;

    BNBuilder algorithm = new PGESwithStages(bbdd_path, nThreads, maxIterations, nItInterleaving);
    ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, net_path, bbdd_path,test_path, seed);


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
        assertTrue(exp.getResults().contains("PGESwithStages,res/alarm,alarm.xbif50001_"));

        String exp_toString = "-----------------------\nExperiment " + "PGESwithStages" + "\n-----------------------\nNet Name: " + "res/alarm" + "\tDatabase: " + "alarm.xbif50001_" + "\tThreads: " + nThreads + "\tInterleaving: " + nItInterleaving + "\tMax. Iterations: " + maxIterations;
        assertEquals(exp_toString, exp.toString());
        exp.printResults();
    }


    @Test
    public void saveExperimentTest(){
        String savePath = "./results/testBN.txt";
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
