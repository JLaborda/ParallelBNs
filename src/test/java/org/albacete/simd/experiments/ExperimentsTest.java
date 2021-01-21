package org.albacete.simd.experiments;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class ExperimentsTest {

    @Test
    public void ExperimentsConstructorTest(){
        String net_path = "./res/networks/cancer.xbif";
        String bbdd_path = "./res/networks/BBDD/cancer.xbif50000_.csv";
        int nThreads = 2;
        int nItInterleaving = 5;

        Experiment exp = new ExperimentPGES(net_path, bbdd_path, nThreads, nItInterleaving);

        //Asserting
        assertNotNull(exp);



    }

    @Test
    public void runExperiment(){
        String net_path = "./res/networks/cancer.xbif";
        String bbdd_path = "./res/networks/BBDD/cancer.xbif50000_.csv";
        int nThreads = 2;
        int nItInterleaving = 5;

        Experiment exp = new ExperimentPGES(net_path, bbdd_path, nThreads, nItInterleaving);
        exp.runExperiment();

        assertNotEquals(0.0, exp.getScore(), 0.000001);
        assertNotNull(exp.getDfmm());
        assertNotEquals(0L,exp.getElapsedTimeMiliseconds());
        assertNotEquals(0,exp.getnIterations());
        assertNotEquals(Integer.MAX_VALUE,exp.getShd());
    }


    @Test
    public void getNetworkPathsTest(){
        // Arrange
        String netFolder = "res/networks/";
        // Act
        ArrayList<String> net_paths = Experiment.getNetworkPaths(netFolder);
        // Assert
        assertTrue(net_paths.contains(netFolder + "munin.xbif"));
        assertFalse(net_paths.contains(netFolder + "munin1.net"));
    }
    @Test
    public void getBBDDPathsTest(){
        // Arrange
        String bbddFolder = "res/networks/BBDD/";
        // Act
        ArrayList<String> bbdd_paths = Experiment.getBBDDPaths(bbddFolder);
        // Assert
        assertTrue(bbdd_paths.contains(bbddFolder + "alarm.xbif50000_.csv"));
        assertFalse(bbdd_paths.contains(bbdd_paths + "munin1.net"));
    }

    @Test
    public void hashNetworksTest(){

        //TEST: Hashing correctly bbdd paths and net paths.
        //Arrange
        List<String> bbddPaths = Arrays.asList("res/networks/BBDD/alarm.xbif50000_.csv");
        List<String> netPaths = Arrays.asList("res/networks/alarm.xbif");


        //Act
        HashMap<String, HashMap<String, String>> result = Experiment.hashNetworks(netPaths, bbddPaths);

        //Assert
        assertFalse(result.isEmpty());
        assertEquals(1,result.size());
        assertTrue(result.containsKey("xbif50000_"));

        HashMap<String,String> result2 = result.get("xbif50000_");
        assertFalse(result2.isEmpty());
        assertEquals(1, result2.size());
        assertTrue(result2.containsKey(netPaths.get(0)));
        assertEquals(bbddPaths.get(0),result2.get(netPaths.get(0)));


        //TEST: When a bbdd path and a net path don't share the same name, they should not be assigned together.
        //Arrange
        bbddPaths = Arrays.asList("res/networks/BBDD/munin.xbif50000_.csv");
        netPaths = Arrays.asList("res/networks/alarm.net");

        //Act
        result = Experiment.hashNetworks(netPaths, bbddPaths);

        //Assert
        assertFalse(result.isEmpty());
        assertEquals(1,result.size());
        assertTrue(result.containsKey("xbif50000_"));

        result2 = result.get("xbif50000_");
        assertTrue(result2.isEmpty());
    }


    @Test
    public void saveExperimentTest(){
        //Arrange: Creating Experiment
        Experiment experiment = new ExperimentPGES("res/networks/cancer.xbif", "res/networks/BBDD/cancer.xbif_.csv", 2, 5);
        experiment.runExperiment();

        //Act: Saving Experiment
        experiment.saveExperiment();

        //Assert: Checking if the file has been saved
        String path = "./experiments/networks/cancer/pges/cancer.xbif_T2_I5_global_results.csv";

        File temp = new File(path);

        assertTrue(temp.exists());

    }





}
