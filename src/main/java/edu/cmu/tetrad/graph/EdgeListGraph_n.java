package edu.cmu.tetrad.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Pablo Torrijos Arenas
 */
public class EdgeListGraph_n extends EdgeListGraph {
    
    final HashMap<Node,Set<Node>> neighboursMap;
        
    public EdgeListGraph_n(){
        super();
        this.neighboursMap = new HashMap();
    }
    
    public EdgeListGraph_n(Graph graph){
        super(graph);        
        this.neighboursMap = new HashMap();
        for (Node node : this.nodes) {
            this.neighboursMap.put(node, new HashSet<>());
        }
    }
    
    public EdgeListGraph_n(List<Node> nodes){
        super(nodes);
        this.neighboursMap = new HashMap();
        for (Node node : this.nodes) {
            this.neighboursMap.put(node, new HashSet<>());
        }
    }
    
    /**
     * Determines whether some edge or other exists between two nodes.
     * @param node1
     * @param node2
     * @return 
     */
    @Override
    public boolean isAdjacentTo(Node node1, Node node2) {
        if (node1 == null || node2 == null || this.edgeLists.get(node1) == null || this.edgeLists.get(node2) == null) {
            return false;
        }

        return neighboursMap.get(node1).contains(node2) && neighboursMap.get(node2).contains(node1);
    }
    
    /**
     * Adds an edge to the graph.
     *
     * @param edge the edge to be added
     * @return true if the edge was added, false if not.
     */
    @Override
    public boolean addEdge(Edge edge) {
        synchronized (this.edgeLists) {
            if (edge == null) {
                throw new NullPointerException();
            }

            this.edgeLists.get(edge.getNode1()).add(edge);
            this.edgeLists.get(edge.getNode2()).add(edge);

            this.edgesSet.add(edge);
            
            // Ahora ambos nodos son vecinos
            this.neighboursMap.get(edge.getNode1()).add(edge.getNode2());
            this.neighboursMap.get(edge.getNode2()).add(edge.getNode1());

            if (Edges.isDirectedEdge(edge)) {
                Node node = Edges.getDirectedEdgeTail(edge);

                if (node.getNodeType() == NodeType.ERROR) {
                    getPcs().firePropertyChange("nodeAdded", null, node);
                }
            }

            this.ancestors = null;
            getPcs().firePropertyChange("edgeAdded", null, edge);
            return true;
        }
    }
    

    /**
     * Removes an edge from the graph. (Note: It is dangerous to make a
     * recursive call to this method (as it stands) from a method containing
     * certain types of iterators. The problem is that if one uses an iterator
     * that iterates over the edges of node A or node B, and tries in the
     * process to remove those edges using this method, a concurrent
     * modification exception will be thrown.)
     *
     * @param edge the edge to remove.
     * @return true if the edge was removed, false if not.
     */
    @Override
    public boolean removeEdge(Edge edge) {
        synchronized (this.edgeLists) {
            if (!this.edgesSet.contains(edge)) {
                return false;
            }

            Set<Edge> edgeList1 = this.edgeLists.get(edge.getNode1());
            Set<Edge> edgeList2 = this.edgeLists.get(edge.getNode2());

            edgeList1 = new HashSet<>(edgeList1);
            edgeList2 = new HashSet<>(edgeList2);
            
            // Si no existe el enlace inverso, dejan de ser vecinos
            if (edgesSet.contains(edge.reverse())){
                this.neighboursMap.get(edge.getNode1()).remove(edge.getNode2());
                this.neighboursMap.get(edge.getNode2()).remove(edge.getNode1());
            }
            
            this.edgesSet.remove(edge);
            edgeList1.remove(edge);
            edgeList2.remove(edge);

            this.edgeLists.put(edge.getNode1(), edgeList1);
            this.edgeLists.put(edge.getNode2(), edgeList2);

            this.highlightedEdges.remove(edge);
            this.stuffRemovedSinceLastTripleAccess = true;

            this.ancestors = null;
            getPcs().firePropertyChange("edgeRemoved", edge, null);
            return true;
        }
    }
    
    /**
     * @param node1
     * @param node2
     * @return the edges connecting node1 and node2.
     */
    @Override
    public List<Edge> getEdges(Node node1, Node node2) {
        if (!isAdjacentTo(node1, node2)) {
            return new ArrayList<>();
        }
        
        Set<Edge> edges = this.edgeLists.get(node1);
        if (edges == null) {
            return new ArrayList<>();
        }

        List<Edge> _edges = new ArrayList<>();

        edges.stream().filter(edge -> (edge.getDistalNode(node1) == node2)).forEachOrdered(edge -> {
            _edges.add(edge);
        });

        return _edges;
    }
    
    
    
    
    class nodesPair {
        Node node1;
        Node node2;
        
        public nodesPair(Node node1, Node node2){
            this.node1 = node1;
            this.node2 = node2;
        }
        
        @Override
        public boolean equals(Object o) {
            // self check
            if (this == o)
                return true;
            // null check
            if (o == null)
                return false;
            // type check and cast
            if (getClass() != o.getClass())
                return false;
            nodesPair nodes = (nodesPair) o;
            // field comparison
            return node1.equals(nodes.node1)
                    && node2.equals(nodes.node2);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + Objects.hashCode(this.node1);
            hash = 23 * hash + Objects.hashCode(this.node2);
            return hash;
        }

    }
    
}
