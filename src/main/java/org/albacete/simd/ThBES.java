package org.albacete.simd;

import consensusBN.PowerSet;
import consensusBN.PowerSetFabric;
import consensusBN.SubSet;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class ThBES extends GESThread {


    /**
     * Constructor of ThFES with an initial DAG
     * @param dataSet data of the problem
     * @param initialDag initial DAG with which the FES stage starts with, if it's null, use the other constructor
     * @param subset subset of edges the fes stage will try to add to the resulting graph
     */
    public ThBES(DataSet dataSet, Graph initialDag, ArrayList<TupleNode> subset) {
        setDataSet(dataSet);
        setInitialGraph(initialDag);
        setSubSetSearch(subset);
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


    private void initialize() {
        setStructurePrior(0.001);
        setSamplePrior(10.0);
    }



    @Override
    public void run() {
        this.currentGraph = search();
    }

    /**
     * Search method that explores the data and currentGraph to return a better Graph
     * @return PDAG that contains either the result of the BES or FES method.
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

        // Do backward search.
        score = bes(graph, score);//iges(graph, score);

        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;
        return graph;
    }


    /**
     * Backward equivalence search.
     *
     * @param graph The graph in the state prior to the backward equivalence
     *              search.
     * @param score The score in the state prior to the backward equivalence
     *              search
     * @return the score in the state after the BES method.
     *         Note that the graph is changed as a side-effect to its state after
     *         the backward equivalence search.
     */
    private double bes(Graph graph, double score) {
        System.out.println("** BACKWARD EQUIVALENCE SEARCH");
        double bestScore = score;
        double bestDelete;

        x_d = null;
        y_d = null;
        h_0 = null;
        int it = 0;

        System.out.println("Initial Score = " + nf.format(bestScore));
        // Calling fs to calculate best edge to add.
        bestDelete = bs(graph,bestScore);

        while(x_d != null){
            // Changing best score because x_d, and y_d are not null
            bestScore = bestDelete;

            // Deleting edge
            delete(x_d,y_d,h_0, graph);

            // Checking cycles?
            // boolean ciclos = graph.existsDirectedCycle();

            //PDAGtoCPDAG
            rebuildPattern(graph);

            // Printing score
            if (!h_0.isEmpty())
                System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestDelete-score) +")\tOperator: " + graph.getEdge(x_d, y_d) + " " + h_0);
            else
                System.out.println("Score: " + nf.format(bestScore) + " (+" + nf.format(bestDelete-score) +")\tOperator: " + graph.getEdge(x_d, y_d));
            bestScore = bestDelete;

            // Checking that the maximum number of edges has not been reached
            if (getMaxNumEdges() != -1 && graph.getNumEdges() > getMaxNumEdges()) {
                System.out.println("Maximum edges reached");
                break;
            }

            // Indicating that the thread has added an edge to the graph
            this.flag = true;
            it++;

            // Executing BS function to calculate the best edge to be added
            bestDelete = bs(graph,bestScore);

        }
        return bestScore;

    }

    private double bs(Graph graph, double initialScore){
        //   	System.out.println("\n** BACKWARD ELIMINATION SEARCH");
        //   	System.out.println("Initial Score = " + nf.format(initialScore));

        PowerSetFabric.setMode(PowerSetFabric.MODE_BES);
        double bestScore = initialScore;

        x_d = y_d = null;
        h_0 = null;
        List<Edge> edges1 = graph.getEdges();
        List<Edge> edges = new ArrayList<>();

        for (TupleNode tupleNode : this.S) {
            Node _x = tupleNode.x;
            Node _y = tupleNode.y;

            // Adding Edges to check
            edges.add(Edges.directedEdge(_x, _y));
            edges.add(Edges.directedEdge(_y, _x));

        }

        for (Edge edge : edges) {

            // Checking if the edge is actually inside the graph
            if(!graph.containsEdge(edge))
                continue;

            Node _x = Edges.getDirectedEdgeTail(edge);
            Node _y = Edges.getDirectedEdgeHead(edge);

            List<Node> hNeighbors = getSubsetOfNeighbors(_x, _y, graph);
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



}
