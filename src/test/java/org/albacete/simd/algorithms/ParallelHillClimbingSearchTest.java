package org.albacete.simd.algorithms;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.Resources;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.albacete.simd.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ParallelHillClimbingSearchTest {

    /**
     * Dataset created from the data file
     */
    final DataSet cancerDataset = Utils.readData(Resources.CANCER_BBDD_PATH);
    /**
     * Variable X-Ray
     */
    final Node xray = cancerDataset.getVariable("Xray");
    /**
     * Variable Dysponea
     */
    final Node dyspnoea = cancerDataset.getVariable("Dyspnoea");
    /**
     * Variabe Cancer
     */
    final Node cancer = cancerDataset.getVariable("Cancer");
    /**
     * Variable Pollution
     */
    final Node pollution = cancerDataset.getVariable("Pollution");
    /**
     * Variable Smoker
     */
    final Node smoker = cancerDataset.getVariable("Smoker");

    /**
     * Subset1 of pairs of nodes or variables.
     */
    final List<Edge> subset1 = new ArrayList<>();
    /**
     * Subset2 of pairs of nodes or variables.
     */
    final List<Edge> subset2 = new ArrayList<>();

    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    /**
     * Testing both possible constructors of the Main class
     * @result Both objects should have the same dataset stored in it.
     */
    @Test
    public void constructorAndGettersTest(){
        // Arrange
        int num_cols = 5;

        // Act
        ParallelHillClimbingSearch phc1 = new ParallelHillClimbingSearch(Resources.CANCER_BBDD_PATH, 1);
        ParallelHillClimbingSearch phc2 = new ParallelHillClimbingSearch(cancerDataset, 2);
        ParallelHillClimbingSearch phc3 = new ParallelHillClimbingSearch(Resources.CANCER_BBDD_PATH,4, 30, 8);
        ParallelHillClimbingSearch phc4 = new ParallelHillClimbingSearch(cancerDataset,8, 35, 10);

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
        ParallelHillClimbingSearch alg = new ParallelHillClimbingSearch(Resources.CANCER_BBDD_PATH, 1);
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
        new ParallelHillClimbingSearch(path, 1);
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
        ParallelHillClimbingSearch alg = new ParallelHillClimbingSearch(Resources.CANCER_BBDD_PATH, 1);
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
        ParallelHillClimbingSearch pGESv2 = new ParallelHillClimbingSearch(Resources.CANCER_BBDD_PATH, 1);
        // Act
        pGESv2.setSeed(newSeed);
        // Assert
        assertEquals(newSeed, pGESv2.getSeed());
    }


    /**
     * Tests the search method of the Main class.
     * @result The resulting graph is equal to the expected graph for the cancer dataset.
     */
    /*
    @Test
    public void searchCancerTest(){
        //Arrange
        Utils.setSeed(42);
        ForwardStage.meanTimeTotal=0;
        BackwardStage.meanTimeTotal=0;
        ParallelHillClimbingSearch alg = new ParallelHillClimbingSearch(Resources.CANCER_BBDD_PATH, 2);

        //Expectation
        List<Node> nodes = Arrays.asList(cancer, dyspnoea, pollution, xray, smoker);
        EdgeListGraph expectation = new EdgeListGraph(nodes);
        expectation.addDirectedEdge(cancer, dyspnoea);
        expectation.addDirectedEdge(cancer, xray);
        expectation.addDirectedEdge(pollution, cancer);
        expectation.addDirectedEdge(smoker, cancer);

        // Act
        alg.search();

        //Assert
        assertEquals(expectation, alg.getCurrentGraph());

    }
*/
    @Test
    public void searchAlarmTest(){
        //Arrange
        Utils.setSeed(42);
        ForwardStage.meanTimeTotal=0;
        BackwardStage.meanTimeTotal=0;
        String alarmPath = Resources.CANCER_BBDD_PATH;
        ParallelHillClimbingSearch pGESv2 = new ParallelHillClimbingSearch(alarmPath, 2);

        // Act
        pGESv2.search();

        //Assert
        assertNotEquals(null, pGESv2.getCurrentGraph());
        assertNotEquals(1, pGESv2.getIterations());
    }


    @Test
    public void gettersTest(){
        Utils.setSeed(42);
        ForwardStage.meanTimeTotal=0;
        BackwardStage.meanTimeTotal=0;
        ParallelHillClimbingSearch phc1 = new ParallelHillClimbingSearch(cancerDataset, 1);
        phc1.search();
        assertEquals(5 * 4, phc1.getSetOfArcs().size());
        assertEquals(1, phc1.getSubSets().size());
        assertEquals(1,phc1.getGraphs().size());
        assertNotEquals(1, phc1.getIterations());
        assertEquals(cancerDataset, phc1.getProblem().getData());


    }
    @Test
    public void convergenceTest(){
        //Arrange
        Utils.setSeed(42);
        ForwardStage.meanTimeTotal=0;
        BackwardStage.meanTimeTotal=0;
        DataSet datasetAlarm = Utils.readData(Resources.CANCER_BBDD_PATH);
        ParallelHillClimbingSearch phc1 = new ParallelHillClimbingSearch(datasetAlarm, 1);
        phc1.setMaxIterations(2);
        //Act
        phc1.search();
        //Assert
        assertEquals(2,phc1.getIterations());

    }

    @Test
    public void interruptedExceptionTest(){
        DataSet datasetAlarm = Utils.readData(Resources.CANCER_BBDD_PATH);
        ParallelHillClimbingSearch phc1 = new ParallelHillClimbingSearch(datasetAlarm, 1);
        phc1.setMaxIterations(30);
        phc1.search();
    }


}
