package org.albacete.simd.clustering;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HierarchicalClustering {

    private final Object lock = new Object();
    private Problem problem;
    private Set<Edge> allEdges;
    private double[][] simMatrix;
    private boolean isParallel = false;
    private Map<Edge, Double> edgeScores = new ConcurrentHashMap<>();
    private List<Set<Node>> clusters;

    public HierarchicalClustering(Problem problem) {
        this.problem = problem;
        this.simMatrix = new double[problem.getVariables().size()][problem.getVariables().size()];
    }


    public HierarchicalClustering(Problem problem, boolean isParallel) {
        this(problem);
        this.isParallel = isParallel;
    }

    //Prueba
    public static void main(String[] args) {
        String networkFolder = "./res/networks/";
        String net_name = "alarm";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50001_.csv";
        Problem p = new Problem(Utils.readData(bbdd_path));

        HierarchicalClustering clustering = new HierarchicalClustering(p, false);
        Map<Edge, Double> map = clustering.getEdgesScore();

        System.out.println("Mapa: " + map);

        System.out.println("Probando clusterizing");
        List<Set<Node>> clusters = clustering.clusterize(10);


        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster " + i + ": " + clusters.get(i));
        }

    }

    public Map<Edge, Double> getEdgesScore() {
        // Calculating all the edges
        allEdges = Utils.calculateArcs(problem.getData()); //IDEA: ¿Calcularlo directamente en problem?

        // Getting hashmap of indexes
        HashMap<Node, Integer> index = problem.getHashIndices();
        // Parallel for loop
        allEdges.parallelStream().forEach((edge) -> {
            Node parent = edge.getNode1();
            Node child = edge.getNode2();
            int parentIndex = index.get(parent);
            int childIndex = index.get(child);

            double score = GESThread.localBdeuScore(childIndex, new int[]{parentIndex}, problem) -
                    GESThread.localBdeuScore(childIndex, new int[]{}, problem);
            edgeScores.put(edge, score);
        });
        return edgeScores;
    }

    private void initializeSimMatrixSequential() {
        List<Node> nodes = problem.getVariables();
        int numNodes = problem.getVariables().size();
        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                // Getting Nodes
                Node x = nodes.get(i);
                Node y = nodes.get(j);
                // Getting Edge
                Edge edge = new Edge(x, y, Endpoint.TAIL, Endpoint.ARROW);
                // Getting score
                simMatrix[i][j] = edgeScores.get(edge);
            }
        }
    }

    private void initializeSimMatrixParallel() {
        // Similarity matrix
        // Getting hashmap of indexes
        Map<Node, Integer> index = problem.getHashIndices();
        // Calculating simmilarity
        edgeScores.entrySet().parallelStream().forEach(edgeDoubleEntry -> {
                    int i = index.get(edgeDoubleEntry.getKey().getNode1()); //Getting node 1 index
                    int j = index.get(edgeDoubleEntry.getKey().getNode2()); // Getting node 2 index
                    double score = edgeDoubleEntry.getValue();              // Getting score of edge
                    addValueSimMatrix(i, j, score);                         //Adding value to the sim matrix
                }
        );

    }

    private void initializeSimMatrix() {
        if (isParallel)
            initializeSimMatrixParallel();
        else
            initializeSimMatrixSequential();

    }

    private void addValueSimMatrix(int i, int j, double score) {
        synchronized (lock) {
            simMatrix[i][j] = score;
        }
    }

    private double getScoreClusters(Set<Node> cluster1, Set<Node> cluster2) {
        // Nodes of the problem
        List<Node> nodes = problem.getVariables();
        // Indexes of the nodes
        Map<Node, Integer> index = problem.getHashIndices();
        // Merged cluster
        Set<Node> mergeCluster = new HashSet<>();
        mergeCluster.addAll(cluster1);
        mergeCluster.addAll(cluster2);
        // initializing score
        double score = 0.0;

        List<Node> mergeClusterList = new ArrayList<>(mergeCluster);

        for (int i = 0; i < mergeClusterList.size(); i++) {
            for (int j = i+1; j < mergeClusterList.size() ; j++) {
                Node nodeI = mergeClusterList.get(i);
                Node nodeJ = mergeClusterList.get(j);
                Edge edge = new Edge(nodeI, nodeJ, Endpoint.TAIL, Endpoint.ARROW);
                System.out.println(edge);
                score+=edgeScores.get(edge);
            }
        }

        //In parallel
        // Maybe it won't work because of the mergeCluster not being concurrent.
        /*
        score = edgeScores.entrySet().parallelStream().filter(edgeDoubleEntry -> {
            Node node1 = edgeDoubleEntry.getKey().getNode1();
            Node node2 = edgeDoubleEntry.getKey().getNode2();
            return mergeCluster.contains(node1) && mergeCluster.contains(node2);
        })
                .map(
                edgeDoubleEntry -> edgeDoubleEntry.getValue()
                )
                .reduce( 0.0, (value1, value2) -> value1 + value2);
        */

        score /= ((double) mergeCluster.size() * (mergeCluster.size() - 1) / 2);
        return score;
    }

    private void deleteClusterInSimMatrix(int posJ){
        // Deleting cluster in position posJ
        clusters.remove(posJ);
        // Making copy
        double[][]auxMatrix = Arrays.stream(simMatrix)
                .map(double[]::clone)
                .toArray(double[][]::new);
        // Reducing by one the size of the simMatrix since the size of the clusters is one less
        simMatrix = new double[clusters.size()][clusters.size()];

        // Row posJ and column posJ will be deleted
        int p = 0;
        for (int i = 0; i < auxMatrix.length; i++) {
            if(i == posJ)
                continue;
            int q = 0;
            for (int j = 0; j < auxMatrix.length; j++) {
                if(j == posJ)
                    continue;
                simMatrix[p][q] = auxMatrix[i][j];
                q++;
            }
            p++;
        }
    }

    public List<Set<Node>> clusterize(int numClusters) {
        //Initial setup
        Map<Edge, Double> scores = getEdgesScore();
        List<Node> nodes = problem.getVariables();
        int numNodes = nodes.size();
        //Initializing clusters
        clusters = new ArrayList<>(nodes.size());
        for (Node n : nodes) {
            Set<Node> s = new HashSet<Node>();
            s.add(n);
            clusters.add(s);
        }

        // Max vars per clusters
        int maxVarsClusters = Math.floorDiv(nodes.size(), numClusters) + 2;

        // Initializing Simmilarity Matrix
        initializeSimMatrix();

        // Calculating clusters

        while (clusters.size() > numClusters) {

            // Initializing scores and initial positions of indexes
            double maxValue = Double.NEGATIVE_INFINITY;
            int posI = -1;
            int posJ = -1;

            //Checking which two clusters are better to merge together
            for (int i = 0; i < numClusters; i++) {
                for (int j = i + 1; j < numClusters; j++) {
                    if (simMatrix[i][j] > maxValue) {
                        maxValue = simMatrix[i][j];
                        posI = i;
                        posJ = j;
                    }
                }
            }
            // Merging the chosen clusters into posI of the clusters list.
            Set<Node> mergeCluster = new HashSet<>();
            mergeCluster.addAll(clusters.get(posI));
            mergeCluster.addAll(clusters.get(posJ));
            clusters.set(posI, mergeCluster);

            //Recalculating simMatrix (Parallel?)
            for (int j = posI + 1; j < numClusters; j++) {
                if (j != posJ) {
                    simMatrix[posI][j] = getScoreClusters(clusters.get(posI), clusters.get(j));
                }
            }
            for (int i = 0; i < posI; i++) {
                simMatrix[i][posI] = getScoreClusters(clusters.get(i), clusters.get(posI));
            }

            // Deleting cluster and the information of posJ in simMatrix
            deleteClusterInSimMatrix(posJ);

        }
        return clusters;
    }


    public List<Set<Edge>> generateEdgeDistribution(List<Set<Node>> clusters, boolean duplicate){
        List<Node> nodes = problem.getVariables();
        List<Set<Edge>> edgeDistribution = new ArrayList<>(clusters.size());
        int [] numEdgesInCluster = new int[clusters.size()];

        //Inner edges
        for (int c = 0; c < clusters.size(); c++) {
            Set<Node> cluster = clusters.get(c);
            // Filtering only the edges where the nodes are contained inside the corresponding cluster
            Set<Edge> innerEdges = allEdges.stream().filter(edge -> {
                Node n1 = edge.getNode1();
                Node n2 = edge.getNode2();
                return cluster.contains(n1) && cluster.contains(n2);
            }).collect(Collectors.toSet());
            // Adding innerEdges
            edgeDistribution.add(innerEdges);
            // Updating number of edges inside this edgeDistribution cluster
            numEdgesInCluster[c] = innerEdges.size();
        }

        //Outer edges
        for (int c1 = 0; c1 < clusters.size()-1; c1++) {
            List<Node> cluster1 = new ArrayList<>(clusters.get(c1));
            Set<Edge> edgesCluster1 = edgeDistribution.get(c1);
            for (int c2 = c1+1; c2 < clusters.size(); c2++) {
                List<Node> cluster2 = new ArrayList<>(clusters.get(c2));
                Set<Edge> edgesCluster2 = edgeDistribution.get(c2);
                // ESTO ES MUY INEFICIENTE... QUIZÁS SE PUEDA HACER EN PARALELO...
                for (Node node1: cluster1) {
                    for (Node node2: cluster2) {
                        Edge candidateEdge = new Edge(node1, node2, Endpoint.TAIL, Endpoint.ARROW);
                        double value = edgeScores.get(candidateEdge);
                        if(duplicate && value > 0){
                            edgesCluster1.add(candidateEdge);
                            edgesCluster2.add(candidateEdge);
                            edgeDistribution.set(c1, edgesCluster1);
                            edgeDistribution.set(c2, edgesCluster2);
                            numEdgesInCluster[c1]+=1;
                            numEdgesInCluster[c2]+=1;
                        }
                        //BOOKMARK!
                        else{

                        }
                    }
                }
            }

        }

        return edgeDistribution;
    }


}
