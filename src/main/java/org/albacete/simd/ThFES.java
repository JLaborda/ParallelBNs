package org.albacete.simd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
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
        initialize();
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
        initialize();
    }



    //==========================PUBLIC METHODS==========================//


    // Thread method
    public void run(){
        this.currentGraph = search();
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

    private void initialize() {
        setStructurePrior(0.001);
        setSamplePrior(10.0);
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
            // boolean ciclos = graph.existsDirectedCycle();

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
















}
