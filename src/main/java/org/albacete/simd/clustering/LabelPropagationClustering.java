package org.albacete.simd.clustering;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.albacete.simd.utils.Problem;

public class LabelPropagationClustering extends ClusteringBES {
    
    private HashMap<Node,LabelNode> nodes;
    
    private ArrayList<LabelNode> nodesList;
    
    private Set<Edge> edges;
    
    private HashMap<Integer,Integer> clases;

    private int numClusters;
    
    
    private long startTime;
    private long endTime;
    
    
    public LabelPropagationClustering() {
        
    }
    
    public LabelPropagationClustering(Problem problem) {
        super(problem);
    }
    
    public LabelPropagationClustering(Problem problem, Graph graph) {
        super(problem, graph);
    }

    @Override
    public List<Set<Edge>> generateEdgeDistribution(int numClusters, boolean duplicate) {
        this.numClusters = numClusters;
        
        // Initialize the nodes
        generateNodes();
        
        // Realize the Label Propagation algorithm
        propagateLabels();
        
        // If the algorithm can't generate as low clusters as we want
        if (clases.size() > numClusters) {
            joinClusters();
        }

        // Return the edges in each cluster
        HashMap<Integer,Set<Edge>> edgeDistribution = new HashMap<>(numClusters);
        for (Integer clase : clases.keySet()) {
            edgeDistribution.put(clase, new HashSet<>());
        }

        Set<Edge> outer = new HashSet<>();
        for (Edge edge : edges) {
            Node n1 = edge.getNode1();
            Node n2 = edge.getNode2();
            int clusterID1 = nodes.get(n1).clase;
            int clusterID2 = nodes.get(n2).clase;
            if (clusterID1 == clusterID2) {
                //Inner edge
                Set<Edge> edgeCluster = edgeDistribution.get(clusterID1);
                edgeCluster.add(edge);
            } else {
                //Outer edge
                if (duplicate) {
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
        }
        
        List list = new ArrayList(edgeDistribution.values());
        
        
        
        endTime = System.currentTimeMillis();
        long elapsedTime = endTime-startTime;
        System.out.println("\n\n\n            TIEMPO CLUSTERING LP: " + (double) elapsedTime/1000+", clases.size(): "+ clases.size()+"\n\n\n");
        
        
        return list;
    }
    
    
    private void generateNodes() {
        startTime = System.currentTimeMillis();
        nodes = new HashMap<>();
        
        nodesList = new ArrayList<>();
        
        edges = getGraph().getEdges();
        
        int classCount = 0;

        // Create a LabelNode for each Node into nodes set, adding their neighbors.
        // Also, set a unique class for each LabelNode with some links
        for(Edge edge : edges) {
            Node node1 = edge.getNode1();
            Node node2 = edge.getNode2();
            if (!nodes.containsKey(node1)) {
                LabelNode ln1 = new LabelNode(node1,classCount);
                nodes.put(node1, ln1);
                nodesList.add(ln1);
                classCount++;
            }
            
            if (!nodes.containsKey(node2)) {
                LabelNode ln2 = new LabelNode(node2,classCount);
                nodes.put(node2, ln2);
                nodesList.add(ln2);
                classCount++;
            }
            
            nodes.get(node1).addNeighbor(nodes.get(node2));
            nodes.get(node2).addNeighbor(nodes.get(node1));
        }

        // Initially, there are n clusters, and each class have 1 node
        this.clases = new HashMap(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            this.clases.put(i, 1);
        }
    }
    
    
    private void propagateLabels() {
        int actualNum = clases.size();
        // Iterate until obtaining the desired num of clusters
        while(clases.size() > numClusters) {
            // Change the label of each node to the one that appears on the most neighbors
            for (LabelNode node : nodesList){
                int maxFreqLabel = node.maxFreq();
                if (maxFreqLabel != node.clase){
                    this.clases.put(node.clase, this.clases.get(node.clase) - 1);
                    this.clases.put(maxFreqLabel, this.clases.get(maxFreqLabel) + 1);

                    if (this.clases.get(node.clase) == 0) {
                        this.clases.remove(node.clase);
                    }

                    node.clase = maxFreqLabel;
                    
                    if (clases.size() == numClusters) break;
                }
            }
            
            // Check if we have removed any class this iteration
            if (clases.size() == actualNum) break;
            else actualNum = clases.size();
            
            System.out.println("\nClases: " + clases.size());
            for (Integer name: clases.keySet()) {
                System.out.println(name + " " + clases.get(name));
            }
        }
    }
    
    
    /* If we have N threads, order the clusters and then join the cluster N with 
    the cluster N+1. That is, join the smallest of the clusters that would be 
    the final solution up to this point, with the largest of the remaining clusters.
            
    For example, if we have 2 threads and clusters of size 20,19,12,11; we join 
    the cluster 19 with 12 to obtain 31,20,11, and then 20 with 11 obtaining a 
    perfect result of 31,31.
            
    If we would join the smallest clusters first, we join 12 with 11 obtaining 
    23,20,19; and then 20 with 19 obtaining 39,19; which is not the optimal solution */
    private void joinClusters() {
        // While we don't have the number of clusters that we want
        while(clases.size() > numClusters) {
            // Order the classes
            LinkedHashMap<Integer, Integer> clasesSorted = 
                (LinkedHashMap<Integer, Integer>) clases.entrySet().stream()
               .sorted(Entry.comparingByValue())
               .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                                         (e1, e2) -> e1, LinkedHashMap::new));
            
            System.out.println("\nORDENADO");
            for (Integer name: clasesSorted.keySet()) {
                System.out.println(name + " " + clasesSorted.get(name));
            }

            // Join the N class with the N+1 class
            int i = 0;
            int labelN = 0;
            ArrayList<Integer> keySet = new ArrayList(clasesSorted.keySet());
            Collections.reverse(keySet);
            for (int label : keySet) {
                // The label that we have to mantain
                if (i == numClusters-1) {
                    labelN = label;
                }
                
                // Convert labels of i to labelN
                if (i == numClusters) {
                    for (LabelNode node : nodes.values()) {
                        if (node.clase == label) {
                            node.clase = labelN;
                            clases.put(labelN, clases.get(labelN)+1);
                        }
                    }
                    clases.remove(label);
                }
                
                if (i > numClusters) break;
                i++;
            }
        }
        
        System.out.println("\nClases: " + clases.size());
        for (Integer name: clases.keySet()) {
            System.out.println(name + " " + clases.get(name));
        }
    }
}



class LabelNode {
    Node node;
    Set<LabelNode> neighbors;
    int clase;
    
    public LabelNode(Node node, int classCount) {
        this.node = node;
        this.clase = classCount;
        neighbors = new HashSet<>();
    }

    public void addNeighbor(LabelNode neighbor){
        neighbors.add(neighbor);
    }
    
    // Returns the most frequently occurring label among all adjacent labels
    public int maxFreq(){
        // Create a Map with Label:Appearances
        HashMap<Integer,Integer> map = new HashMap<>();
        for(LabelNode neighbor : neighbors){
            if(map.containsKey(neighbor.clase)){
                map.put(neighbor.clase, map.get(neighbor.clase)+1);
            }else{
                map.put(neighbor.clase,1);
            }
        }
        
        // Return the most frequent label
        int bestLabel = 0;
        int maxValue = 0;
        for (int label : map.keySet()) {
            if (map.get(label) > maxValue){
                maxValue = map.get(label);
                bestLabel = label;
            }
        }
        
        return bestLabel;
    }
}



