package consensusBN.circularFusion;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.SearchGraphUtils;
import org.albacete.simd.framework.FESFusion;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import static org.albacete.simd.utils.Utils.pdagToDag;

public class CircularDag {
    public Dag dag;
    public int id;
    public boolean convergence = false;
    
    private double bdeu = Double.NEGATIVE_INFINITY;
    private double lastBdeu;
    private Problem problem;
    private Set<Edge> subsetEdges;
    private int nItInterleaving;
    
    public static final String EXPERIMENTS_FOLDER = "./experiments/";

    public CircularDag(Problem problem, Set<Edge> subsetEdges, int nItInterleaving, int id) {
        this.id = id;
        this.problem = problem;
        this.subsetEdges = subsetEdges;
        this.nItInterleaving = nItInterleaving;
        this.dag = new Dag(problem.getVariables());
    }

    public void fusionGES(CircularDag inputDag) throws InterruptedException {

        // Setup
        // 1. Update bdeu and convergence variables
        setup();
        
        // 2. Check if the input dag is empty
        if (!inputDag.dag.getEdges().isEmpty()) {
            // 3. Merge dags into an arraylist
            ArrayList<Dag> dags = mergeBothDags(inputDag);

            // 4. FES Fusion (Consensus Fusion + FES)
            applyFESFusion(dags);
        }
        
        // 5. GES Stage
        Graph besGraph = applyGES();

        // 6. Transforming pdag from GES to a dag
        dag = transformPDAGtoDAG(besGraph);

        // 7. Update bdeu value
        updateResults();

        // 8. Convergence
        checkConvergence();
    }
    
    private void setup() {
        lastBdeu = bdeu;
        convergence = false;
    }
    
    private ArrayList<Dag> mergeBothDags(CircularDag dag2) {
        ArrayList<Dag> dags = new ArrayList<>();
        dags.add(dag);
        dags.add(dag2.dag);
        return dags;
    }
    
    private void applyFESFusion(ArrayList<Dag> dags)  throws InterruptedException {
        FESFusion fusion = new FESFusion(problem, dag, dags);
        dag = fusion.fusion();
        printResults(id, "Fusion", GESThread.scoreGraph(dag, problem));
        
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, dag, dag.getEdges());
        bes.run();
        Graph graph = bes.getCurrentGraph();
        pdagToDag(graph);
        dag = new Dag(graph);
    }

    private void printResults(int id, String stage, double BDeu) {
        String savePath = EXPERIMENTS_FOLDER + "temp_" + id + ".csv";
        File file = new File(savePath);
        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(file, true);
            csvWriter.append(id + "," + stage + "," + BDeu + "\n");
            csvWriter.flush();
        } catch (IOException ex) {
        }
    }

    private Graph applyGES() throws InterruptedException {
        // Do the FESThread
        FESThread fes = new FESThread(problem, dag, subsetEdges, this.nItInterleaving);
        fes.run();
        Graph fesGraph = fes.getCurrentGraph();
        fesGraph = transformPDAGtoDAG(fesGraph);
        printResults(id, "FES", GESThread.scoreGraph(fesGraph, problem));
    
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, fesGraph, subsetEdges);
        bes.run();
        Graph besGraph = bes.getCurrentGraph();
        besGraph = transformPDAGtoDAG(besGraph);
        return besGraph;
    }

    private Dag transformPDAGtoDAG(Graph besGraph) {
        pdagToDag(besGraph);
        return new Dag(besGraph);
    }
    
    private void updateResults() {
        bdeu = GESThread.scoreGraph(dag, problem);
        printResults(id, "BES", bdeu);
    }

    private void checkConvergence() {
        if (bdeu <= lastBdeu) convergence = true;
    }
    
    public double getBDeu() {
        return bdeu;
    }

    public void setBDeu(double bdeu) {
        this.bdeu = bdeu;
    }
}