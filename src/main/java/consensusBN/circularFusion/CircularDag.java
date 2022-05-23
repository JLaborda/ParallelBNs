package consensusBN.circularFusion;

import edu.cmu.tetrad.graph.Dag;

public class CircularDag {
    public Dag dag;
    public int id;
    public boolean convergence = false;

    public CircularDag(Dag dag, int id) {
        this.dag = dag;
        this.id = id;
    }

    public CircularDag(Dag dag, int id, boolean convergence) {
        this(dag, id);
        this.convergence = convergence;
    }
}
