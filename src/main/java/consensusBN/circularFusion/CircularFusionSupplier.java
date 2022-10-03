package consensusBN.circularFusion;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.SearchGraphUtils;

import org.albacete.simd.utils.Problem;
import org.albacete.simd.framework.FESFusion;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.GESThread;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;
import org.albacete.simd.threads.FESThread;

public class CircularFusionSupplier implements Supplier<CircularDag> {

    private Dag currentDag = null;
    private final Dag inputDag;
    private CircularDag result = null;
    private Dag fusionDag;
    private Dag gesDag;
    private final Set<Edge> subsetEdges;
    private final int id;
    private final int nItInterleaving;

    private final Problem problem;
    
    private boolean fusionFlag;
    private boolean gesFlag;

    public CircularFusionSupplier(Problem problem, Dag initialDag, Dag inputDag, Set<Edge> subsetEdges, int id, int nItInterleaving){
        this.problem = problem;
        this.currentDag = initialDag;
        this.inputDag = inputDag;
        this.subsetEdges = subsetEdges;
        this.nItInterleaving = nItInterleaving;
        this.id = id;
    }

    @Override
    public CircularDag get() {
        if(result == null) {
            return run();
        }
        else{
            return new CircularDag(currentDag, id);
        }
    }

    private CircularDag run(){
        // In the first iteration, we only do runGES()
        if (!currentDag.equals(inputDag)) {
            fusionStage();
        } else {
            fusionDag = currentDag;
            fusionFlag = true;
        }
        runGES();
        return updateInitialDag();
    }

    private void fusionStage(){
        ArrayList<Dag> dags = new ArrayList<>();
        dags.add(currentDag);
        dags.add(inputDag);

        // Do the consesus fusion + FESThread
        FESFusion fusion = new FESFusion(problem, currentDag, dags);
        fusionDag = fusion.fusion();
        
        /*// Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, fusionDag, fusionDag.getEdges());
        bes.run();
        try {
            Graph graph = bes.getCurrentGraph();
            SearchGraphUtils.pdagToDag(graph);
            fusionDag = new Dag(graph);
        } catch (InterruptedException ex) {System.out.println("Cannot do the BES stage");}*/

        // Flag to see if fusion had changued the result
        if (fusionDag.equals(currentDag) || fusionDag.equals(inputDag)) {
            fusionFlag = true;
        } else fusionFlag = false;
    }

    private void runGES(){
        // Do the FESThread 
        FESThread fes = new FESThread(problem, fusionDag, subsetEdges, this.nItInterleaving);
        fes.run();
        try {
            Graph graph = fes.getCurrentGraph();
            SearchGraphUtils.pdagToDag(graph);
            gesDag = new Dag(graph);
        } catch (InterruptedException ex) {System.out.println("Cannot do the FES stage");}
        
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, gesDag, subsetEdges);
        bes.run();
        try {
            Graph graph = bes.getCurrentGraph();
            SearchGraphUtils.pdagToDag(graph);
            gesDag = new Dag(graph);
        } catch (InterruptedException ex) {System.out.println("Cannot do the BES stage");}
        
        /*GES_BNBuilder ges = new GES_BNBuilder(fusionDag, problem, subsetEdges);
        Graph resultingGraph = ges.search();
        gesFlag = ges.convergence();
        if(resultingGraph != null)
            gesDag = new Dag(resultingGraph);
        else
            System.out.println("Resulting graph was null");*/
    }

    private CircularDag updateInitialDag(){
        currentDag = new Dag(gesDag);
        result = new CircularDag(currentDag, id, hasConverged());
        result.setBDeu(GESThread.scoreGraph(currentDag, problem));
        return result;
    }

    public Dag getCurrentDag() {
        return currentDag;
    }

    private boolean hasConverged(){
        // Convergence in both GES and fusion
        return gesFlag && fusionFlag;
    }

}
