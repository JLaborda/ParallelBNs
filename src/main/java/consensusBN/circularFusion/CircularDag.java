package consensusBN.circularFusion;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.SearchGraphUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import org.albacete.simd.framework.FESFusion;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;

public class CircularDag {
    public Dag dag;
    public int id;
    public boolean convergence = false;
    
    private double bdeu;
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
    
    public CircularDag(Dag dag, int id) {
        this.dag = dag;
        this.id = id;
    }

    public CircularDag(Dag dag, int id, boolean convergence) {
        this(dag, id);
        this.convergence = convergence;
    }
    
    public void fusionGES(CircularDag dag2){
        lastBdeu = bdeu;
        convergence = false;
        
        ArrayList<Dag> dags = new ArrayList<>();
        dags.add(dag);
        dags.add(dag2.dag);

        // Do the consesus fusion + FESThread
        FESFusion fusion = new FESFusion(problem, dag, dags);
        dag = fusion.fusion();
        printResults(id,"Fusion",GESThread.scoreGraph(dag, problem));
        
        // Do the FESThread 
        FESThread fes = new FESThread(problem, dag, subsetEdges, this.nItInterleaving);
        fes.run();
        try {
            Graph graph = fes.getCurrentGraph();
            SearchGraphUtils.pdagToDag(graph);
            dag = new Dag(graph);
        } catch (InterruptedException ex) {System.out.println("Cannot do the FES stage");}
        printResults(id,"FES",GESThread.scoreGraph(dag, problem));
        
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, dag, subsetEdges);
        bes.run();
        try {
            Graph graph = bes.getCurrentGraph();
            SearchGraphUtils.pdagToDag(graph);
            dag = new Dag(graph);
        } catch (InterruptedException ex) {System.out.println("Cannot do the BES stage");}
        
        // Update bdeu value
        bdeu = GESThread.scoreGraph(dag, problem);
        printResults(id,"BES",bdeu);
        
        if (bdeu == lastBdeu) convergence = true;
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
    
    private void printResults(int id, String stage, double BDeu) {
        String savePath = EXPERIMENTS_FOLDER + "temp_" + id + ".csv";
        File file = new File(savePath);
        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(file,true);
            csvWriter.append(id + "," + stage + "," + BDeu + "\n");
            csvWriter.flush();
        } catch (IOException ex) {}
    }
    
}
