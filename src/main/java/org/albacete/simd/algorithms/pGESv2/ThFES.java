package org.albacete.simd.algorithms.pGESv2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import consensusBN.SubSet;
import consensusBN.PowerSet;
import consensusBN.PowerSetFabric;

@SuppressWarnings("DuplicatedCode")
public class ThFES extends GESThread{

    /**
     * Constructor of ThFES with an initial DAG
     * @param dataSet data of the problem
     * @param initialDag initial DAG with which the FES stage starts with, if it's null, use the other constructor
     * @param subset subset of edges the fes stage will try to add to the resulting graph
     * @param maxIt maximum number of iterations allowed in the fes stage
     */

    private static int threadCounter = 1;

    public ThFES(Problem problem,Graph initialDag, ArrayList<TupleNode> subset,int maxIt) {
        this.problem = problem;
        setInitialGraph(initialDag);
        setSubSetSearch(subset);
        setMaxIt(maxIt);
        this.id = threadCounter;
        threadCounter++;
    }

    /**
     * Constructor of ThFES with an initial DataSet
     * @param problem object containing information of the problem such as data or variables.
     * @param subset subset of edges the fes stage will try to add to the resulting graph
     * @param maxIt maximum number of iterations allowed in the fes stage
     */
    public ThFES(Problem problem, ArrayList<TupleNode> subset,int maxIt) {
        this.problem = problem;
        this.initialDag = new EdgeListGraph(new LinkedList<>(getVariables()));
        setSubSetSearch(subset);
        setMaxIt(maxIt);
        this.id = threadCounter;
        threadCounter++;
    }



    //==========================PUBLIC METHODS==========================//


    @Override
    /*
      Run method from {@link Thread Thread} interface. The method executes the {@link #search()} search} method to add
      edges to the initial graph.
     */
    public void run(){
        this.currentGraph = search();
    }



    //===========================PRIVATE METHODS========================//

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
        //localScoreCache.clear();

        Graph graph = new EdgeListGraph(this.initialDag);
        //buildIndexing(graph);

        // Method 1-- original.
        double score = scoreGraph(graph, problem);

        // Do forward search.
        score = fes(graph, score);

        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;
        return graph;

    }

    /**
     * Forward equivalence search.
     *
     * @param graph The graph in the state prior to the forward equivalence
     *              search.
     * @param score The score in the state prior to the forward equivalence
     *              search
     * @return the score in the state after the FES method.
     *         Note that the graph is changed as a side-effect to its state after
     *         the forward equivalence search.
     */

    private double fes(Graph graph, double score) {
        System.out.println("** FORWARD EQUIVALENCE SEARCH");
        double bestScore = score;
        double bestInsert;

        x_i = null;
        y_i = null;
        t_0 = null;
        iterations = 0;

        System.out.println("Initial Score = " + nf.format(bestScore));
        // Calling fs to calculate best edge to add.
        bestInsert = fs(graph,bestScore);

        while((x_i != null) && (iterations < this.maxIt)){
            // Changing best score because x_i, and therefore, y_i is not null
            bestScore = bestInsert;

            // Inserting edge
            System.out.println("Thread " + getId() + " inserting: (" + x_i + ", " + y_i + ", " + t_0+ ")");
            insert(x_i,y_i,t_0, graph);

            // Checking cycles?
            // boolean cycles = graph.existsDirectedCycle();

            //PDAGtoCPDAG
            rebuildPattern(graph);

            // Printing score
            if (!t_0.isEmpty())
                System.out.println("["+getId() + "] Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i) + " " + t_0);
            else
                System.out.println("["+getId() + "] Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i));
            bestScore = bestInsert;

            // Checking that the maximum number of edges has not been reached
            if (getMaxNumEdges() != -1 && graph.getNumEdges() >= getMaxNumEdges()) {
                System.out.println("Maximum edges reached");
                break;
            }

            // Indicating that the thread has added an edge to the graph
            this.flag = true;
            iterations++;

            // Executing FS function to calculate the best edge to be added
            bestInsert = fs(graph,bestScore);

        }
        return bestScore;

    }

    /**
     * Forward search. Finds the best possible edge to be added into the current graph and returns its score.
     *
     * @param graph The graph in the state prior to the forward equivalence
     *              search.
     * @param score The score in the state prior to the forward equivalence
     *              search
     * @return the score in the state after the forward equivalence search.
     *         Note that the graph is changed as a side-effect to its state after
     *         the forward equivalence search.
     */

    private double fs(Graph graph, double score) {
        //       System.out.println("** FORWARD EQUIVALENCE SEARCH");
        //       System.out.println("Initial Score = " + nf.format(bestScore));

// ------ Searching for the best FES ---

        double bestScore = score;
        PowerSetFabric.setMode(PowerSetFabric.MODE_FES);
        x_i= null;
        y_i= null;
        t_0= null;

        List<Edge> edges = new ArrayList<>();

        for (TupleNode tupleNode : this.S) {
            Node _x = tupleNode.x;
            Node _y = tupleNode.y;

            if (_x == _y)
                continue;

            // Adding Edges to check
            edges.add(Edges.directedEdge(_x, _y));
            edges.add(Edges.directedEdge(_y, _x));
        }

        for(Edge edge : edges){

            Node _x = Edges.getDirectedEdgeTail(edge);
            Node _y = Edges.getDirectedEdgeHead(edge);

            if (graph.isAdjacentTo(_x, _y)) {
                continue;
            }
// ---------------------------- Checking and evaluating an edge between _x and _y-----------
            List<Node> tNeighbors = getSubsetOfNeighbors(_x, _y, graph);
//            PowerSet tSubsets = PowerSetFabric.getPowerSet(_x, _y, tNeighbors);
            
            SubSet tSubset = new SubSet();
			double insertEval = insertEval(_x, _y, tSubset, graph, problem);
			double evalScore = score + insertEval;
			if (evalScore <= score) {
				continue;
			}
            
			if (!(evalScore > bestScore && evalScore > score)) {
                continue;
            }
                   

            List<Node> naYXT = new LinkedList<>(tSubset);
            List<Node> naYX = findNaYX(_x, _y, graph);
            naYXT.addAll(naYX);
            
            // START TEST 1
            if (tSubset.firstTest == SubSet.TEST_NOT_EVALUATED) {
                if (!isClique(naYXT, graph)) {
                    continue;
                }
            } else if (tSubset.firstTest == SubSet.TEST_FALSE) {
                continue;
            }
            // END TEST 1
            
            // START TEST 2
            if (tSubset.secondTest == SubSet.TEST_NOT_EVALUATED) {
                if (!isSemiDirectedBlocked(_x, _y, naYXT, graph, new HashSet<>())) {
                    continue;
                }
            } else if (tSubset.secondTest == SubSet.TEST_FALSE) { // This can't happen
                continue;
            }
            
            double greedyScore = evalScore;
            int bestNodeIndex;
            Node bestNode = null;
            int subsetTsize = 0;
            
            do {
            	bestNodeIndex = -1;
            	for (int k = 0; k < tNeighbors.size(); k++) {
					Node node = tNeighbors.get(k);
					SubSet newT = new SubSet(tSubset);
					newT.add(node);
					insertEval = insertEval(_x, _y, newT, graph, problem);
					evalScore = score + insertEval;

					if (evalScore <= greedyScore) {
						continue;
					}

					naYXT = new LinkedList<Node>(newT);
					naYXT.addAll(naYX);
					
					// START TEST 1
		            if (tSubset.firstTest == SubSet.TEST_NOT_EVALUATED) {
		                if (!isClique(naYXT, graph)) {
		                    continue;
		                }
		            } else if (tSubset.firstTest == SubSet.TEST_FALSE) {
		                continue;
		            }
		            // END TEST 1
		            
//		            // START TEST 2
		            if (tSubset.secondTest == SubSet.TEST_NOT_EVALUATED) {
		                if (!isSemiDirectedBlocked(_x, _y, naYXT, graph, new HashSet<>())) {
		                    continue;
		                }
		            } else if (tSubset.secondTest == SubSet.TEST_FALSE) { // This can't happen
		                continue;
		            }
					
		            bestNodeIndex = k;
					bestNode = node;
					greedyScore = evalScore;
            	}
            	if (bestNodeIndex != -1) {
					tSubset.add(bestNode);
					tNeighbors.remove(bestNodeIndex);
					subsetTsize++;
				}
            	
            }while ((bestNodeIndex !=-1)&&(subsetTsize <= 1));
            
            if(greedyScore > bestScore) {
            	bestScore = greedyScore;
            	x_i = _x;
            	y_i = _y;
            	t_0 = tSubset;
            }

            //.......................... an edge is evaluated until here.
        }

        return bestScore;

    }



}
