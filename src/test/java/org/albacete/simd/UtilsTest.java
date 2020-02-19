package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test cases for the Utils class
 */
public class UtilsTest {


    /**
     * Tests that a Utils object can be created
     * @result  Utils object not null
     */
    @Test
    public void constructorTest(){
        //Arrange
        Utils utils;

        //Act
        utils = new Utils();

        //Assert
        assertNotNull(utils);
    }

    /**
     * Tests that the method split for tuple nodes splits an array of TupleNode into two subsets correctly.
     * @result An ArrayList with two subset of TupleNode
     */
    @Test
    public void splitTupleNodesTest(){
        //Arrange
        Node n1 = new GraphNode("n1");
        Node n2 = new GraphNode("n2");
        Node n3 = new GraphNode("n3");
        TupleNode[] tupleNodes = {new TupleNode(n1,n2), new TupleNode(n1,n3)};
        int seed = 42;
        int expectedSize = 2;

        //Act
        ArrayList<TupleNode>[] result = Utils.split(tupleNodes, 2, seed);

        //Assert
        assertEquals(expectedSize, result.length);

    }


    /**
     * Tests that the method split for edges splits a List of Edges into two subsets correctly.
     * @result An ArrayList with two subset of TupleNode
     */
    @Test
    public void splitEdgesTest(){
        //Arrange
        Node n1 = new GraphNode("n1");
        Node n2 = new GraphNode("n2");
        Node n3 = new GraphNode("n3");
        List<Edge> edges = Arrays.asList(new Edge(n1, n2, Endpoint.TAIL, Endpoint.ARROW),
                new Edge(n1, n3, Endpoint.TAIL, Endpoint.ARROW));
        int seed = 42;
        int expectedSize = 2;

        //Act
        ArrayList<TupleNode>[] result = Utils.split(edges, 2, seed);

        //Assert
        assertEquals(expectedSize, result.length);

    }

    /**
     * Tests that checks that an Edge is transformed to a TupleNode by using the method edgeToTupleNode.
     * @result Node1 is equal to Node x, and Node2 is equal to Node y from the Edge transformed and the resulting
     * TupleNode.
     */
    @Test
    public void edgeToTupleNodeTest(){
        // Arrange
        Node n1 = new GraphNode("n1");
        Node n2 = new GraphNode("n2");
        Edge edge = new Edge(n1, n2, Endpoint.TAIL, Endpoint.ARROW);

        // Act
        TupleNode result = Utils.edgeToTupleNode(edge);

        // Assert
        assertEquals(n1, result.x);
        assertEquals(n2, result.y);
    }

    /**
     * Tests that the method readData loads data correctly into a DataSet.
     * @result The DataSet is created and the number of columns and of rows correspond with what the data actually has.
     */
    @Test
    public void readDataTest(){
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";

        //Act
        DataSet result = Utils.readData(path);

        //Assert
        assertNotNull(result);
        assertEquals(5, result.getNumColumns());
        assertEquals(5000, result.getNumRows());

    }
}
