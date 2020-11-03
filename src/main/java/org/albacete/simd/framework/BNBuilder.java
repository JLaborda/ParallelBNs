package org.albacete.simd.framework;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.threads.BESThread;
import org.albacete.simd.threads.FESThread;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.ArrayList;
import java.util.List;

// Ideas para el futuro
public abstract class BNBuilder {
    /**
     * {@link DataSet DataSet}DataSet containing the values of the variables of the problem in hand.
     */
    //private final DataSet data;
    protected Problem problem;
    /**
     * The number of threads the algorithm is going to use.
     */
    protected int nThreads = 1;

    /**
     * Seed for the random number generator.
     */
    private long seed = 42;

    /**
     * Number of iterations allowed inside the FES stage. This is a hyperparameter used in experimentation.
     */
    protected int nItInterleaving = 5;


    /**
     * The maximum number of iterations allowed for the algorithm.
     */
    protected int maxIterations = 15;

    /**
     * The {@link GESThread GESThread} array that will be executed in each stage.
     * They can either be {@link FESThread ThFES} or {@link BESThread ThBES} threads.
     */
    protected GESThread[] gesThreads = null;

    /**
     * The {@link Thread Thread} array that encapsulate the {@link GESThread GESThread} for each stage.
     */
    protected Thread[] threads = null;

    /**
     * Subset of {@link Edge Edges}. Each subset will be assigned to {@link GESThread GESThread}
     */
    protected List<List<Edge>> subSets = null;

    /**
     * {@link ArrayList ArrayList} of graphs. This contains the list of {@link Graph graphs} created for each stage,
     * just before the fusion is done.
     */
    protected ArrayList<Dag> graphs = null;

    /**
     * {@link Graph Graph} containing the current bayesian network that has been constructed so far.
     */
    protected Graph currentGraph = null;

    /**
     * Iteration counter. It stores the current iteration of the algorithm.
     */
    protected int it = 1;

    /**
     * {@link Edge Edge} list containing the possible list of edges of the resulting bayesian network.
     */
    protected List<Edge> listOfArcs;



    public BNBuilder(DataSet data, int nThreads, int maxIterations, int nItInterleaving){
        this.problem = new Problem(data);
        this.maxIterations = maxIterations;
        this.nItInterleaving = nItInterleaving;
        initialize(nThreads);
    }

    public BNBuilder(String path, int nThreads, int maxIterations, int nItInterleaving){
        this(Utils.readData(path), nThreads, maxIterations, nItInterleaving);
    }


    private void initialize(int nThreads){
        this.nThreads = nThreads;
        this.gesThreads = new FESThread[this.nThreads];
        this.threads = new Thread[this.nThreads];
        this.subSets = new ArrayList<>(this.nThreads);

        //The total number of arcs of a graph is n*(n-1)/2, where n is the number of nodes in the graph.
        this.listOfArcs = new ArrayList<>(this.problem.getData().getNumColumns() * (this.problem.getData().getNumColumns() -1));
        this.listOfArcs = Utils.calculateArcs(this.problem.getData());
    }

    protected abstract boolean convergence();

    protected abstract void initialConfig();

    protected abstract void repartition();

    protected abstract void forwardStage() throws InterruptedException;

    protected abstract void forwardFusion() throws InterruptedException;

    protected abstract void backwardStage() throws InterruptedException;

    protected abstract void backwardFusion() throws InterruptedException;


    public void search(){
        initialConfig();

        do{
            try{
                repartition();
                forwardStage();
                forwardFusion();
                backwardStage();
                backwardFusion();
            } catch (InterruptedException e) {
                System.err.println("Interrupted Exception");
                e.printStackTrace();
            }
        }while(!convergence());
    }

    public boolean checkWorkingStatus() throws InterruptedException {
        for (GESThread g: gesThreads) {
            if (g.getFlag() ){
                return true;
            }
        }
        return false;
    }




    /**
     * Sets the seed for the random generator.
     * @param seed seed used for the random number generator.
     */
    public void setSeed(long seed) {
        this.seed = seed;
        Utils.setSeed(seed);
    }

    /**
     * Gets the used seed for the random number generator.
     * @return seed used in the random number generator
     */
    public long getSeed(){
        return this.seed;
    }

    /**
     * Gets the list of possible edges of the problem
     * @return List of {@link Edge Edges} representing all the possible edges of the problem.
     */
    public List<Edge> getListOfArcs() {
        return listOfArcs;
    }

    /**
     * Gets the current subsets of edges.
     * @return List of Lists of {@link Edge Edges} containing the edges of each subset.
     */
    public List<List<Edge>> getSubSets() {
        return subSets;
    }

    /**
     * Gets the {@link DataSet DataSet} of the problem.
     * @return {@link DataSet DataSet} with the data of the problem.
     */
    public DataSet getData() {
        return problem.getData();
    }

    /**
     * Gets the maximum number of iterations.
     * @return number of maximum iterations.
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Sets the maximum number of iterations
     * @param maxIterations new value of the maximum number of iterations
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Sets the maximum number of iterations for each {@link FESThread ThFES}.
     * @param nFESItInterleaving maximum number of iterations used in each {@link FESThread ThFES}.
     */
    public void setNFESItInterleaving(int nFESItInterleaving) {
        this.nItInterleaving = nFESItInterleaving;
    }

    /**
     * Gets the current list of graphs.
     * @return ArrayList of the current Dags created in a previous stage.
     */
    public ArrayList<Dag> getGraphs(){
        return this.graphs;
    }

    /**
     * Gets the {@link #currentGraph currentGraph} constructed so far.
     * @return Dag of the currentGraph.
     */
    public Graph getCurrentGraph(){
        return this.currentGraph;
    }

    /**
     * Gets the current iteration number.
     * @return iteration the algorithm is in.
     */
    public int getIterations(){
        return it;
    }

    public Problem getProblem() {
        return problem;
    }


    public int getnThreads() {
        return nThreads;
    }

    public int getItInterleaving() {
        return nItInterleaving;
    }



}
