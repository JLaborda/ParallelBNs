package org.albacete.simd.utils;

import consensusBN.PairWiseConsensusBES;
import edu.cmu.tetrad.bayes.BayesIm;
import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.SearchGraphUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Utils {


    private static Random random = new Random();

    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem.
     * @param listOfArcs List of {@link Edge Edges} containing all the possible edges for the actual problem.
     * @param numSplits The number of splits to do in the listOfArcs.
     * @return The subsets of the listOfArcs in an ArrayList of TupleNode.
     */
    public static List<List<Edge>> split(List<Edge> listOfArcs, int numSplits){


        List<List<Edge>> subSets = new ArrayList<>(numSplits);

        // Shuffling arcs
        List<Edge> shuffledArcs = new ArrayList<>(listOfArcs);
        Collections.shuffle(shuffledArcs, random);

        // Splitting Arcs into subsets
        int n = 0;
        for(int s = 0; s< numSplits-1; s++){
            List<Edge> sub = new ArrayList<>();
            for(int i = 0; i < Math.floorDiv(shuffledArcs.size(),numSplits) ; i++){
                sub.add(shuffledArcs.get(n));
                n++;
            }
            subSets.add(sub);
        }

        // Adding leftovers
        ArrayList<Edge> sub = new ArrayList<>();
        for(int i = n; i < shuffledArcs.size(); i++ ){
            sub.add(shuffledArcs.get(i));
        }
        subSets.add(sub);

        return subSets;

    }

    public static void setSeed(long seed){
        random = new Random(seed);
    }

    /**
     * Calculates the amount of possible arcs between the variables of the dataset and stores it.
     * @param data DataSet used to calculate the arcs between its columns (nodes).
     */
    public static List<Edge> calculateArcs(DataSet data){
        //0. Accumulator
        List<Edge> listOfArcs = new ArrayList<>(data.getNumColumns() * (data.getNumColumns() -1));
        //1. Get edges (variables)
        List<Node> variables = data.getVariables();
        //int index = 0;
        //2. Iterate over variables and save pairs
        for(int i=0; i<data.getNumColumns()-1; i++){
            for(int j=i+1; j<data.getNumColumns(); j++){
                // Getting pair of variables (Each variable is different)
                Node var_A = variables.get(i);
                Node var_B = variables.get(j);

                //3. Storing both pairs
                listOfArcs.add(Edges.directedEdge(var_A,var_B));
                listOfArcs.add(Edges.directedEdge(var_B,var_A));
                //index++;
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


    public static Node getNodeByName(List<Node> nodes, String name){
        for(Node n : nodes){
            if (n.getName().equals(name)){
                return n;
            }
        }
        return null;
    }

    public static int getIndexOfNodeByName(List<Node> nodes, String name){
        for(int i = 0; i < nodes.size(); i++){
            Node n = nodes.get(i);
            if(n.getName().equals(name)){
                return i;
            }
        }
        return -1;
    }

    private static void ensureVariables(ArrayList<Dag> setofbns){

        List<Node> nodes = setofbns.get(0).getNodes();
        //System.out.println("Nodes: " + nodes);
        for(int i = 1 ; i< setofbns.size(); i++){
            Dag oldDag = setofbns.get(i);
            List<Edge> oldEdges = oldDag.getEdges();
            Dag newdag = new Dag(nodes);
            for(Edge e: oldEdges){
                /*
                System.out.println("Node1");
                System.out.println(e.getNode1());
                System.out.println("Node2");
                System.out.println(e.getNode2());
                */
                //int tailIndex = nodes.indexOf(e.getNode1());
                //int headIndex = nodes.indexOf(e.getNode2());

                int tailIndex = getIndexOfNodeByName(nodes, e.getNode1().getName());
                int headIndex = getIndexOfNodeByName(nodes, e.getNode2().getName());

                //System.out.println("tail: " + tailIndex);
                //System.out.println("head: "  + headIndex);
                Edge newEdge = new Edge(nodes.get(tailIndex),nodes.get(headIndex), Endpoint.TAIL, Endpoint.ARROW);
                newdag.addEdge(newEdge);
            }
            setofbns.remove(i);
            setofbns.add(i, newdag);
        }
    }

    public static int compare(Dag bn1, Dag bn2){
        ArrayList<Dag> dags = new ArrayList<>();
        dags.add(bn1);
        dags.add(bn2);
        ensureVariables(dags);
        PairWiseConsensusBES kl = new PairWiseConsensusBES(dags.get(0), dags.get(1));
        kl.getFusion();
        int hmd =  kl.getHammingDistance();
        return hmd;
    }

    public static int SHD (Dag bn1, Dag bn2) {

        ArrayList<Dag> dags = new ArrayList<>();
        dags.add(bn1);
        dags.add(bn2);
        ensureVariables(dags);

        Graph g1 = new EdgeListGraph(dags.get(0));
        Graph g2 = new EdgeListGraph(dags.get(1));

        for(Node n: dags.get(0).getNodes()) {
            List<Node> p = dags.get(0).getParents(n);
            for (int i=0; i<p.size()-1;i++)
                for(int j=i+1; j<p.size();j++) {
                    Edge e1 = g1.getEdge(p.get(i), p.get(j));
                    Edge e2 = g1.getEdge(p.get(j), p.get(i));
                    if(e1==null && e2 == null) {
                        Edge e = new Edge(p.get(i),p.get(j),Endpoint.TAIL,Endpoint.TAIL);
                        g1.addEdge(e);
                    }
                }
        }

        for(Node n: dags.get(1).getNodes()) {
            List<Node> p = dags.get(1).getParents(n);
            for (int i=0; i<p.size()-1;i++)
                for(int j=i+1; j<p.size();j++) {
                    Edge e1 = g2.getEdge(p.get(i), p.get(j));
                    Edge e2 = g2.getEdge(p.get(j), p.get(i));
                    if(e1==null && e2 == null) {
                        Edge e = new Edge(p.get(i),p.get(j),Endpoint.TAIL,Endpoint.TAIL);
                        g2.addEdge(e);
                    }
                }
        }

        int sum = 0;
        for(Edge e: g1.getEdges()) {
            Edge e2 = g2.getEdge(e.getNode1(), e.getNode2());
            Edge e3 = g2.getEdge(e.getNode2(), e.getNode1());
            if(e2 == null && e3 == null) sum++;
        }

        for(Edge e: g2.getEdges()) {
            Edge e2 = g1.getEdge(e.getNode1(), e.getNode2());
            Edge e3 = g1.getEdge(e.getNode2(), e.getNode1());
            if(e2 == null && e3 == null) sum++;
        }
        return sum;
    }



    public static List<Node> getMarkovBlanket(Dag bn, Node n){
        List<Node> mb = new ArrayList<>();

        // Adding children and parents to the Markov's Blanket of this node
        List<Node> children = bn.getChildren(n);
        List<Node> parents = bn.getParents(n);

        mb.addAll(children);
        mb.addAll(parents);

        for(Node child : children){
            for(Node father : bn.getParents(child)){
                if (!father.equals(n)){
                    mb.add(father);
                }
            }
        }

        return mb;
    }

    /**
     * Gives back the percentages of markov's blanquet difference with the original bayesian network. It gives back the
     * percentage of difference with the blanquet of the original bayesian network, the percentage of extra nodes added
     * to the blanquet and the percentage of missing nodes in the blanquet compared with the original.
     * @param original
     * @param created
     * @return
     */
    public static double [] avgMarkovBlanquetdif(Dag original, Dag created) {

        if (original.getNodes().size() != created.getNodes().size())
            return null;

        // First number is the average dfMB, the second one is the amount of more variables in each MB, the last number is the the amount of missing variables in each MB
        double [] result = new double[3];
        double differenceNodes = 0;
        double plusNodes = 0;
        double minusNodes = 0;


        for( Node e1 : original.getNodes()) {
            Node e2 = created.getNode(e1.getName());

            // Creating Markov's Blanket
            List<Node> mb1 = getMarkovBlanket(original, e1);
            List<Node> mb2 = getMarkovBlanket(created, e2);


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
                    differenceNodes++;
                    plusNodes++;
                }
            }
            // Variables de menos
            for(String s1: names1) {
                if(!names2.contains(s1)) {
                    differenceNodes++;
                    minusNodes++;
                }
            }
        }

        // Differences of MM

        result[0] = differenceNodes;
        result[1] = plusNodes;
        result[2] = minusNodes;

        return result;

    }
/*
    public static double scoreGraph(Graph graph, DataSet dataSet) {

        if (graph == null){
            return Double.NEGATIVE_INFINITY;
        }

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
            score += Utils.localBdeuScore(nextIndex, parentIndices);
        }
        return score;
    }
*/

/*
    public static double localBdeuScore(int nNode, int[] nParents) {
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
*/

    /**
     * Transforms a graph to a DAG, and removes any possible inconsistency found throughout its structure.
     * @param g Graph to be transformed.
     * @return Resulting DAG of the inserted graph.
     */
    public static Dag removeInconsistencies(Graph g){
        // Transforming the current graph into a DAG
        SearchGraphUtils.pdagToDag(g);

        // Checking Consistency
        Node nodeT, nodeH;
        for (Edge e : g.getEdges()){
            if(!e.isDirected()) continue;
            //System.out.println("Undirected Edge: " + e);
            Endpoint endpoint1 = e.getEndpoint1();
            if (endpoint1.equals(Endpoint.ARROW)){
                nodeT = e.getNode1();
                nodeH = e.getNode2();
            }else{
                nodeT = e.getNode2();
                nodeH = e.getNode1();
            }


            if(g.existsDirectedPathFromTo(nodeT, nodeH)){
                System.out.println("Directed path from " + nodeT + " to " + nodeH +"\t Deleting Edge...");
                g.removeEdge(e);
            }
        }
        // Adding graph from each thread to the graphs array
        return new Dag(g);

    }

    public static double LL(BayesIm bn, DataSet data) {

        BayesIm bayesIm;

        int[][][] observedCounts;

        Graph graph = bn.getDag();
        Node[] nodes = new Node[graph.getNumNodes()];

        observedCounts = new int[nodes.length][][];

        int[][] observedCountsRowSum = new int[nodes.length][];

        bayesIm = new MlBayesIm(bn);

        for (int i = 0; i < nodes.length; i++) {

            int numRows = bayesIm.getNumRows(i);
            observedCounts[i] = new int[numRows][];

            observedCountsRowSum[i] = new int[numRows];

            for (int j = 0; j < numRows; j++) {

                observedCountsRowSum[i][j] = 0;

                int numCols = bayesIm.getNumColumns(i);
                observedCounts[i][j] = new int[numCols];
            }
        }

        //At this point set values in observedCounts

        for (int j = 0; j < data.getNumColumns(); j++) {
            DiscreteVariable var = (DiscreteVariable) data.getVariables().get(j);
            String varName = var.getName();
            Node varNode = bn.getDag().getNode(varName);
            int varIndex = bayesIm.getNodeIndex(varNode);

            int[] parentVarIndices = bayesIm.getParents(varIndex);

            if (parentVarIndices.length == 0) {
                //System.out.println("No parents");
                for (int col = 0; col < var.getNumCategories(); col++) {
                    observedCounts[varIndex][0][col] = 0;
                }

                for (int i = 0; i < data.getNumRows(); i++) {

                    observedCounts[varIndex][0][data.getInt(i, j)] += 1.0;


                }

            }
            else {    //For variables with parents:
                int numRows = bayesIm.getNumRows(varIndex);

                for (int row = 0; row < numRows; row++) {
                    int[] parValues = bayesIm.getParentValues(varIndex, row);

                    for (int col = 0; col < var.getNumCategories(); col++) {
                        observedCounts[varIndex][row][col] = 0;
                    }

                    for (int i = 0; i < data.getNumRows(); i++) {
                        //for a case where the parent values = parValues increment the estCount

                        boolean parentMatch = true;

                        for (int p = 0; p < parentVarIndices.length; p++) {
                            if (parValues[p] != data.getInt(i, parentVarIndices[p])) {
                                parentMatch = false;
                                break;
                            }
                        }

                        if (!parentMatch) {
                            continue;  //Not a matching case; go to next.
                        }

                        observedCounts[varIndex][row][data.getInt(i, j)] += 1;
                    }

                }

            }


        }


        for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < bayesIm.getNumRows(i); j++) {
                for (int k = 0; k < bayesIm.getNumColumns(i); k++) {
                    observedCountsRowSum[i][j] += observedCounts[i][j][k];
                }
            }
        }

        double sum = 0.0;

        int n = nodes.length;

        for (int i = 0; i < n; i++) {
            int qi = bayesIm.getNumRows(i);
            for (int j = 0; j < qi; j++) {
                int ri = bayesIm.getNumColumns(i);
                for (int k = 0; k < ri; k++) {
                    try {
                        double p1 = observedCounts[i][j][k];
                        double p2 = observedCountsRowSum[i][j];
                        double p3 = Math.log(p1/p2);
                        if(p1 != 0.0) sum += p1 * p3;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        return sum/data.getNumRows()/data.getNumColumns();
    }

    public static double LL(Dag g, DataSet data){
        BayesPm bnaux = new BayesPm(g);
        MlBayesIm bnOut = new MlBayesIm(bnaux, MlBayesIm.MANUAL);
        return LL(bnOut, data);
    }

}
