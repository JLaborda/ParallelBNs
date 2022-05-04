package org.albacete.simd.clustering;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HierarchicalClustering extends Clustering{

    private final Object lock = new Object();
    private Set<Edge> allEdges;
    private double[][] simMatrix;
    private boolean isParallel = false;
    private Map<Edge, Double> edgeScores = new ConcurrentHashMap<>();
    private List<Set<Node>> clusters;
    private Map<Node, Integer> nodeClusterMap = new HashMap<>();

    public HierarchicalClustering(){

    }

    public HierarchicalClustering(Problem problem) {
        super(problem);
    }


    public HierarchicalClustering(Problem problem, boolean isParallel) {
        this(problem);
        this.isParallel = isParallel;
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
        for (int i = 0; i < (numNodes - 1); i++) {
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
        this.simMatrix = new double[problem.getVariables().size()][problem.getVariables().size()];
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
        // Merged cluster
        Set<Node> mergeCluster = new HashSet<>();
        mergeCluster.addAll(cluster1);
        mergeCluster.addAll(cluster2);
        // initializing score
        double score = 0.0;

        List<Node> mergeClusterList = new ArrayList<>(mergeCluster);

        for (int i = 0; i < (mergeClusterList.size() - 1); i++) {
            for (int j = i + 1; j < mergeClusterList.size(); j++) {
                Node nodeI = mergeClusterList.get(i);
                Node nodeJ = mergeClusterList.get(j);
                Edge edge = new Edge(nodeI, nodeJ, Endpoint.TAIL, Endpoint.ARROW);
                //System.out.println(edge);
                score += edgeScores.get(edge);
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
            for (int i = 0; i < (clusters.size() - 1); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
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
            for (int j = posI + 1; j < clusters.size(); j++) {
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

        //Indexing the cluster nodes
        for (int i = 0; i < clusters.size(); i++) {
            Set<Node> nodesCluster = clusters.get(i);
            for (Node node : nodesCluster) {
                nodeClusterMap.put(node, i);
            }
        }

        return clusters;
    }


    public List<Set<Edge>> generateEdgeDistribution(int numClusters, boolean duplicate) {
        // Generating node clusters
        clusters = this.clusterize(numClusters);

        List<Set<Edge>> edgeDistribution = new ArrayList<>(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            edgeDistribution.add(new HashSet<>());
        }
        for (Edge edge : allEdges) {
            Node n1 = edge.getNode1();
            Node n2 = edge.getNode2();
            int clusterID1 = nodeClusterMap.get(n1);
            int clusterID2 = nodeClusterMap.get(n2);
            if (clusterID1 == clusterID2) {
                //Inner edge
                Set<Edge> edgeCluster = edgeDistribution.get(clusterID1);
                edgeCluster.add(edge);
                //edgeDistribution.set(clusterID1, edgeCluster);
            } else {
                //Outer edge
                //¿Siempre añadimos el enlace aunque sea malo?
                if (duplicate && edgeScores.get(edge) > 0) {
                    Set<Edge> edgeCluster1 = edgeDistribution.get(clusterID1);
                    edgeCluster1.add(edge);
                    Set<Edge> edgeCluster2 = edgeDistribution.get(clusterID2);
                    edgeCluster2.add(edge);
                } else {
                    if (edgeDistribution.get(clusterID1).size() <= edgeDistribution.get(clusterID2).size()) {
                        Set<Edge> edgeCluster1 = edgeDistribution.get(clusterID1);
                        edgeCluster1.add(edge);
                    } else {
                        Set<Edge> edgeCluster2 = edgeDistribution.get(clusterID2);
                        edgeCluster2.add(edge);
                    }
                }
            }
        }
        return edgeDistribution;
    }

    //Prueba
    /*
    public static void main(String[] args) {
        String networkFolder = "./res/networks/";
        String net_name = "alarm";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50001_.csv";
        Problem p = new Problem(Utils.readData(bbdd_path));
        System.out.println("Number of nodes: " + p.getVariables().size());

        HierarchicalClustering clustering = new HierarchicalClustering(p, false);
        Map<Edge, Double> map = clustering.getEdgesScore();

        System.out.println("Mapa: " + map);

        System.out.println("Probando clusterizing");
        List<Set<Node>> clusters = clustering.clusterize(2);

        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster " + i + ": " + clusters.get(i));
            System.out.println("Number of nodes: " + clusters.get(i).size());
        }

        List<Set<Edge>> edgeDistribution = clustering.generateEdgeDistribution(clusters, true);

        for (int i = 0; i < edgeDistribution.size(); i++) {
            System.out.println("EdgeDistibution " + i + ": " + edgeDistribution.get(i));
            System.out.println("Number of edges: " + edgeDistribution.get(i).size());
        }
    }
     */


}
