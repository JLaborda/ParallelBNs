package org.albacete.simd.pGES;

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
import edu.cmu.tetrad.graph.Dag;
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
import consensusBN.ListFabric;
import consensusBN.PowerSet;
import consensusBN.PowerSetFabric;

public class ThGES implements Runnable{
	
	private ArrayList<Node> S = null;
	
	private DataSet data = null;
	
	private Graph initialDag = null;
	
	private Graph currentGraph = null;
	
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
    private int numTotalCalls=0;
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

    private int maxNumEdges = -1;
    private double modelBDeu;
    
    private int[][] cases;
    private int[] nValues;

	private int maxIt;

	private Node x_i;

	private Node y_i;

	private SubSet t_0;

	private Node x_d;

	private Node y_d;

	private SubSet h_0;


    
    public ThGES(DataSet dataSet,Graph initialDag,ArrayList<Node> subset,int maxIt) {
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

    
    public ThGES(DataSet dataSet,ArrayList<Node> subset,int maxIt) {
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
		// TODO Auto-generated method stub
		this.maxIt = maxIt;
	}



	private void setSubSetSearch(ArrayList<Node> subset) {
		// TODO Auto-generated method stub
    	this.S=subset;
		
	}



	private void setInitialDag(Graph initialDag2) {
		// TODO Auto-generated method stub
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
    public Graph search() {
        long startTime = System.currentTimeMillis();
        numTotalCalls=0;
        numNonCachedCalls=0;
        localScoreCache.clear();

        Graph graph = new EdgeListGraph(this.initialDag);
        buildIndexing(graph);

        // Method 1-- original.
        double score = scoreGraph(graph);
        
        // Do forward/backward search.
        score = iges(graph, score);

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
     * Forward and Backward both equivalence search.
     *
     * @param graph The graph in the state prior to the forward equivalence
     *              search.
     * @param score The score in the state prior to the forward equivalence
     *              search
     * @return the score in the state after the GES b and f both simultanourly equivelance search.
     *         Note that the graph is changed as a side-effect to its state after
     *         the forward equivelance search.
     */
    
    private double iges(Graph graph, double score) {
    	System.out.println("** GES EQUIVALENCE SEARCH");
        double bestScore = score;
        double bestInsert;
        double bestDelete;
        
        x_i = null;
    	y_i = null;
    	t_0 = null;
    	x_d = null;
    	y_d = null;
    	h_0 = null;
    	int it = 0;
        
        System.out.println("Initial Score = " + nf.format(bestScore));
       
        do{
        	 bestInsert = fes(graph,bestScore);
        	 bestDelete = bes(graph,bestScore);
 
        	if ((x_i != null) && (bestInsert > bestDelete)) {
        		insert(x_i,y_i,t_0, graph);
        		//            Edge prevEdge=graph.getEdge(x, y);
        		boolean ciclos = graph.existsDirectedCycle();
        		rebuildPattern(graph);
        		if (!t_0.isEmpty())
        			System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i) + " " + t_0);
        		else
        			System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i));
        		bestScore = bestInsert;

        		if (getMaxNumEdges() != -1 && graph.getNumEdges() > getMaxNumEdges()) {
        			break;
        		}
        		this.flag = true;
        		it++;
        	}

        	if ((x_d != null) && (bestDelete >= bestInsert)) {
        		System.out.println("DELETE " + graph.getEdge(x_d, y_d) + h_0.toString() + " (" + nf.format(bestDelete-score) + ")");
        		delete(x_d, y_d, h_0, graph);
        		rebuildPattern(graph);
        		bestScore = bestDelete;
        		this.flag = true;
        	}
        }while(((x_d != null) || (x_i != null)) && (it < this.maxIt));
        return bestScore;

    }
    
    /**
     * Forward equivalence search.
     *
     * @param graph The graph in the state prior to the forward equivalence
     *              search.
     * @param score The score in the state prior to the forward equivalence
     *              search
     * @return the score in the state after the forward equivelance search.
     *         Note that the graph is changed as a side-effect to its state after
     *         the forward equivelance search.
     */
    
    private double fes(Graph graph, double score) {
 //       System.out.println("** FORWARD EQUIVALENCE SEARCH");
        double bestScore = score;
 //       System.out.println("Initial Score = " + nf.format(bestScore));
        
// ------ Miramos el mejor FES ---        
        
        PowerSetFabric.setMode(PowerSetFabric.MODE_FES);
        x_i=null;
        y_i=null;
        t_0 = null;
     
        List<Node> nodes = graph.getNodes();

        for (Node _x : nodes) {
            for (Node _y : this.S) { // S es el subconjunto al que restringimos.
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
        }
        
        return bestScore;
        
    }
    

    /**
     * Backward equivalence search.
     */
    private double bes(Graph graph, double initialScore) {
    	//   	System.out.println("\n** BACKWARD ELIMINATION SEARCH");
    	//   	System.out.println("Initial Score = " + nf.format(initialScore));
    	PowerSetFabric.setMode(PowerSetFabric.MODE_BES);
        double bestScore = initialScore;

    
    	x_d = y_d = null;
    	h_0 = null;
    	List<Edge> edges1 = graph.getEdges();
    	List<Edge> edges = new ArrayList<>();

    	for (Edge edge : edges1) {
    		Node _x = edge.getNode1();
    		Node _y = edge.getNode2();

    		if (Edges.isUndirectedEdge(edge)) {
    			edges.add(Edges.directedEdge(_x, _y));
    			edges.add(Edges.directedEdge(_y, _x));
    		} else {
    			edges.add(edge);
    		}
    	}

    	for (Edge edge : edges) {
    		Node _x = Edges.getDirectedEdgeTail(edge);
    		Node _y = Edges.getDirectedEdgeHead(edge);

    		List<Node> hNeighbors = getHNeighbors(_x, _y, graph);
    		//                List<Set<Node>> hSubsets = powerSet(hNeighbors);
    		PowerSet hSubsets= PowerSetFabric.getPowerSet(_x,_y,hNeighbors);

    		while(hSubsets.hasMoreElements()) {
    			SubSet hSubset=hSubsets.nextElement();
    			double deleteEval = deleteEval(_x, _y, hSubset, graph);
    			double evalScore = initialScore + deleteEval;

    			//                    System.out.println("Attempt removing " + _x + "-->" + _y + "(" +
    			//                            evalScore + ")");

    			if (!(evalScore > bestScore)) {
    				continue;
    			}

    			// INICIO TEST 1
    			List<Node> naYXH = findNaYX(_x, _y, graph);
    			naYXH.removeAll(hSubset);
    			if (!isClique(naYXH, graph)) {
    				continue;
    			}
    			// FIN TEST 1

    			bestScore = evalScore;
    			x_d = _x;
    			y_d = _y;
    			h_0 = hSubset;
    		}

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
     * Get all nodes that are connected to Y by an undirected edge and adjacent
     * to X
     */
    private static List<Node> getHNeighbors(Node x, Node y, Graph graph) {
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
     * Evaluate the Delete(X, Y, T) operator (Definition 12 from Chickering,
     * 2002).
     */
    private double deleteEval(Node x, Node y, Set<Node> h, Graph graph) {
        Set<Node> set1 = new HashSet<>(findNaYX(x, y, graph));
        set1.removeAll(h);
        set1.addAll(graph.getParents(y));
        Set<Node> set2 = new HashSet<>(set1);
        set1.remove(x);
        set2.add(x);
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

    /**
     * Do an actual deletion (Definition 13 from Chickering, 2002).
     */
    private static void delete(Node x, Node y, Set<Node> subset, Graph graph) {
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
     * Returns true iif the given set forms a clique in the given graph.
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

    
    public void setInitialGraph(Graph currentGraph){
    	
    	this.initialDag = currentGraph;
    	
    }

    //===========================SCORING METHODS===========================//

    public double scoreGraph(Graph graph) {
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
    

    private List<Node> getVariables() {
        return variables;
    }


//    private DataSet dataSet() {
//        return dataSet;
//    }

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
        if (maxNumEdges < -1) throw new IllegalArgumentException();

        this.maxNumEdges = maxNumEdges;
    }

    public double getModelBDeu() {
        return modelBDeu;
    }

    public double getScore(Graph dag) {
        return scoreGraph(dag);
    }

	public int getNumNonCachedCalls() {
		return numNonCachedCalls;
	}

	public int getNumTotalCalls() {
		return numTotalCalls;
	}
	
	public int getMaxParents() {
		return ListFabric.getMaxSize()+1;
	}
	
	public void setMaxParents(int maxParents) {
		ListFabric.setMaxSize(maxParents-1);
	}
	
	public boolean isUsePowerSetsCache() {
		return PowerSetFabric.isUsePowerSetsCache();
	}

	public void setUsePowerSetsCache(boolean usePowerSetsCache) {
		PowerSetFabric.setUsePowerSetsCache(usePowerSetsCache);
	}
    
    
    
}
