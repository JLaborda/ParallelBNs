package org.albacete.simd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
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
    public ThFES(DataSet dataSet,Graph initialDag, ArrayList<TupleNode> subset,int maxIt) {
        setDataSet(dataSet);
        setInitialGraph(initialDag);
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
        setStructurePrior(0.001);
        setSamplePrior(10.0);
    }

    /**
     * Constructor of ThFES with an initial DataSet
     * @param dataSet data of the problem.
     * @param subset subset of edges the fes stage will try to add to the resulting graph
     * @param maxIt maximum number of iterations allowed in the fes stage
     */
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
        setStructurePrior(0.001);
        setSamplePrior(10.0);
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
        localScoreCache.clear();

        Graph graph = new EdgeListGraph(this.initialDag);
        buildIndexing(graph);

        // Method 1-- original.
        double score = scoreGraph(graph);

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
            insert(x_i,y_i,t_0, graph);

            // Checking cycles?
            // boolean cycles = graph.existsDirectedCycle();

            //PDAGtoCPDAG
            rebuildPattern(graph);

            // Printing score
            if (!t_0.isEmpty())
                System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i) + " " + t_0);
            else
                System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestInsert-score) +")\tOperator: " + graph.getEdge(x_i, y_i));
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
        x_i=null;
        y_i=null;
        t_0 = null;

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
                // END TEST 2

                bestScore = evalScore;
                x_i = _x;
                y_i = _y;
                t_0 = tSubset;
            }

            //.......................... an edge is evaluated until here.

        }


        return bestScore;

    }
















}
