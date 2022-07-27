package org.albacete.simd.clustering;

import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.utils.Problem;


public abstract class ClusteringBES extends Clustering {
    
    Graph graph;
    
    public ClusteringBES(){
        super();
    }

    public ClusteringBES(Problem problem){
        super(problem);
    }
    
    public ClusteringBES(Problem problem, Graph graph){
        super(problem);
        this.graph = graph;
    }
    
    /**
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * @param graph the graph to set
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

}
