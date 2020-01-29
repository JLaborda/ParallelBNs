package org.albacete.simd;

import edu.cmu.tetrad.graph.Edge;

import java.util.*;

public class Utils {

    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem
     */
    public static ArrayList<TupleNode>[] splitArcs(TupleNode[] listOfArcs, int numSplits, long seed){


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

    public static ArrayList<TupleNode>[] splitArcs(List<Edge> edges, int numSplits, long seed){
        // Transforming edges into TupleNodes
        TupleNode[] listOfArcs = new TupleNode[edges.size()];
        for(int i=0; i<edges.size(); i++){
            listOfArcs[i] = edgeToTupleNode(edges.get(i));
        }
        return splitArcs(listOfArcs, numSplits, seed);
    }

    public static TupleNode edgeToTupleNode(Edge edge){
        return new TupleNode(edge.getNode1(), edge.getNode2());
    }


}
