package org.albacete.simd.framework;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.BackwardsHillClimbingThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.List;

public class BHCFusion extends FusionStage{

    public BHCFusion(Problem problem, Graph currentGraph, ArrayList<Dag> graphs) {
        super(problem, currentGraph, graphs);
    }

    @Override
    protected Dag fusion() throws InterruptedException {

        Dag fusionGraph = this.fusionIntersection();

        // Getting Scores
        double fusionScore = GESThread.scoreGraph(fusionGraph, problem);
        double currentScore = GESThread.scoreGraph(this.currentGraph, problem);

        System.out.println("Fusion Score: " + fusionScore);
        System.out.println("Current Score: " + currentScore);


        // Checking if the score has improved
        if (fusionScore > currentScore) {
            this.currentGraph = fusionGraph;
            return (Dag) this.currentGraph;
        }

        System.out.println("BHC to obtain the fusion: ");

        List<Edge> candidates = new ArrayList<>();

        for (Edge e: this.currentGraph.getEdges()){
            if(fusionGraph.getEdge(e.getNode1(), e.getNode2())==null && fusionGraph.getEdge(e.getNode2(),e.getNode1())==null ) {
                candidates.add(Edges.directedEdge(e.getNode1(),e.getNode2()));
                candidates.add(Edges.directedEdge(e.getNode2(),e.getNode1()));
            }
        }
        // Quizás sea mejor poner el BES
        //BESThread fuse = new BESThread(this.problem, this.currentGraph, candidates);
        BackwardsHillClimbingThread fuse = new BackwardsHillClimbingThread(this.problem,this.currentGraph,candidates);

        fuse.run();

        this.currentGraph = fuse.getCurrentGraph();
        System.out.println("Resultado del BHC de la fusion: "+ BackwardsHillClimbingThread.scoreGraph(this.currentGraph, problem));
        //this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
        //System.out.println("Resultado del BHC de la fusion tras removeInconsistencies: "+ BackwardsHillClimbingThread.scoreGraph(this.currentGraph, problem));

        return new Dag(this.currentGraph);

    }
}
