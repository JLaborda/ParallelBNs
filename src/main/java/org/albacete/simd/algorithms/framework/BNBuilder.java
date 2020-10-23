package org.albacete.simd.algorithms.framework;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.TupleNode;
import org.albacete.simd.utils.Utils;

import java.util.ArrayList;

// Ideas para el futuro
public abstract class BNBuilder {
    /**
     * {@link DataSet DataSet}DataSet containing the values of the variables of the problem in hand.
     */
    //private final DataSet data;
    private Problem problem;
    /**
     * The number of threads the algorithm is going to use.
     */
    private int nThreads = 1;

    /**
     * Seed for the random number generator.
     */
    private long seed = 42;

    /**
     * The maximum number of iterations allowed for the algorithm.
     */
    private int maxIterations = 15;

    /**
     * The {@link GESThread GESThread} array that will be executed in each stage.
     * They can either be {@link FESThread ThFES} or {@link BESThread ThBES} threads.
     */
    private GESThread[] gesThreads = null;

    /**
     * The {@link Thread Thread} array that encapsulate the {@link GESThread GESThread} for each stage.
     */
    private Thread[] threads = null;

    /**
     * Subset of {@link TupleNode TupleNodes}. Each subset will be assigned to {@link GESThread GESThread}
     */
    private ArrayList<TupleNode>[] subSets = null;

    /**
     * {@link ArrayList ArrayList} of graphs. This contains the list of {@link Graph graphs} created for each stage,
     * just before the fusion is done.
     */
    private ArrayList<Dag> graphs = null;

    /**
     * {@link Graph Graph} containing the current bayesian network that has been constructed so far.
     */
    private Graph currentGraph = null;

    /**
     * Iteration counter. It stores the current iteration of the algorithm.
     */
    private int it = 1;

    /**
     * {@link TupleNode TupleNode} array containing the possible list of edges of the resulting bayesian network.
     */
    private TupleNode[] listOfArcs;

    private ArrayList<Stage> stages;



    public BNBuilder(DataSet data, int nThreads){
        this.problem = new Problem(data);
        initialize(nThreads);
    }

    public BNBuilder(String path, int nThreads){
        this(Utils.readData(path), nThreads);
    }


    private void initialize(int nThreads){
        this.nThreads = nThreads;
        this.gesThreads = new FESThread[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList[this.nThreads];

        //The total number of arcs of a graph is n*(n-1)/2, where n is the number of nodes in the graph.
        this.listOfArcs = new TupleNode[this.problem.getData().getNumColumns() * (this.problem.getData().getNumColumns() -1) / 2];
    }

    private boolean convergence(){
        return true;
    }

    private void initialConfig(){

    }

    public void search(){
        initialConfig();

        do{
            for(Stage s : stages){
                s.run();
            }
        }while(!convergence());
    }


}
