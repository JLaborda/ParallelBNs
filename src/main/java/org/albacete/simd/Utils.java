package org.albacete.simd;

import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Edge;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Utils {

    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem.
     * @param listOfArcs Array of {@link TupleNode TupleNode} of all the possible edges for the actual problem.
     * @param numSplits The number of splits to do in the listOfArcs.
     * @param seed The random seed used for the splits.
     * @return The subsets of the listOfArcs in an ArrayList of TupleNode.
     */
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



}
