package org.albacete.simd.utils;

import edu.cmu.tetrad.graph.Node;

/**
 * TupleNode is a structure that represents a tuple of two nodes. It has two components, a {@link Node Node} x, and a {@link Node Node} y.
 */
public class TupleNode {
    /**
     * First {@link Node Node} of the tuple
     */
    public final Node x;

    /**
     * Second {@link Node Node} of the tuple
     */
    public final Node y;

    /**
     * Constructor of a TupleNode. It needs two {@link Node nodes}
     * @param x First {@link Node Node} of the tuple.
     * @param y Second {@link Node Node} of the tuple.
     */
    public TupleNode(Node x, Node y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Checks if two TupleNodes are equal or not
     * @param obj The other TupleNode that we are checking for equivalence.
     * @return true if they are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        //Casting object
        if (!(obj instanceof TupleNode)){
            return false;
        }
        TupleNode other = (TupleNode) obj;

        // Checking Equivalence
        return (other.x.getName().equals(this.x.getName()) && (other.y.getName().equals(this.y.getName())))
                || (other.x.getName().equals(this.y.getName()) && (other.y.getName().equals(this.x.getName())));
    }

    /**
     * Creates a {@link String String} with the values of the {@link Node Nodes} of the tuple.
     * @return {@link String String} with the information of the TupleNode.
     */
    @Override
    public String toString() {
        return "(" + this.x +", " + this.y + ")";
    }
}
