package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
        DataSet data = main.data;
        int result = data.getNumColumns();
        //Assert
        assertEquals(num_cols, result);
        for (Node n: columns) {

            assertTrue(data.getVariableNames().contains(n.getName()));
        }
    }

    @Test
    public void calculateArcsTest(){
        //Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        Main main = new Main(path, 1);

        //Nodes:
        /*ArrayList<Tuple<Node,Node>> expected = new ArrayList<>(Arrays.asList(new Tuple(xray, dyspnoea), new Tuple(xray, cancer), new Tuple(xray, pollution), new Tuple(xray, smoker),
                new Tuple(dyspnoea, cancer), new Tuple(dyspnoea, pollution), new Tuple(dyspnoea, smoker),
                new Tuple(cancer, pollution), new Tuple(cancer, smoker),
                new Tuple(pollution, smoker)));
        */


        TupleNode[] expected = new TupleNode[]{new TupleNode(xray, dyspnoea), new TupleNode(xray, cancer), new TupleNode(xray, pollution), new TupleNode(xray, smoker),
                new TupleNode(dyspnoea, cancer), new TupleNode(dyspnoea, pollution), new TupleNode(dyspnoea, smoker),
                new TupleNode(cancer, pollution), new TupleNode(cancer, smoker),
                new TupleNode(pollution, smoker)};
        //Act
        main.calculateArcs();
        TupleNode[] result =  main.listOfArcs;
        //Assert
        assertEquals(expected.length, result.length);

        // ERROR: I need to check equality between nodes...
        for (TupleNode tupleNode1 : expected) {
            boolean isEqual = false;
            for (TupleNode tupleNode2 : result) {
                if (tupleNode1.equals(tupleNode2))
                    isEqual = true;
            }
            assertTrue(isEqual);
        }
        //assertTrue(expected.equals(result));

    }
}
