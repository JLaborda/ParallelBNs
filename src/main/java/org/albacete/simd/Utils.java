package org.albacete.simd;

import consensusBN.ConsensusBES;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.LocalScoreCache;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.ProbUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Utils {


    private static final LocalScoreCache localScoreCache = new LocalScoreCache();

    private static int[][] cases;
    private static int[] nValues;
    private static String varNames[];
    public static double samplePrior = 10.0;
    public static double structurePrior = 0.001;


    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem.
     * @param listOfArcs Array of {@link TupleNode TupleNode} of all the possible edges for the actual problem.
     * @param numSplits The number of splits to do in the listOfArcs.
     * @param seed The random seed used for the splits.
     * @return The subsets of the listOfArcs in an ArrayList of TupleNode.
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<TupleNode>[] split(TupleNode[] listOfArcs, int numSplits, long seed){


        ArrayList<TupleNode>[] subSets = new ArrayList[numSplits];

        // Shuffling arcs
        List<TupleNode> shuffledArcs = Arrays.asList(listOfArcs);
        Random random = new Random(seed);
        Collections.shuffle(shuffledArcs, random);

        // Splitting Arcs into subsets
        int n = 0;
        for(int s = 0; s< subSets.length-1; s++){
            ArrayList<TupleNode> sub = new ArrayList<>();
            for(int i = 0; i < Math.floorDiv(shuffledArcs.size(),numSplits) ; i++){
                sub.add(shuffledArcs.get(n));
                n++;
            }
            subSets[s] = sub;
        }

        // Adding leftovers
        ArrayList<TupleNode> sub = new ArrayList<>();
        for(int i = n; i < shuffledArcs.size(); i++ ){
            sub.add(shuffledArcs.get(i));
        }
        subSets[subSets.length-1] = sub;

        return subSets;

    }

    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem.
     * @param edges List of {@link Edge Edges} of all the possible edges for the actual problem.
     * @param numSplits The number of splits to do in the listOfArcs.
     * @param seed The random seed used for the splits.
     * @return The subsets of the listOfArcs in an ArrayList of TupleNode.
     */
    public static ArrayList<TupleNode>[] split(List<Edge> edges, int numSplits, long seed){
        // Transforming edges into TupleNodes
        TupleNode[] listOfArcs = new TupleNode[edges.size()];
        for(int i=0; i<edges.size(); i++){
            listOfArcs[i] = edgeToTupleNode(edges.get(i));
        }
        return split(listOfArcs, numSplits, seed);
    }

    /**
     * Transforms an Edge into a TupleNode
     * @param edge {@link Edge Edge} passed as argument to transform itself into a {@link TupleNode TupleNode}
     * @return The corresponding {@link TupleNode TupleNode} of the edge.
     */
    public static TupleNode edgeToTupleNode(Edge edge){
        return new TupleNode(edge.getNode1(), edge.getNode2());
    }

    /**
     * Calculates the amount of possible arcs between the variables of the dataset and stores it.
     * @param data DataSet used to calculate the arcs between its columns (nodes).
     */
    public static TupleNode[] calculateArcs(DataSet data){
        //0. Accumulator
        TupleNode[] listOfArcs = new TupleNode[data.getNumColumns() * (data.getNumColumns() -1) / 2];
        //1. Get edges (variables)
        List<Node> variables = data.getVariables();
        int index = 0;
        //2. Iterate over variables and save pairs
        for(int i=0; i<data.getNumColumns()-1; i++){
            for(int j=i+1; j<data.getNumColumns(); j++){
                // Getting pair of variables
                Node var_A = variables.get(i);
                Node var_B = variables.get(j);
                //3. Storing both pairs
                // Maybe we can use Edge object
                listOfArcs[index] = new TupleNode(var_A,var_B);
                index++;
                //this.listOfArcs[index] = new TupleNode(var_B,var_A);
                //index++;
            }
        }
        return listOfArcs;
    }


    /**
     * Stores the data from a csv as a DataSet object.
     * @param path
     * Path to the csv file.
     * @return DataSet containing the data from the csv file.
     */
    public static DataSet readData(String path){
        // Initial Configuration
        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);
        DataSet dataSet = null;
        // Reading data
        try {
            dataSet = reader.parseTabular(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataSet;
    }


    public static int compare(Dag bn1, Dag bn2){
        ArrayList<Dag> dags = new ArrayList<Dag>();
        dags.add(bn1);
        dags.add(bn2);
        ensureVariables(dags);
        ConsensusBES cons = new ConsensusBES(dags);
        cons.fusion();
        return cons.getNumberOfInsertedEdges();
    }

    private static void ensureVariables(ArrayList<Dag> setofbns){

        List<Node> nodes = setofbns.get(0).getNodes();

        for(int i = 1 ; i< setofbns.size(); i++){
            Dag oldDag = setofbns.get(i);
            List<Edge> oldEdges = oldDag.getEdges();
            Dag newdag = new Dag(nodes);
            for(Edge e: oldEdges){
                Node node1 = setofbns.get(0).getNode(e.getNode1().getName());
                Node node2 = setofbns.get(0).getNode(e.getNode2().getName());
                Edge newEdge = new Edge(node1,node2, e.getEndpoint1(), e.getEndpoint2());
                newdag.addEdge(newEdge);
            }
            setofbns.remove(i);
            setofbns.add(i, newdag);
        }
    }

    public static double [] avgMarkovBlanquetdif(Dag original, Dag created) {

        if (original.getNodes().size() != created.getNodes().size())
            return null;

        // First number is the average dfMB, the second one is the amount of more variables in each MB, the last number is the the amount of missing variables in each MB
        double [] result = new double[3];
        double res1 = 0;
        double res2 = 0;
        double res3 = 0;


        for( Node e1 : original.getNodes()) {
            Node e2 = created.getNode(e1.getName());

            // Creando los mantos de markov de la variable en cada DAG.
            ArrayList<Node> mb1 = new ArrayList<Node>();
            ArrayList<Node> mb2 = new ArrayList<Node>();

            mb1.addAll(original.getParents(e1));
            mb1.addAll(original.getChildren(e1));
            for(Node c: original.getChildren(e1)) {
                mb1.addAll(original.getParents(c));
            }

            mb2.addAll(created.getParents(e2));
            mb2.addAll(created.getChildren(e2));
            for(Node c: created.getChildren(e2)) {
                mb2.addAll(created.getParents(c));
            }

            ArrayList<String> names1 = new ArrayList<String>();
            ArrayList<String> names2 = new ArrayList<String>();
            // Nodos de más en el manto creado
            for (Node n1 : mb1) {
                String name1 = n1.getName();
                names1.add(name1);
            }
            for (Node n2 : mb2) {
                String name2 = n2.getName();
                names2.add(name2);
            }

            //Variables de más
            for(String s2: names2) {
                if(!names1.contains(s2)) {
                    res1++;
                    res2++;
                }
            }
            // Variables de menos
            for(String s1: names1) {
                if(!names2.contains(s1)) {
                    res1++;
                    res3++;
                }
            }
        }

        // Avg difference
        res1 = res1 / original.getNodes().size();

        result[0] = res1;
        result[1] = res2;
        result[2] = res3;

        return result;

    }

    public static double scoreGraph(Graph graph, DataSet dataSet) {

        // Setting up Scorer
        List<String> _varNames = dataSet.getVariableNames();

        varNames = _varNames.toArray(new String[0]);
        List<Node> variables = dataSet.getVariables();

        cases=new int[dataSet.getNumRows()][dataSet.getNumColumns()];
        for(int i=0;i<dataSet.getNumRows();i++) {
            for(int j=0;j<dataSet.getNumColumns();j++) {
                cases[i][j]=dataSet.getInt(i, j);
            }
        }
        nValues=new int[dataSet.getNumColumns()];
        for(int i=0;i<dataSet.getNumColumns();i++)
            nValues[i]=((DiscreteVariable)dataSet.getVariable(i)).getNumCategories();

//      Graph dag = SearchGraphUtils.dagFromPattern(graph);
        Graph dag = new EdgeListGraph(graph);
        SearchGraphUtils.pdagToDag(dag);
        double score = 0.;

        for (Node next : dag.getNodes()) {
            Collection<Node> parents = dag.getParents(next);
            int nextIndex = -1;
            for (int i = 0; i < variables.size(); i++) {
                if (varNames[i].equals(next.getName())) {
                    nextIndex = i;
                    break;
                }
            }
            int parentIndices[] = new int[parents.size()];
            Iterator<Node> pi = parents.iterator();
            int count = 0;
            while (pi.hasNext()) {
                Node nextParent = pi.next();
                for (int i = 0; i < variables.size(); i++) {
                    if (varNames[i].equals(nextParent.getName())) {
                        parentIndices[count++] = i;
                        break;
                    }
                }
            }
            score += localBdeuScore(nextIndex, parentIndices);
        }
        return score;
    }

    protected static double localBdeuScore(int nNode, int[] nParents) {
        //numTotalCalls++;
        double oldScore = localScoreCache.get(nNode, nParents);
        if (!Double.isNaN(oldScore)) {
            return oldScore;
        }
        //numNonCachedCalls++;
        int numValues=nValues[nNode];
        int numParents=nParents.length;

        double ess=samplePrior;
        double kappa=structurePrior;

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

        for(int i=0;i<cases.length;i++) {
            int iCPT = 0;
            for (int iParent = 0; iParent < numParents; iParent++) {
                iCPT = iCPT * numValuesParents[iParent] + cases[i][nParents[iParent]];
            }
            Ni_jk[iCPT][cases[i][nNode]]++;
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



}
