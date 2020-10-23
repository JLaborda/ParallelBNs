package org.albacete.simd.algorithms;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.algorithms.ParallelHillClimbingSearch;
import org.albacete.simd.utils.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ParallelHillClimbingSearchTest {

    /**
     * String containing the path to the data used in the test. The data used in these tests is made by sampling the
     * cancer Bayesian Network @see
     * <a href="https://www.bnlearn.com/bnrepository/discrete-small.html">https://www.bnlearn.com/bnrepository/discrete-small.html</a>
     */
    final String path = "src/test/resources/cancer.xbif_.csv";
    /**
     * Dataset created from the data file
     */
    final DataSet dataset = Utils.readData(path);
    /**
     * Variable X-Ray
     */
    final Node xray = dataset.getVariable("Xray");
    /**
     * Variable Dysponea
     */
    final Node dyspnoea = dataset.getVariable("Dyspnoea");
    /**
     * Variabe Cancer
     */
    final Node cancer = dataset.getVariable("Cancer");
    /**
     * Variable Pollution
     */
    final Node pollution = dataset.getVariable("Pollution");
    /**
     * Variable Smoker
     */
    final Node smoker = dataset.getVariable("Smoker");

    /**
     * Subset1 of pairs of nodes or variables.
     */
    final List<Edge> subset1 = new ArrayList<>();
    /**
     * Subset2 of pairs of nodes or variables.
     */
    final List<Edge> subset2 = new ArrayList<>();


    /**
     * This method initializes the subsets, splitting the nodes in what is expected to happen when the seed is 42
     */


    /**
     * Testing both possible constructors of the Main class
     * @result Both objects should have the same dataset stored in it.
     */
    @Test
    public void constructorAndGettersTest(){
        // Arrange
        int num_cols = 5;

        // Act
        ParallelHillClimbingSearch phc1 = new ParallelHillClimbingSearch(path, 1);
        ParallelHillClimbingSearch phc2 = new ParallelHillClimbingSearch(dataset, 2);
        ParallelHillClimbingSearch phc3 = new ParallelHillClimbingSearch(path,4, 30, 8);
        ParallelHillClimbingSearch phc4 = new ParallelHillClimbingSearch(dataset,8, 35, 10);

        DataSet data1 = phc1.getData();
        DataSet data2 = phc2.getData();
        DataSet data3 = phc3.getData();
        DataSet data4 = phc4.getData();

        int threads1 = phc1.getnThreads();
        int threads2 = phc2.getnThreads();
        int threads3 = phc3.getnThreads();
        int threads4 = phc4.getnThreads();

        int maxIterations1 = phc1.getMaxIterations();
        int maxIterations2 = phc2.getMaxIterations();
        int maxIterations3 = phc3.getMaxIterations();
        int maxIterations4 = phc4.getMaxIterations();

        int interleaving1 = phc1.getnItInterleaving();
        int interleaving2 = phc2.getnItInterleaving();
        int interleaving3 = phc3.getnItInterleaving();
        int interleaving4 = phc4.getnItInterleaving();

        // Assert
        assertNotNull(phc1);
        assertNotNull(phc2);
        assertNotNull(phc3);
        assertNotNull(phc4);
        assertEquals(num_cols, data1.getNumColumns());
        assertEquals(num_cols, data2.getNumColumns());
        assertEquals(num_cols, data3.getNumColumns());
        assertEquals(num_cols, data4.getNumColumns());
        assertEquals(1, threads1);
        assertEquals(2, threads2);
        assertEquals(4, threads3);
        assertEquals(8, threads4);
        assertEquals(15, maxIterations1);
        assertEquals(15, maxIterations2);
        assertEquals(30, maxIterations3);
        assertEquals(35, maxIterations4);
        assertEquals(5, interleaving1);
        assertEquals(5, interleaving2);
        assertEquals(8, interleaving3);
        assertEquals(10, interleaving4);
    }

    /**
     * Testing that the csv read is correct
     * @result The resulting DataSet should be the one corresponding to the cancer DataSet.
     */
    @Test
    public void readDataTest(){
        //Arrange
        int num_cols = 5;
        List<Node> columns = new ArrayList<>(Arrays.asList(xray, dyspnoea, cancer, pollution, smoker));
        //Act
        ParallelHillClimbingSearch alg = new ParallelHillClimbingSearch(path, 1);
        DataSet data = alg.getData();
        int result = data.getNumColumns();
        //Assert
        assertEquals(num_cols, result);
        for (Node n: columns) {
            assertTrue(data.getVariableNames().contains(n.getName()));
        }
    }

    /**
     * When an incorrect path is passed to the Main constructor, an exception should be thrown
     * @result An exception is thrown caused by an incorrect path.
     */
    @Test(expected = Exception.class)
    public void exceptionReadDataTest(){
        String path = "";
        //noinspection unused
        ParallelHillClimbingSearch alg = new ParallelHillClimbingSearch(path, 1);
        fail();
    }

    


   
    /**
     * Testing the setter and getter of MaxIterations
     * @result MaxIterations is changed to 21 and obtained as such.
     */
    @Test
    public void setgetMaxIterations(){
        // Arrange
        int expected = 21;
        ParallelHillClimbingSearch alg = new ParallelHillClimbingSearch(path, 1);
        // Act
        alg.setMaxIterations(21);
        int actual = alg.getMaxIterations();
        // Assert
        assertEquals(expected, actual);
    }

    /**
     * Testing the setter and getter of Seed
     * @result Seed is changed to 21 and obtained as such.
     */
    @Test
    public void setgetSeedTest(){
        // Arrange
        long newSeed = 21;
        ParallelHillClimbingSearch pGESv2 = new ParallelHillClimbingSearch(path, 1);
        // Act
        pGESv2.setSeed(newSeed);
        // Assert
        assertEquals(newSeed, pGESv2.getSeed());
    }


    /**
     * Tests the search method of the Main class.
     * @result The resulting graph is equal to the expected graph for the cancer dataset.
     */
    @Test
    public void searchCancerTest(){
        //Arrange
        ParallelHillClimbingSearch pGESv2 = new ParallelHillClimbingSearch(path, 2);

        //Expectation
        List<Node> nodes = Arrays.asList(cancer, dyspnoea, pollution, xray, smoker);
        EdgeListGraph expectation = new EdgeListGraph(nodes);
        expectation.addDirectedEdge(cancer, dyspnoea);
        expectation.addDirectedEdge(cancer, pollution);
        expectation.addDirectedEdge(cancer, smoker);
        expectation.addDirectedEdge(xray, cancer);

        // Act
        pGESv2.search();

        //Assert
        assertEquals(expectation, pGESv2.getCurrentGraph());

    }


    @Test
    public void gettersTest(){
        ParallelHillClimbingSearch phc1 = new ParallelHillClimbingSearch(dataset, 1);

        assertEquals(5*4/2, phc1.getListOfArcs().length);
        assertEquals(1, phc1.getSubSets().length);
        assertNull(phc1.getGraphs());
        assertEquals(1, phc1.getIterations());
        assertEquals(dataset, phc1.getProblem().getData());


    }


}
