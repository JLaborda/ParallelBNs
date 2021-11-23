package org.albacete.simd.experiments;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;


import static org.junit.Assert.*;

public class ExperimentsTest {
    String net_path = "./res/networks/alarm.xbif";
    String bbdd_path = "./res/networks/BBDD/alarm.xbif50001_.csv";
    String test_path = "./res/networks/BBDD/tests/alarm_test.csv";
    int nThreads = 2;
    int nItInterleaving = 5;
    int seed = 42;
    int maxIterations = 15;


    @Test
    public void ExperimentsConstructorTest(){

        Experiment exp = new ExperimentPGES(net_path, bbdd_path, test_path, nThreads, maxIterations, nItInterleaving, seed);

        //Asserting
        assertNotNull(exp);



    }

    @Test
    public void runExperiment(){
        Experiment exp = new ExperimentPGES(net_path, bbdd_path, test_path, nThreads, maxIterations, nItInterleaving, seed);
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

    /*
    @Test
    public void getBBDDPathsTest(){
        // Arrange
        String bbddFolder = "res/networks/BBDD/";
        // Act
        ArrayList<String> bbdd_paths = Experiment.getBBDDPaths(bbddFolder);
        // Assert
        assertTrue(bbdd_paths.contains(bbddFolder + "alarm.xbif50001_.csv"));
        assertFalse(bbdd_paths.contains(bbdd_paths + "munin1.net"));
    }


    @Test
    public void hashNetworksTest(){

        //TEST: Hashing correctly bbdd paths and net paths.
        //Arrange
        List<String> bbddPaths = Arrays.asList("res/networks/BBDD/alarm.xbif50001_.csv");
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
        bbddPaths = Arrays.asList("res/networks/BBDD/munin_test.csv");
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
*/

    @Test
    public void saveExperimentTest(){
        String savePath = "./results/test.txt";
        File file = new File(savePath);
        try {
            //Arrange: Creating Experiment and deleting previous file
            Files.deleteIfExists(file.toPath());
            Experiment experiment = new ExperimentPGES(net_path, bbdd_path, test_path, nThreads, maxIterations, nItInterleaving, seed);
            experiment.runExperiment();
            String results = experiment.getResults();
            
            //Act: Saving Experiment
            Experiment.saveExperiment(savePath, results);
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
