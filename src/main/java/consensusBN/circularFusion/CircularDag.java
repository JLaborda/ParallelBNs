package consensusBN.circularFusion;

import edu.cmu.tetrad.graph.Dag;

public class CircularDag {
    public Dag dag;
    public int id;
    public boolean convergence = false;
    private double bdeu;

    public CircularDag(Dag dag, int id) {
        this.dag = dag;
        this.id = id;
    }

    public CircularDag(Dag dag, int id, boolean convergence) {
        this(dag, id);
        this.convergence = convergence;
    }

    /**
     * @return the bdeu
     */
    public double getBDeu() {
        return bdeu;
    }

    /**
     * @param bdeu the bdeu to set
     */
    public void setBDeu(double bdeu) {
        this.bdeu = bdeu;
    }
    
    
}
