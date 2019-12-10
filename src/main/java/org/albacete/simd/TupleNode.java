package org.albacete.simd;

import edu.cmu.tetrad.graph.Node;

public class TupleNode {
    public final Node x;
    public final Node y;
    public TupleNode(Node x, Node y) {
        this.x = x;
        this.y = y;
    }

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

    @Override
    public String toString() {
        return "(" + this.x +", " + this.y + ")";
    }
}
