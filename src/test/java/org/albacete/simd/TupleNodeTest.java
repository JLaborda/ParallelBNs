package org.albacete.simd;

import edu.cmu.tetrad.graph.*;
import org.junit.Test;

import static org.junit.Assert.*;


public class TupleNodeTest {

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

    @Test
    public void equalsReverseTest(){
        // Arrange
        Node x = new GraphNode("X");
        Node y = new GraphNode("Y");

        // Act
        TupleNode t1 = new TupleNode(x,y);
        TupleNode t2 = new TupleNode(y,x);

        // Arrange
        assertEquals(t1, t2);

    }

    @Test
    public void notEqualTest(){
        // Arrange
        Node x = new GraphNode("X");
        Node y = new GraphNode("Y");

        // Act
        TupleNode t1 = new TupleNode(x,y);
        TupleNode t2 = new TupleNode(x,x);

        // Arrange
        assertNotEquals(t1, t2);

    }

    @Test
    public void notEqualNotTupleTest(){
        // Arrange
        Node x = new GraphNode("X");
        String y = "Y";

        // Act
        TupleNode t1 = new TupleNode(x,x);

        // Arrange
        assertNotEquals(t1, y);

    }

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
