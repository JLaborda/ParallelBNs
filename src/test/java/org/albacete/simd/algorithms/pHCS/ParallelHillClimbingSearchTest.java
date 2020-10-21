package org.albacete.simd.algorithms.pHCS;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
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
    public void constructorTest(){
        // Arrange
        int num_cols = 5;

        // Act
        ParallelHillClimbingSearch alg1 = new ParallelHillClimbingSearch(path, 1);
        ParallelHillClimbingSearch alg2 = new ParallelHillClimbingSearch(dataset, 1);
        //ParallelHillClimbingSearch alg3 = new ParallelHillClimbingSearch(path, 1, 30, 15);
        //ParallelHillClimbingSearch alg4 = new ParallelHillClimbingSearch(dataset, 1, 30, 15);
        DataSet data1 = alg1.getData();
        DataSet data2 = alg2.getData();
        //DataSet data3 = alg3.getData();
        //DataSet data4 = alg4.getData();

        // Assert
        assertNotNull(alg1);
        assertNotNull(alg2);
        //assertNotNull(alg3);
        //assertNotNull(alg4);
        assertEquals(num_cols, data1.getNumColumns());
        assertEquals(num_cols, data2.getNumColumns());
        //assertEquals(num_cols, data3.getNumColumns());
        //assertEquals(num_cols, data4.getNumColumns());
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

    /**
     * Executes the main function in order to see that everything is working, and that no exceptions are being thrown.
     * @result No exception is thrown.
     */


}
