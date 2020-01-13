package org.albacete.simd;

import consensusBN.SubSet;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.LocalScoreCache;
import edu.cmu.tetrad.search.MeekRules;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.ProbUtils;
import org.albacete.simd.TupleNode;

import java.text.NumberFormat;
import java.util.*;

public abstract class GESThread implements Runnable{
    /**
     * Tuple of Nodes that will be checked by this thread in the FES method
     */
    protected ArrayList<TupleNode> S;
    /**
     * Dataset of the problem in hands
     */
    protected DataSet data = null;

    /**
     * Initial DAG this thread starts with. It can be null or a resulting DAG of an iteration.
     */
    protected Graph initialDag = null;
    /**
     * The current DAG with which the thread is working. This is a modified DAG of the initial DAG.
     */
    protected Graph currentGraph = null;

    /**
     * Flag indicating if the thread is working or not.
     */
    protected boolean flag = false;

    /**
     * For discrete data scoring, the structure prior.
     */
    protected double structurePrior;

    /**
     * For discrete data scoring, the sample prior.
     */
    protected double samplePrior;

    /**
     * Map from variables to their column indices in the data set.
     */
    protected HashMap<Node, Integer> hashIndices;

    /**
     * Array of variable names from the data set, in order.
     */
    protected String[] varNames;

    /**
     * List of variables in the data set, in order.
     */
    protected List<Node> variables;

    /**
     * For formatting printed numbers.
     */
    protected final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

    /**
     * Caches scores for discrete search.
     */
    protected final LocalScoreCache localScoreCache = new LocalScoreCache();

    /**
     * Total calls done
     */
    protected int numTotalCalls=0;

    /**
     * Total calls done to non-cached information
     */
    protected int numNonCachedCalls=0;

    /**
     * Elapsed time of the most recent search.
     */
    protected long elapsedTime;


    /**
     * True if cycles are to be aggressively prevented. May be expensive
     * for large graphs (but also useful for large graphs).
     */
    protected boolean aggressivelyPreventCycles = false;

    /**
     * Maximum number of edges.
     */
    protected int maxNumEdges = -1;

    /**
     * Score of the bdeu model.
     */
    protected double modelBDeu;

    /**
     * Cases for each variable of the problem.
     */
    protected int[][] cases;

    /**
     * Number of values a variable can take.
     */
    protected int[] nValues;

    /**
     * Maximum number of iterations.
     */
    protected int maxIt;

    /**
     * Node X from the edge X->Y that can be inserted in the graph.
     */
    protected Node x_i;

    /**
     * Node Y from the edge X->Y that can be inserted in the graph.
     */
    protected Node y_i;

    /**
     * Subset T of Nodes with which X and Y are inserted.
     */
    protected SubSet t_0;

    /**
     * Node X from the edge X->Y that can be deleted from the graph.
     */
    protected Node x_d;

    /**
     * Node Y from the edge X->Y that can be deleted from the graph.
     */
    protected Node y_d;

    /**
     * Subset H of Nodes with which X and Y are deleted.
     */
    protected SubSet h_0;


    /**
     * Search method that explores the data and currentGraph to return a better Graph
     * @return PDAG that contains either the result of the BES or FES method.
     */
    public abstract Graph search();


    public void setMaxIt(int maxIt) {
        this.maxIt = maxIt;
    }


    public void setSubSetSearch(ArrayList<TupleNode> subset) {
        this.S=subset;

    }

    public void setInitialDag(Graph initialDag2) {
        this.initialDag = new EdgeListGraph(initialDag2);
    }


    public boolean isAggressivelyPreventCycles() {
        return this.aggressivelyPreventCycles;
    }

    public void setAggressivelyPreventCycles(boolean aggressivelyPreventCycles) {
        this.aggressivelyPreventCycles = aggressivelyPreventCycles;
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
     * Get all nodes that are connected to Y by an undirected edge and not
     * adjacent to X.
     */
    public static List<Node> getTNeighbors(Node x, Node y, Graph graph) {
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
     * Get all nodes that are connected to Y by an undirected edge and adjacent
     * to X
     */
    public static List<Node> getHNeighbors(Node x, Node y, Graph graph) {
        List<Node> hNeighbors = new LinkedList<>(graph.getAdjacentNodes(y));
        hNeighbors.retainAll(graph.getAdjacentNodes(x));

        for (int i = hNeighbors.size() - 1; i >= 0; i--) {
            Node z = hNeighbors.get(i);
            Edge edge = graph.getEdge(y, z);
            if (!Edges.isUndirectedEdge(edge)) {
                hNeighbors.remove(z);
            }
        }

        return hNeighbors;
    }


    /**
     * Evaluate the Insert(X, Y, T) operator (Definition 12 from Chickering,
     * 2002).
     */
    public double insertEval(Node x, Node y, Set<Node> t, Graph graph) {
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
    public void insert(Node x, Node y, Set<Node> subset, Graph graph) {
        graph.addDirectedEdge(x, y);

        for (Node node : subset) {
            graph.removeEdge(node, y);
            graph.addDirectedEdge(node, y);
        }
    }

    /**
     * Do an actual deletion (Definition 13 from Chickering, 2002).
     */
    public static void delete(Node x, Node y, Set<Node> subset, Graph graph) {
        graph.removeEdges(x, y);

        for (Node aSubset : subset) {
            if (!graph.isParentOf(aSubset, x) && !graph.isParentOf(x, aSubset)) {
                graph.removeEdge(x, aSubset);
                graph.addDirectedEdge(x, aSubset);
            }
            graph.removeEdge(y, aSubset);
            graph.addDirectedEdge(y, aSubset);
        }
    }


    /**
     * Evaluate the Delete(X, Y, T) operator (Definition 12 from Chickering,
     * 2002).
     */
    protected double deleteEval(Node x, Node y, Set<Node> h, Graph graph) {
        Set<Node> set1 = new HashSet<>(findNaYX(x, y, graph));
        set1.removeAll(h);
        set1.addAll(graph.getParents(y));
        Set<Node> set2 = new HashSet<>(set1);
        set1.remove(x);
        set2.add(x);
        return scoreGraphChange(y, set1, set2);
    }


    //--Auxiliary methods.

    /**
     * Find all nodes that are connected to Y by an undirected edge that are
     * adjacent to X (that is, by undirected or directed edge) NOTE: very
     * inefficient implementation, since the current library does not allow
     * access to the adjacency list/matrix of the graph.
     */
    protected static List<Node> findNaYX(Node x, Node y, Graph graph) {
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
    protected static boolean isClique(List<Node> set, Graph graph) {
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
    protected boolean isSemiDirectedBlocked(Node x, Node y, List<Node> naYXT,
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
    protected void rebuildPattern(Graph graph) {
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
    protected void pdag(Graph graph) {
        MeekRules rules = new MeekRules();
        rules.setAggressivelyPreventCycles(this.aggressivelyPreventCycles);
        rules.orientImplied(graph);
    }

    protected void setDataSet(DataSet dataSet) {
        List<String> _varNames = dataSet.getVariableNames();

        this.data = dataSet;
        this.varNames = _varNames.toArray(new String[0]);
        this.variables = dataSet.getVariables();
    }

    protected void buildIndexing(Graph graph) {
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

    protected double scoreGraph(Graph graph) {
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

    protected double scoreGraphChange(Node y, Set<Node> parents1,
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


    public List<Node> getVariables() {
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

    // Thread method
    public synchronized Graph getCurrentGraph() throws InterruptedException{
        // While searching is not complete, wait
        while (currentGraph == null)
            wait();
        return this.currentGraph;
    }





}
