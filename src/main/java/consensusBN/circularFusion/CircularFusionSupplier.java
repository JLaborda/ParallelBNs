package consensusBN.circularFusion;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;

public class CircularFusionSupplier implements Supplier<CircularDag> {

    private static final int MAX_FES_ITERATIONS = 1000;

    private Dag currentDag = null;
    private final Dag inputDag;
    private CircularDag result = null;
    private Dag fusionDag;
    private Dag gesDag;
    private final Set<Edge> subsetEdges;
    private int id;

    private Problem problem;

    private boolean fusionFlag;
    private boolean gesFlag;

    CircularFusionSupplier(Problem problem, Dag initialDag, Dag inputDag, Set<Edge> subsetEdges, int id){
        this.problem = problem;
        this.currentDag = initialDag;
        this.inputDag = inputDag;
        this.subsetEdges = subsetEdges;
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
        fusionStage();
        runGES();
        return updateInitialDag();
    }

    private void fusionStage(){
        ArrayList<Dag> dags = new ArrayList<>();
        dags.add(currentDag);
        dags.add(inputDag);
        ConsensusUnion bnFusionMethod = new ConsensusUnion(dags);
        fusionDag = bnFusionMethod.union();
        //TODO: ¿Hay que añadir BES intersección?
        //TODO: Calcular la convergencia de la fusión
    }

    private void runGES(){
        GES_BNBuilder ges = new GES_BNBuilder(fusionDag, problem, subsetEdges);
        Graph resultingGraph = ges.search();
        gesFlag = ges.convergence();
        if(resultingGraph != null)
            gesDag = new Dag(resultingGraph);
        else
            System.out.println("Resulting graph was null");
    }

    private CircularDag updateInitialDag(){
        currentDag = new Dag(gesDag);
        result = new CircularDag(currentDag, id, hasConverged());
        return result;
    }

    public Dag getCurrentDag() {
        return currentDag;
    }

    private boolean hasConverged(){
        // No changes in either ges or fusion
        return !(gesFlag || fusionFlag);
    }



}
