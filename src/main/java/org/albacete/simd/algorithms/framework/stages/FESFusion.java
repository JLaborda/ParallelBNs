package org.albacete.simd.algorithms.framework.stages;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class FESFusion extends FusionStage{

    public FESFusion(Problem problem, Graph currentGraph, ArrayList<Dag> graphs) {
        super(problem, currentGraph, graphs);
    }

    @Override
    protected Dag fusion() {
        // Applying ConsensusUnion fusion
        ConsensusUnion fusion = new ConsensusUnion(this.graphs);
        Graph fusionGraph = fusion.union();

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

        System.out.println("FES to obtain the fusion: ");


        List<Edge> candidates = new ArrayList<>();


        for (Edge e: fusionGraph.getEdges()){
            if(this.currentGraph.getEdge(e.getNode1(), e.getNode2())!=null || this.currentGraph.getEdge(e.getNode2(),e.getNode1())!=null ) continue;
            candidates.add(Edges.directedEdge(e.getNode1(),e.getNode2()));
            candidates.add(Edges.directedEdge(e.getNode2(),e.getNode1()));
        }


        FESThread fuse = new FESThread(this.problem,this.currentGraph,candidates,candidates.size());

        fuse.run();

        try {
            this.currentGraph = fuse.getCurrentGraph();
            System.out.println("Score Fusion: "+ FESThread.scoreGraph(this.currentGraph, problem));
            this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
            System.out.println("Score Fusion sin inconsistencias: "+ FESThread.scoreGraph(this.currentGraph, problem));
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        return new Dag(this.currentGraph);
    }
}
