package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
@SuppressWarnings("SpellCheckingInspection")
public class MainTest
{
    //Nodes of Cancer Network
    final Node xray = new GraphNode("Xray");
    final Node dyspnoea = new GraphNode("Dyspnoea");
    final Node cancer = new GraphNode ("Cancer");
    final Node pollution = new GraphNode("Pollution");
    final Node smoker = new GraphNode("Smoker");

    /**
     * Testing csv reading
     */
    @Test
    public void readDataTest(){
        //Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        int num_cols = 5;
        List<Node> columns = new ArrayList<>(Arrays.asList(xray, dyspnoea, cancer, pollution, smoker));
        //Act
        Main main = new Main(path, 1);
        DataSet data = main.getData();
        int result = data.getNumColumns();
        //Assert
        assertEquals(num_cols, result);
        for (Node n: columns) {

            assertTrue(data.getVariableNames().contains(n.getName()));
        }
    }

    /**
     * Testing that all the possible arcs from a dataset are generated.
     * We check the size and the equality of the resulting arcs with its expected set.
     */
    @Test
    public void calculateArcsTest(){
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 1);

        TupleNode[] expected = new TupleNode[]{new TupleNode(xray, dyspnoea), new TupleNode(xray, cancer), new TupleNode(xray, pollution), new TupleNode(xray, smoker),
                new TupleNode(dyspnoea, cancer), new TupleNode(dyspnoea, pollution), new TupleNode(dyspnoea, smoker),
                new TupleNode(cancer, pollution), new TupleNode(cancer, smoker),
                new TupleNode(pollution, smoker),
                new TupleNode(dyspnoea, xray), new TupleNode(cancer, xray), new TupleNode(pollution, xray), new TupleNode(smoker, xray),
                new TupleNode(cancer, dyspnoea), new TupleNode(pollution, dyspnoea), new TupleNode(smoker, dyspnoea),
                new TupleNode(pollution, cancer), new TupleNode(smoker, cancer),
                new TupleNode(smoker, pollution)};
        // Act
        main.calculateArcs();
        TupleNode[] result =  main.getListOfArcs();

        // Assert
        // Asserting size
        assertEquals(expected.length, result.length);
        for (TupleNode tupleNode1 : expected) {
            boolean isEqual = false;
            for (TupleNode tupleNode2 : result) {
                if (tupleNode1.equals(tupleNode2))
                    isEqual = true;
            }
            assertTrue(isEqual);
        }
    }

    /**
     * Testing that the arcs have been splitted correctly
     */
    @Test
    public void splitArcsTest(){
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 2);

        // Act
        main.calculateArcs();
        main.splitArcs();
        TupleNode[] arcs = main.getListOfArcs();
        ArrayList<TupleNode>[] subsets = main.getSubSets();

        // Assert
        // Checking that each arc is in fact in a subset, and that it is only once in it.
        for (TupleNode edge : arcs) {
            int counter = 0;
            for (ArrayList<TupleNode> subset : subsets){
                counter += Collections.frequency(subset, edge);
            }
            // Double pairs
            assertEquals(2, counter);
        }


    }
    @Test
    public void setgetSeedTest(){
        // Arrange
        long newSeed = 21;
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 1);
        // Act
        main.setSeed(newSeed);
        // Assert
        assertEquals(newSeed, main.getSeed());
    }
}
