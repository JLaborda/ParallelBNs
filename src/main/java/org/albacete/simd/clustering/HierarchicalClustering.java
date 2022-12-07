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
    private final Map<Edge, Double> edgeScores = new ConcurrentHashMap<>();
    private List<Set<Node>> clusters;
    private final Map<Node, Set<Integer>> nodeClusterMap = new HashMap<>();

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
        if(edgeScores.isEmpty()) {
            //System.out.println("Calculating edges score...");
            calculateEdgesScore();
        }
        return edgeScores;
    }

    private void calculateEdgesScore(){
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

            HashSet<Node> hashParent = new HashSet<>();
            hashParent.add(parent);
            double score = GESThread.localBdeuScore(childIndex, new int[]{parentIndex}, hashParent, problem) -
                    GESThread.localBdeuScore(childIndex, new int[]{}, new HashSet<>(), problem);
            edgeScores.put(edge, score);
            //edgeScores.put(edge.reverse(), score);  // Debería funcionar con Utils.calculateEdges, pero no da buenos resultados
        });
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
        Set<Node> mergeCluster = ConcurrentHashMap.newKeySet();//new HashSet<>();
        mergeCluster.addAll(cluster1);
        mergeCluster.addAll(cluster2);
        // initializing score
        double score = 0.0;
        // Sequential for loop

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


        //In parallel. This takes way too long...
        /*
        score = edgeScores.entrySet().parallelStream().filter(edgeDoubleEntry -> {
            Node node1 = edgeDoubleEntry.getKey().getNode1();
            Node node2 = edgeDoubleEntry.getKey().getNode2();
            return mergeCluster.contains(node1) && mergeCluster.contains(node2);
        })
                .map(
                        Map.Entry::getValue
                )
                .reduce( 0.0, Double::sum);
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

    public List<Set<Node>> clusterize(int numClusters, boolean isJoint) {
        //Initial setup
        getEdgesScore();
        List<Node> nodes = problem.getVariables();

        //Initializing clusters Sequential
        clusters = new ArrayList<>(nodes.size());
        for (Node n : nodes) {
            Set<Node> s = new HashSet<>();
            s.add(n);
            clusters.add(s);
        }

        // Initializing Similarity Matrix
        initializeSimMatrix();

        // Calculating clusters
        while (clusters.size() > numClusters) {

            // Initializing scores and initial positions of indexes
            double maxValue = Double.NEGATIVE_INFINITY;
            int posI = -1;
            int posJ = -1;

            //Checking which two clusters are better to merge together in parallel
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

            //Recalculating simMatrix
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

        // Creating joint clusters if necessary
        if(isJoint){
            createJointClusters();
        }

        //Indexing the cluster nodes
        indexClusters();

        return clusters;
    }

    private List<Set<Node>> createJointClusters(){

        //1. Calculating the number of variables that need to be in each cluster
        int maxVarsClusters = clusters.parallelStream().map(Set::size).max(Integer::compare).orElse(-1);

        //2. For each cluster, find the best nodes to add until the cluster size is equal to maxVarsClusters
        clusters.parallelStream().forEach(cluster -> {
            while(cluster.size() < maxVarsClusters){
                //2.1. Find the best node to add to the cluster
                Node bestNode = null;
                double bestScore = Double.NEGATIVE_INFINITY;
                for(Node node : problem.getVariables()){
                    if(!cluster.contains(node)){
                        Set<Node> auxCluster = new HashSet<>(cluster);
                        auxCluster.add(node);
                        double score = getScoreClusters(auxCluster, cluster);
                        if(score > bestScore){
                            bestScore = score;
                            bestNode = node;
                        }
                    }
                }
                //2.2. Add the best node to the cluster
                cluster.add(bestNode);
            }
        });
        return clusters;
    }

    private void indexClusters() {
        for (int i = 0; i < clusters.size(); i++) {
            Set<Node> nodesCluster = clusters.get(i);
            for (Node node : nodesCluster) {
                Set<Integer> clusterIndexes;
                if(!nodeClusterMap.containsKey(node)) {
                    clusterIndexes = new HashSet<>();
                }
                else{
                    clusterIndexes = nodeClusterMap.get(node);
                }
                clusterIndexes.add(i);
                nodeClusterMap.put(node, clusterIndexes);
            }
        }
    }

    @Override
    public List<Set<Edge>> generateEdgeDistribution(int numClusters, boolean jointClusters) {
        // Generating node clusters
        if(clusters == null) {
            clusterize(numClusters, jointClusters);
        }
        // Generating edge distribution
        List<Set<Edge>> edgeDistribution = new ArrayList<>(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            Set<Edge> edges = new HashSet<>();
            edgeDistribution.add(edges);
        }

        // Generating the Inner and Outer edges using parallelism
        allEdges.parallelStream().forEach(edge -> {
            Node node1 = edge.getNode1();
            Node node2 = edge.getNode2();

            // Both sets indicate where each node is located in each cluster.
            Set<Integer> clusterIndexes1 = nodeClusterMap.get(node1);
            Set<Integer> clusterIndexes2 = nodeClusterMap.get(node2);

            // To calculate the inner edges, we need to check where the nodes repeat themselves in the clusters by means of an intersection.
            Set<Integer> innerClusterIndexes = new HashSet<>(clusterIndexes1);
            innerClusterIndexes.retainAll(clusterIndexes2);
            // To calculate the outer edges, we need to check where the nodes don't repeat themselves in the clusters by means of a difference.
            Set<Integer> outerClusterIndexes = new HashSet<>(clusterIndexes1);
            outerClusterIndexes.removeAll(clusterIndexes2);

            // Adding the edge to each cluster in the innercluster indexes to create inner edges:
            for (Integer innerClusterIndex : innerClusterIndexes) {
                edgeDistribution.get(innerClusterIndex).add(edge);
            }

            // This adds the edge as an outer edge to only one cluster.
            // If the edge is an outer edge, now we add it to the smallest edgeDistrubution cluster.
            if(outerClusterIndexes.size() > 0) {
                int minSize = Integer.MAX_VALUE;
                int minIndex = -1;
                for (Integer outerClusterIndex : outerClusterIndexes) {
                    if (edgeDistribution.get(outerClusterIndex).size() < minSize) {
                        minSize = edgeDistribution.get(outerClusterIndex).size();
                        minIndex = outerClusterIndex;
                    }
                }
                edgeDistribution.get(minIndex).add(edge);
            }

        });


        /*
        Set<Edge> outer = new HashSet<>();

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
            }
            else {
                //Outer edge
                //¿Siempre añadimos el enlace aunque sea malo?
                if (jointClusters && edgeScores.get(edge) > 0) {
                    Set<Edge> edgeCluster1 = edgeDistribution.get(clusterID1);
                    edgeCluster1.add(edge);
                    Set<Edge> edgeCluster2 = edgeDistribution.get(clusterID2);
                    edgeCluster2.add(edge);
                } else {
                    if (!outer.contains(edge.reverse())){
                        if (edgeDistribution.get(clusterID1).size() <= edgeDistribution.get(clusterID2).size()) {
                            Set<Edge> edgeCluster1 = edgeDistribution.get(clusterID1);
                            edgeCluster1.add(edge);
                            edgeCluster1.add(edge.reverse());
                            outer.add(edge.reverse());
                        } else {
                            Set<Edge> edgeCluster2 = edgeDistribution.get(clusterID2);
                            edgeCluster2.add(edge);
                            edgeCluster2.add(edge.reverse());
                            outer.add(edge.reverse());
                        }
                    }
                }
            }
        }*/
        return edgeDistribution;
    }

/*
    //Prueba
    public static void main(String[] args) {
        String networkFolder = "./res/networks/";
        String net_name = "cancer";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        Problem p = new Problem(Utils.readData(bbdd_path));
        System.out.println("Number of nodes: " + p.getVariables().size());
        System.out.println("Number of edges: " + p.getVariables().size() * (p.getVariables().size() - 1) / 2);

        HierarchicalClustering clustering = new HierarchicalClustering(p, true);

        //Map<Edge, Double> map = clustering.getEdgesScore();
        //System.out.println("Mapa: " + map);

        System.out.println("Probando clusterizing");
        long startTime = System.nanoTime();
        List<Set<Node>> clusters = clustering.clusterize(4, true);
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        System.out.println("Tiempo de ejecución del clustering en segundos: " + totalTime/1000000000.0 + " segundos");
        for (int i = 0; i < clusters.size(); i++) {
            //System.out.println("Cluster " + i + ": " + clusters.get(i));
            System.out.println("Number of nodes in cluster" + i + ": " + clusters.get(i).size());
        }

        startTime = System.nanoTime();
        List<Set<Edge>> edgeDistribution = clustering.generateEdgeDistribution(2, true);
        endTime = System.nanoTime();
        totalTime = endTime - startTime;
        System.out.println("Tiempo de ejecución de la distribución de enlaces en segundos: " + totalTime/1000000000.0 + " segundos");

        for (int i = 0; i < edgeDistribution.size(); i++) {
            System.out.println("EdgeDistibution " + i + ": " + edgeDistribution.get(i));
            System.out.println("Number of edges: " + edgeDistribution.get(i).size());
        }

        System.out.println("Different edges:");
        int size = 0;
        int sizeSame = 0;
        for (int i = 0; i < edgeDistribution.size(); i++) {
            for (int j = i+1; j < edgeDistribution.size(); j++) {
                Set<Edge> aux = new HashSet<>(edgeDistribution.get(i));
                aux.removeAll(edgeDistribution.get(j));
                Set<Edge> aux2 = new HashSet<>(edgeDistribution.get(i));
                aux2.retainAll(edgeDistribution.get(j));
                size += aux.size();
                sizeSame += aux2.size();
                System.out.println("Number of different edges between " + i + " and " + j + ": " + aux.size());
                System.out.println("Number of equal edges between " + i + " and " + j + ": " + aux2.size());
            }
        }
        System.out.printf("Number of different edges: %d\n", size);
        System.out.printf("Number of same edges: %d\n", sizeSame);

    }
 */



}
