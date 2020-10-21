package org.albacete.simd.utils;

import edu.cmu.tetrad.graph.*;
import org.albacete.simd.utils.TupleNode;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for the TupleNode class
 */
public class TupleNodeTest {

    /**
     * Tests that the constructor creates a TupleNode
     * @result A TupleNode is created.
     */
    @Test
    public void constructorTest(){
        // Arrange
        Node x = new GraphNode("X");
        Node y = new GraphNode("Y");

        // Act
        TupleNode actual = new TupleNode(x,y);

        // Assert
        assertEquals(x,actual.x);
        assertEquals(y,actual.y);
    }

    /**
     * Checks that two TupleNode are equal when two TupleNodes contain the same Nodes.
     * @result Both TupleNode are equal.
     */
    @Test
    public void equalsTest(){
        // Arrange
        Node x = new GraphNode("X");
        Node y = new GraphNode("Y");

        // Act
        TupleNode t1 = new TupleNode(x,y);
        TupleNode t2 = new TupleNode(x,y);

        // Arrange
        assertEquals(t1, t2);

    }

    /**
     * Checks that two TupleNode are equal when two TupleNode contain the same Nodes, but are in different order (X,Y) = (Y,X)
     * @result Both TupleNode are the same, even if they are reversed.
     */
    @Test
    public void equalsReverseTest(){
        // Arrange
        Node node1 = new GraphNode("X");
        Node node2= new GraphNode("Y");

        // Act
        TupleNode t1 = new TupleNode(node1,node2);
        TupleNode t2 = new TupleNode(node2,node1);

        // Arrange
        assertEquals(t1, t2);

    }

    /**
     * Checks that two TupleNode are not the same when the Nodes in them are not the same.
     * @result Two TupleNodes are not equal when all the components are not the same.
     */
    @Test
    public void notEqualTest(){
        // Arrange
        Node node1 = new GraphNode("X");
        Node node2 = new GraphNode("Y");

        // Act
        TupleNode t1 = new TupleNode(node1, node2);
        TupleNode t2 = new TupleNode(node1, node1);

        // Arrange
        assertNotEquals(t1, t2);

    }

    /**
     * Testing that a TupleNode and an Object are not equal.
     * @result TupleNode is not equal to an Object
     */
    @Test
    public void notEqualNotTupleTest(){
        // Arrange
        Node node1 = new GraphNode("X");
        Node node2 = new GraphNode("Y");
        Object y = new Object();

        // Act
        TupleNode t1 = new TupleNode(node1, node2);

        // Arrange
        assertNotEquals(t1, y);

    }

    /**
     * Testing that the toString method returns a String with the names of the nodes.
     * @result A string (X,Y) is returned by the toString method.
     */
    @Test
    public void toStringTest(){
        // Arrange
        Node x = new GraphNode("X");
        Node y = new GraphNode("Y");
        String expected = "(X, Y)";
        // Act
        TupleNode t1 = new TupleNode(x,y);
        String actual = t1.toString();

        //assert
        assertEquals(expected, actual);
    }
}