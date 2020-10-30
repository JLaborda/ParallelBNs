package org.albacete.simd.algorithms.framework.stages;

import edu.cmu.tetrad.graph.*;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class BESFusion extends FusionStage{
    public BESFusion(Problem problem, Graph currentGraph, ArrayList<Dag> graphs) {
        super(problem, currentGraph, graphs);
    }

    @Override
    protected Dag fusion() {
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

        System.out.println("BES to obtain the fusion: ");

        List<Edge> candidates = new ArrayList<>();

        for (Edge e: this.currentGraph.getEdges()){
            if(fusionGraph.getEdge(e.getNode1(), e.getNode2())==null && fusionGraph.getEdge(e.getNode2(),e.getNode1())==null ) {
                candidates.add(Edges.directedEdge(e.getNode1(),e.getNode2()));
                candidates.add(Edges.directedEdge(e.getNode2(),e.getNode1()));
            }
        }



        BESThread fuse = new BESThread(this.problem,this.currentGraph,candidates);

        fuse.run();

        try {
            this.currentGraph = fuse.getCurrentGraph();
            System.out.println("Resultado del BES de la fusion: "+ BESThread.scoreGraph(this.currentGraph, problem));
            this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
            System.out.println("Score Fusion sin inconsistencias: "+ BESThread.scoreGraph(this.currentGraph, problem));
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        return new Dag(this.currentGraph);
    }
}
