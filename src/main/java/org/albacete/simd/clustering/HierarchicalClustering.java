package org.albacete.simd.clustering;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HierarchicalClustering {

    private final Object lock = new Object();
    private Problem problem;
    private Set<Edge> allEdges;
    private double[][] simMatrix;
    private boolean isParallel = false;
    private Map<Edge, Double> edgeScores = new ConcurrentHashMap<>();

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
        String net_name = "andes";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50001_.csv";
        Problem p = new Problem(Utils.readData(bbdd_path));

        HierarchicalClustering clustering = new HierarchicalClustering(p, false);
        Map<Edge, Double> map = clustering.getEdgesScore();

        System.out.println("Mapa: " + map);

        System.out.println("Probando clusterizing");
        clustering.clusterize(2);

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

    private void getSimMatrixSequential() {
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

    private void getSimMatrixParallel() {
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

    private void calculateSimMatrix() {
        if (isParallel)
            getSimMatrixParallel();
        else
            getSimMatrixSequential();

    }

    private void addValueSimMatrix(int i, int j, double score) {
        synchronized (lock) {
            simMatrix[i][j] = score;
        }
    }

    private double getScoreClusrters(Set<Node> cluster1, Set<Node> cluster2) {
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

/* BOOKMARK!
        for (int i = 0; i < mergeCluster.size(); i++) {
            for (int j = i; j < mergeCluster.size() ; j++) {
                Node nodeI = mergeClusterArray.
                score+=
            }
        }

 */


        return score;
    }

    public List<Set<Node>> clusterize(int numClusters) {
        //Initial setup
        Map<Edge, Double> scores = getEdgesScore();
        List<Node> nodes = problem.getVariables();
        int numNodes = nodes.size();
        //Initializing clusters
        List<Set<Node>> clusters = new ArrayList<>(nodes.size());
        for (Node n : nodes) {
            Set<Node> s = new HashSet<Node>();
            s.add(n);
            clusters.add(s);
        }

        // Max vars per clusters
        int maxVarsClusters = Math.floorDiv(nodes.size(), numClusters) + 2;

        // Initializing Simmilarity Matrix
        calculateSimMatrix();

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
            // Merging the chosen clusters
            Set<Node> mergeCluster = new HashSet<>();
            mergeCluster.addAll(clusters.get(posI));
            mergeCluster.addAll(clusters.get(posJ));
            clusters.set(posI, mergeCluster);

            //Recalculating simMatrix (Parallel?)
            for (int j = posI + 1; j < numClusters; j++) {
                if (j != posJ) {
                    // BOOKMARK!
                    //simMatrix[posI][j] = getScoreClusrters()
                }
            }


        }


        return clusters;
    }


}
