package org.albacete.simd.algorithms.GES;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.LocalScoreCache;
import org.albacete.simd.algorithms.pGESv2.GESThread;
import org.albacete.simd.algorithms.pGESv2.ThBES;
import org.albacete.simd.algorithms.pGESv2.ThFES;
import org.albacete.simd.algorithms.pGESv2.TupleNode;
import org.albacete.simd.utils.Utils;

import java.util.*;

public class GES {

    private ThFES fes;
    private ThBES bes;
    private Graph initialDag;
    private DataSet data;
    protected String[] varNames;
    protected List<Node> variables;
    protected static int[] nValues;
    protected final LocalScoreCache localScoreCache = new LocalScoreCache();
    private long elapsedTime;
    private double modelBDeu;

    protected double samplePrior = 10.0;
    protected double structurePrior = 0.001;
    protected int numTotalCalls=0;
    protected int numNonCachedCalls=0;
    private int maxIt = 15;
    private ArrayList<TupleNode> combinations = new ArrayList<>();
    private Graph graph;



    public GES(DataSet dataSet){

        List<String> _varNames = dataSet.getVariableNames();

        this.data = dataSet;
        this.varNames = _varNames.toArray(new String[0]);
        this.variables = dataSet.getVariables();
        GESThread.setCases(data);

        // Getting the complete set of arc combinations.
        TupleNode[] arcs = Utils.calculateArcs(data);
        Collections.addAll(combinations, arcs);

        this.initialDag = new EdgeListGraph(new LinkedList<>(variables));

    }

    public GES(DataSet dataSet, Graph initialDag){
        this(dataSet);
        this.initialDag = initialDag;
    }

    /**
     * Greedy equivalence search: Start from the empty graph, add edges till
     * model is significant. Then start deleting edges till a minimum is
     * achieved.
     *
     * @return the resulting Pattern.
     */
    public Graph search() throws InterruptedException {
        long startTime = System.currentTimeMillis();


        //buildIndexing(graph);

        // Method 1-- original.
        double score = 0;

        // Doing forward search
        System.out.println("FES stage: ");
        fes = new ThFES(data, combinations,100);
        fes.run();
        graph = fes.getCurrentGraph();


        // Doing backward search
        System.out.println("BES stage: ");
        bes = new ThBES(data, graph, combinations);
        bes.run();
        graph = bes.getCurrentGraph();
        score = bes.getScoreBDeu();

        // Transforming the resulting graph into a DAG
        graph = Utils.removeInconsistencies(graph);

        // Measuring stats
        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;


        return graph;
    }


    public synchronized Graph getCurrentGraph() throws InterruptedException {
        while(graph == null){
            wait();
        }
        return this.graph;
    }


}
