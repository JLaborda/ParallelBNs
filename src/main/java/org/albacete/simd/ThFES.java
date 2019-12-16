package org.albacete.simd;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.LocalScoreCache;
import edu.cmu.tetrad.search.MeekRules;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.ProbUtils;
import consensusBN.SubSet;
import consensusBN.PowerSet;
import consensusBN.PowerSetFabric;

public class ThFES implements Runnable{

    /**
     * Tuple of Nodes that will be checked by this thread in the FES method
     */
    private ArrayList<TupleNode> S;
    /**
     * Dataset of the problem in hands
     */
    private DataSet data = null;

    /**
     * Initial DAG this thread starts with. It can be null or a resulting DAG of an iteration.
     */
    private Graph initialDag = null;
    /**
     * The current DAG with which the thread is working. This is a modified DAG of the initial DAG.
     */
    private Graph currentGraph = null;

    /**
     * Flag indicating if the thread is working or not.
     */
    private boolean flag = false;

    /**
     * For discrete data scoring, the structure prior.
     */
    private double structurePrior;

    /**
     * For discrete data scoring, the sample prior.
     */
    private double samplePrior;

    /**
     * Map from variables to their column indices in the data set.
     */
    private HashMap<Node, Integer> hashIndices;

    /**
     * Array of variable names from the data set, in order.
     */
    private String[] varNames;

    /**
     * List of variables in the data set, in order.
     */
    private List<Node> variables;

    /**
     * For formatting printed numbers.
     */
    private final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

    /**
     * Caches scores for discrete search.
     */
    private final LocalScoreCache localScoreCache = new LocalScoreCache();

    /**
     * Total calls done
     */
    private int numTotalCalls=0;

    /**
     * Total calls done to non-cached information
     */
    private int numNonCachedCalls=0;

    /**
     * Elapsed time of the most recent search.
     */
    private long elapsedTime;


    /**
     * True if cycles are to be aggressively prevented. May be expensive
     * for large graphs (but also useful for large graphs).
     */
    private boolean aggressivelyPreventCycles = false;

    /**
     * Maximum number of edges.
     */
    private int maxNumEdges = -1;

    /**
     * Score of the bdeu model.
     */
    private double modelBDeu;

    /**
     * Cases for each variable of the problem.
     */
    private int[][] cases;

    /**
     * Number of values a variable can take.
     */
    private int[] nValues;

    /**
     * Maximum number of iterations.
     */
    private int maxIt;

    /**
     * Node X from the edge X->Y that can be inserted in the graph.
     */
    private Node x_i;

    /**
     * Node Y from the edge X->Y that can be inserted in the graph.
     */
    private Node y_i;

    /**
     * Subset T of Nodes with which X and Y are inserted.
     */
    private SubSet t_0;


    /**
     * Constructor of ThFES with an initial DAG
     * @param dataSet data of the problem
     * @param initialDag initial DAG with which the FES stage starts with, if it's null, use the other constructor
     * @param subset subset of edges the fes stage will try to add to the resulting graph
     * @param maxIt maximum number of iterations allowed in the fes stage
     */
    public ThFES(DataSet dataSet,Graph initialDag, ArrayList<TupleNode> subset,int maxIt) {
        setDataSet(dataSet);
        setInitialDag(initialDag);
        setSubSetSearch(subset);
        setMaxIt(maxIt);
        cases=new int[dataSet.getNumRows()][dataSet.getNumColumns()];
        for(int i=0;i<dataSet.getNumRows();i++) {
            for(int j=0;j<dataSet.getNumColumns();j++) {
                cases[i][j]=dataSet.getInt(i, j);
            }
        }
        nValues=new int[dataSet.getNumColumns()];
        for(int i=0;i<dataSet.getNumColumns();i++)
            nValues[i]=((DiscreteVariable)dataSet.getVariable(i)).getNumCategories();
        initialize(10., 0.001);
    }


    public ThFES(DataSet dataSet, ArrayList<TupleNode> subset,int maxIt) {
        setDataSet(dataSet);
        this.initialDag = new EdgeListGraph(new LinkedList<>(getVariables()));
        setSubSetSearch(subset);
        setMaxIt(maxIt);
        cases=new int[dataSet.getNumRows()][dataSet.getNumColumns()];
        for(int i=0;i<dataSet.getNumRows();i++) {
            for(int j=0;j<dataSet.getNumColumns();j++) {
                cases[i][j]=dataSet.getInt(i, j);
            }
        }
        nValues=new int[dataSet.getNumColumns()];
        for(int i=0;i<dataSet.getNumColumns();i++)
            nValues[i]=((DiscreteVariable)dataSet.getVariable(i)).getNumCategories();
        initialize(10., 0.001);
    }

    private void setMaxIt(int maxIt) {
        this.maxIt = maxIt;
    }



    private void setSubSetSearch(ArrayList<TupleNode> subset) {
        this.S=subset;

    }



    private void setInitialDag(Graph initialDag2) {
        this.initialDag = new EdgeListGraph(initialDag2);
    }



    //==========================PUBLIC METHODS==========================//


    public boolean isAggressivelyPreventCycles() {
        return this.aggressivelyPreventCycles;
    }

    public void setAggressivelyPreventCycles(boolean aggressivelyPreventCycles) {
        this.aggressivelyPreventCycles = aggressivelyPreventCycles;
    }

    // Thread method
    public void run(){
        this.currentGraph = search();
    }

    // Thread method
    public synchronized Graph getCurrentGraph() throws InterruptedException{
        // While searching is not complete, wait
        while (currentGraph == null)
            wait();
        return this.currentGraph;
    }

    // Thread method
    public synchronized boolean getFlag() throws InterruptedException{
        // While searching is not complete, wait
        while (currentGraph == null)
            wait();
        return this.flag;
    }

    // Thread method
    public synchronized void resetFlag(){
        this.flag = false;
    }

    public double getScoreBDeu() {
        return this.modelBDeu;
    }
    /**
     * Greedy equivalence search: Start from the empty graph, add edges till
     * model is significant. Then start deleting edges till a minimum is
     * achieved.
     *
     * @return the resulting Pattern.
     */
    private Graph search() {
        long startTime = System.currentTimeMillis();
        numTotalCalls=0;
        numNonCachedCalls=0;
        localScoreCache.clear();

        Graph graph = new EdgeListGraph(this.initialDag);
        buildIndexing(graph);

        // Method 1-- original.
        double score = scoreGraph(graph);

        // Do forward search.
        score = fes(graph, score);//iges(graph, score);

        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;
        return graph;

    }


    //===========================PRIVATE METHODS========================//

    private void initialize(double samplePrior, double structurePrior) {
        setStructurePrior(structurePrior);
        setSamplePrior(samplePrior);
    }

    /**
     * Forward equivalence search.
     *
     * @param graph The graph in the state prior to the forward equivalence
     *              search.
     * @param score The score in the state prior to the forward equivalence
     *              search
     * @return the score in the state after the GES b and f both simultanourly equivelance search.
     *         Note that the graph is changed as a side-effect to its state after
     *         the forward equivelance search.
     */

    private double fes(Graph graph, double score) {
        System.out.println("** FORWARD EQUIVALENCE SEARCH");
        double bestScore = score;
        double bestInsert;

        x_i = null;
        y_i = null;
        t_0 = null;
        int it = 0;

        System.out.println("Initial Score = " + nf.format(bestScore));
        // Calling fs to calculate best edge to add.
        bestInsert = fs(graph,bestScore);

        while((x_i != null) && (it < this.maxIt)){
            // Changing best score because x_i, and therefore, y_i is not null
            bestScore = bestInsert;

            // Inserting edge
            insert(x_i,y_i,t_0, graph);

            // Checking cycles?
            boolean ciclos = graph.existsDirectedCycle();

            //PDAGtoCPDAG
            rebuildPattern(graph);

            // Printing score
            if (!t_0.isEmpty())
                System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i) + " " + t_0);
            else
                System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i));
            bestScore = bestInsert;

            // Checking that the maximum number of edges has not been reached
            if (getMaxNumEdges() != -1 && graph.getNumEdges() > getMaxNumEdges()) {
                System.out.println("Maximum edges reached");
                break;
            }

            // Indicating that the thread has added an edge to the graph
            this.flag = true;
            it++;

            // Executing FS function to calculate the best edge to be added
            bestInsert = fs(graph,bestScore);

        }
        return bestScore;

    }

    /**
     * Forward search.
     *
     * @param graph The graph in the state prior to the forward equivalence
     *              search.
     * @param score The score in the state prior to the forward equivalence
     *              search
     * @return the score in the state after the forward equivelance search.
     *         Note that the graph is changed as a side-effect to its state after
     *         the forward equivelance search.
     */

    private double fs(Graph graph, double score) {
        //       System.out.println("** FORWARD EQUIVALENCE SEARCH");
        double bestScore = score;
        //       System.out.println("Initial Score = " + nf.format(bestScore));

// ------ Miramos el mejor FES ---

        PowerSetFabric.setMode(PowerSetFabric.MODE_FES);
        x_i=null;
        y_i=null;
        t_0 = null;

        for (TupleNode tupleNode : this.S){
            Node _x = tupleNode.x;
            Node _y = tupleNode.y;
            if (_x == _y) {
                continue;
            }

            if (graph.isAdjacentTo(_x, _y)) {
                continue;
            }
// ---------------------------- chequea y evalua un enlace entre _x e _y-----------
            List<Node> tNeighbors = getTNeighbors(_x, _y, graph);
            PowerSet tSubsets = PowerSetFabric.getPowerSet(_x, _y, tNeighbors);

            while (tSubsets.hasMoreElements()) {
                SubSet tSubset = tSubsets.nextElement();

                double insertEval = insertEval(_x, _y, tSubset, graph);
                double evalScore = score + insertEval;

                if (!(evalScore > bestScore && evalScore > score)) {
                    continue;
                }

                List<Node> naYXT = new LinkedList<>(tSubset);
                naYXT.addAll(findNaYX(_x, _y, graph));

                // INICIO TEST 1
                if (tSubset.firstTest == SubSet.TEST_NOT_EVALUATED) {
                    if (!isClique(naYXT, graph)) {
                        continue;
                    }
                } else if (tSubset.firstTest == SubSet.TEST_FALSE) {
                    continue;
                }
                // FIN TEST 1

                // INICIO TEST 2
                if (tSubset.secondTest == SubSet.TEST_NOT_EVALUATED) {
                    if (!isSemiDirectedBlocked(_x, _y, naYXT, graph, new HashSet<>())) {
                        continue;
                    }
                } else if (tSubset.secondTest == SubSet.TEST_FALSE) { // No puede ocurrir
                    continue;
                }
                // FIN TEST 2

                bestScore = evalScore;
                x_i = _x;
                y_i = _y;
                t_0 = tSubset;
            }

            //.......................... hasta aqui se evalua un enlace.

        }


        return bestScore;

    }


    /**
     * Get all nodes that are connected to Y by an undirected edge and not
     * adjacent to X.
     */
    private static List<Node> getTNeighbors(Node x, Node y, Graph graph) {
        List<Node> tNeighbors = new LinkedList<>(graph.getAdjacentNodes(y));
        tNeighbors.removeAll(graph.getAdjacentNodes(x));

        for (int i = tNeighbors.size() - 1; i >= 0; i--) {
            Node z = tNeighbors.get(i);
            Edge edge = graph.getEdge(y, z);

            if (!Edges.isUndirectedEdge(edge)) {
                tNeighbors.remove(z);
            }
        }

        return tNeighbors;
    }


    /**
     * Evaluate the Insert(X, Y, T) operator (Definition 12 from Chickering,
     * 2002).
     */
    private double insertEval(Node x, Node y, Set<Node> t, Graph graph) {
        // set1 contains x; set2 does not.
        Set<Node> set1 = new HashSet<>(findNaYX(x, y, graph));
        set1.addAll(t);
        set1.addAll(graph.getParents(y));
        Set<Node> set2 = new HashSet<>(set1);
        set1.add(x);
        return scoreGraphChange(y, set1, set2);
    }


    /**
     * Do an actual insertion
     * (Definition 12 from Chickering, 2002).
     */
    private void insert(Node x, Node y, Set<Node> subset, Graph graph) {
        graph.addDirectedEdge(x, y);

        for (Node node : subset) {
            graph.removeEdge(node, y);
            graph.addDirectedEdge(node, y);
        }
    }



    //--Auxiliary methods.

    /**
     * Find all nodes that are connected to Y by an undirected edge that are
     * adjacent to X (that is, by undirected or directed edge) NOTE: very
     * inefficient implementation, since the current library does not allow
     * access to the adjacency list/matrix of the graph.
     */
    private static List<Node> findNaYX(Node x, Node y, Graph graph) {
        List<Node> naYX = new LinkedList<>(graph.getAdjacentNodes(y));
        naYX.retainAll(graph.getAdjacentNodes(x));

        for (int i = naYX.size()-1; i >= 0; i--) {
            Node z = naYX.get(i);
            Edge edge = graph.getEdge(y, z);

            if (!Edges.isUndirectedEdge(edge)) {
                naYX.remove(z);
            }
        }

        return naYX;
    }

    /**
     * Returns true if the given set forms a clique in the given graph.
     */
    private static boolean isClique(List<Node> set, Graph graph) {
        List<Node> setv = new LinkedList<>(set);
        for (int i = 0; i < setv.size() - 1; i++) {
            for (int j = i + 1; j < setv.size(); j++) {
                if (!graph.isAdjacentTo(setv.get(i), setv.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Verifies if every semidirected path from y to x contains a node in naYXT.
     */
    private boolean isSemiDirectedBlocked(Node x, Node y, List<Node> naYXT,
                                          Graph graph, Set<Node> marked) {
        if (naYXT.contains(y)) {
            return true;
        }

        if (y == x) {
            return false;
        }

        for (Node node1 : graph.getNodes()) {
            if (node1 == y || marked.contains(node1)) {
                continue;
            }

            if (graph.isAdjacentTo(y, node1) && !graph.isParentOf(node1, y)) {
                marked.add(node1);

                if (!isSemiDirectedBlocked(x, node1, naYXT, graph, marked)) {
                    return false;
                }

                marked.remove(node1);
            }
        }

        return true;
    }



    /**
     * Completes a pattern that was modified by an insertion/deletion operator
     * Based on the algorithm described on Appendix C of (Chickering, 2002).
     */
    private void rebuildPattern(Graph graph) {
        SearchGraphUtils.basicPattern(graph);
        pdag(graph);
    }

    /**
     * Fully direct a graph with background knowledge. I am not sure how to
     * adapt Chickering's suggested algorithm above (dagToPdag) to incorporate
     * background knowledge, so I am also implementing this algorithm based on
     * Meek's 1995 UAI paper. Notice it is the same implemented in PcSearch.
     * </p> *IMPORTANT!* *It assumes all colliders are oriented, as well as
     * arrows dictated by time order.*
     *
     * ELIMINADO BACKGROUND KNOWLEDGE
     */
    private void pdag(Graph graph) {
        MeekRules rules = new MeekRules();
        rules.setAggressivelyPreventCycles(this.aggressivelyPreventCycles);
        rules.orientImplied(graph);
    }

    private void setDataSet(DataSet dataSet) {
        List<String> _varNames = dataSet.getVariableNames();

        this.data = dataSet;
        this.varNames = _varNames.toArray(new String[0]);
        this.variables = dataSet.getVariables();
    }

    private void buildIndexing(Graph graph) {
        this.hashIndices = new HashMap<>();
        for (Node next : graph.getNodes()) {
            for (int i = 0; i < this.varNames.length; i++) {
                if (this.varNames[i].equals(next.getName())) {
                    this.hashIndices.put(next, i);
                    break;
                }
            }
        }
    }


    //===========================SCORING METHODS===========================//

    private double scoreGraph(Graph graph) {
//        Graph dag = SearchGraphUtils.dagFromPattern(graph);
        Graph dag = new EdgeListGraph(graph);
        SearchGraphUtils.pdagToDag(dag);
        double score = 0.;

        for (Node next : dag.getNodes()) {
            Collection<Node> parents = dag.getParents(next);
            int nextIndex = -1;
            for (int i = 0; i < getVariables().size(); i++) {
                if (this.varNames[i].equals(next.getName())) {
                    nextIndex = i;
                    break;
                }
            }
            int[] parentIndices = new int[parents.size()];
            Iterator<Node> pi = parents.iterator();
            int count = 0;
            while (pi.hasNext()) {
                Node nextParent = pi.next();
                for (int i = 0; i < getVariables().size(); i++) {
                    if (this.varNames[i].equals(nextParent.getName())) {
                        parentIndices[count++] = i;
                        break;
                    }
                }
            }
            score += localBdeuScore(nextIndex, parentIndices);
        }
        return score;
    }

    private double scoreGraphChange(Node y, Set<Node> parents1,
                                    Set<Node> parents2) {
        int yIndex = hashIndices.get(y);
        int[] parentIndices1 = new int[parents1.size()];

        int count = 0;
        for (Node aParents1 : parents1) {
            parentIndices1[count++] = (hashIndices.get(aParents1));
        }

        int[] parentIndices2 = new int[parents2.size()];

        int count2 = 0;
        for (Node aParents2 : parents2) {
            parentIndices2[count2++] = (hashIndices.get(aParents2));
        }
        double score1 = localBdeuScore(yIndex, parentIndices1);
        double score2 = localBdeuScore(yIndex, parentIndices2);
        return score1 - score2;
    }


    protected double localBdeuScore(int nNode, int[] nParents) {
        numTotalCalls++;
        double oldScore = localScoreCache.get(nNode, nParents);
        if (!Double.isNaN(oldScore)) {
            return oldScore;
        }
        numNonCachedCalls++;
        int numValues=nValues[nNode];
        int numParents=nParents.length;

        double ess=getSamplePrior();
        double kappa=getStructurePrior();

        int[] numValuesParents=new int[nParents.length];
        int cardinality=1;
        for(int i=0;i<numValuesParents.length;i++) {
            numValuesParents[i]=nValues[nParents[i]];
            cardinality*=numValuesParents[i];
        }

        int[][] Ni_jk = new int[cardinality][numValues];
        double Np_ijk = (1.0*ess) / (numValues*cardinality);
        double Np_ij = (1.0*ess) / cardinality;

        // initialize
        for (int j = 0; j < cardinality;j++)
            for(int k= 0; k<numValues; k++)
                Ni_jk[j][k] = 0;

        for (int[] aCase : cases) {
            int iCPT = 0;
            for (int iParent = 0; iParent < numParents; iParent++) {
                iCPT = iCPT * numValuesParents[iParent] + aCase[nParents[iParent]];
            }
            Ni_jk[iCPT][aCase[nNode]]++;
        }

        double fLogScore = 0.0;

        for (int iParent = 0; iParent < cardinality; iParent++) {
            double N_ij = 0;
            double N_ijk = 0;

            for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
                if (Ni_jk[iParent][iSymbol] != 0) {
                    N_ijk = Ni_jk[iParent][iSymbol];
                    fLogScore += ProbUtils.lngamma(N_ijk + Np_ijk);
                    fLogScore -= ProbUtils.lngamma(Np_ijk);
                    N_ij += N_ijk;
                }
            }
            if (Np_ij != 0)
                fLogScore += ProbUtils.lngamma(Np_ij);
            if (Np_ij + N_ij != 0)
                fLogScore -= ProbUtils.lngamma(Np_ij + N_ij);
        }
        fLogScore += Math.log(kappa) * cardinality * (numValues - 1);

        localScoreCache.add(nNode, nParents, fLogScore);
        return fLogScore;
    }


    //==========================SETTERS AND GETTERS=========================//


    public void setInitialGraph(Graph currentGraph){
        this.initialDag = currentGraph;
    }

    public Graph getInitialGraph(){
        return this.initialDag;
    }


    private List<Node> getVariables() {
        return variables;
    }


    public double getStructurePrior() {
        return structurePrior;
    }

    public double getSamplePrior() {
        return samplePrior;
    }

    public void setStructurePrior(double structurePrior) {
        this.structurePrior = structurePrior;
    }

    public void setSamplePrior(double samplePrior) {
        this.samplePrior = samplePrior;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public int getMaxNumEdges() {
        return maxNumEdges;
    }

    public void setMaxNumEdges(int maxNumEdges) {
        if (maxNumEdges < -1)
            throw new IllegalArgumentException();

        this.maxNumEdges = maxNumEdges;
    }







}
