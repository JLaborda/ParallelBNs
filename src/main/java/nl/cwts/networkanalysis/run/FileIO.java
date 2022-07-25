package nl.cwts.networkanalysis.run;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Node;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import nl.cwts.networkanalysis.Clustering;
import nl.cwts.networkanalysis.Layout;
import nl.cwts.networkanalysis.Network;
import nl.cwts.util.DynamicDoubleArray;
import nl.cwts.util.DynamicIntArray;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

/**
 * Utility functions for file I/O.
 *
 * <p>
 * All methods in this class are static.
 * </p>
 *
 * @author Ludo Waltman
 * @author Nees Jan van Eck
 * @author Vincent Traag
 */
public class FileIO
{
    /**
     * Column separator for edge list, clustering, and layout files.
     */
    public static final String COLUMN_SEPARATOR = "\t";

    /**
     * Reads an edge list from a file and creates a network.
     *
     * @param ds             DataSet
     * @param weightedEdges  Indicates whether edges have weights
     * @param sortedEdgeList Indicates whether the edge list is sorted
     *
     * @return Network
     */
    public static Network readEdgeList(DataSet ds, boolean weightedEdges, boolean sortedEdgeList)
    {
        // Read edge list.
        DynamicIntArray[] edges = new DynamicIntArray[2];
        edges[0] = new DynamicIntArray(100);
        edges[1] = new DynamicIntArray(100);

        int nNodes = 0;
        Problem problem = new Problem(ds);
        
        System.out.println("\n\n\n\n\n" + problem.getVariables() + "\n\n\n\n\n\n");

        
        HashMap<Node,Integer> hashNodes = new HashMap<>();
        List<Node> nodeList = problem.getVariables();
        for (int i = 0; i < nodeList.size(); i++) {
            hashNodes.put(nodeList.get(i), i);
        }
        
        Set<Edge> allEdges = Utils.calculateEdges(problem.getData());

        Random ran = new Random();
        for (Edge edge : allEdges) {
            if (ran.nextInt(200) == 0){
                edges[0].append(hashNodes.get(edge.getNode1()));
                edges[1].append(hashNodes.get(edge.getNode2()));
                System.out.println("Enlace " + hashNodes.get(edge.getNode1()) + "-" + hashNodes.get(edge.getNode2()) + ",  " + edge.getNode1() + "-" + edge.getNode2());
            }
        }

        // Create network.
        Network network = null;
        int[][] edges2 = new int[2][];
        edges2[0] = edges[0].toArray();
        edges2[1] = edges[1].toArray();

        network = new Network(problem.getnValues().length, true, edges2, sortedEdgeList, true);
        
        return network;
    }

    /**
     * Reads a clustering from a file.
     *
     * @param filename Filename
     * @param nNodes   Number of nodes
     *
     * @return Clustering
     */
    public static Clustering readClustering(String filename, int nNodes)
    {
        int[] clusters = new int[nNodes];
        Arrays.fill(clusters, -1);
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            int lineNo = 0;
            while (line != null)
            {
                lineNo++;
                String[] columns = line.split(COLUMN_SEPARATOR);
                if (columns.length != 2)
                    throw new IOException("Incorrect number of columns (line " + lineNo + ").");

                int node;
                try
                {
                    node = Integer.parseUnsignedInt(columns[0]);
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Node must be represented by a zero-index integer number (line " + lineNo + ").");
                }
                if (node >= nNodes)
                    throw new IOException("Invalid node (line " + lineNo + ").");
                int cluster;
                try
                {
                    cluster = Integer.parseUnsignedInt(columns[1]);
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Cluster must be represented by a zero-index integer number (line " + lineNo + ").");
                }
                if (clusters[node] >= 0)
                    throw new IOException("Duplicate node (line " + lineNo + ").");
                clusters[node] = cluster;

                line = reader.readLine();
            }
            if (lineNo < nNodes)
                throw new IOException("Missing nodes.");
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error while reading clustering from file: File not found.");
            System.exit(-1);
        }
        catch (IOException e)
        {
            System.err.println("Error while reading clustering from file: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (reader != null)
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    System.err.println("Error while reading clustering from file: " + e.getMessage());
                    System.exit(-1);
                }
        }

        return new Clustering(clusters);
    }

    /**
     * Writes a clustering to a file.
     *
     * @param filename   Filename
     * @param clustering Clustering
     */
    public static void writeClustering(String filename, Clustering clustering)
    {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter((filename == null) ? new OutputStreamWriter(System.out) : new FileWriter(filename));
            for (int i = 0; i < clustering.getNNodes(); i++)
            {
                writer.write(i + COLUMN_SEPARATOR + clustering.getCluster(i));
                writer.newLine();
            }
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error while writing clustering to file: File not found.");
            System.exit(-1);
        }
        catch (IOException e)
        {
            System.err.println("Error while writing clustering to file: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    System.err.println("Error while writing clustering to file: " + e.getMessage());
                    System.exit(-1);
                }
        }
    }

    /**
     * Reads a layout from a file.
     *
     * @param filename Filename
     * @param nNodes   Number of nodes
     *
     * @return Layout
     */
    public static Layout readLayout(String filename, int nNodes)
    {
        double[][] coordinates = new double[2][nNodes];
        boolean[] hasCoordinates = new boolean[nNodes];
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            int lineNo = 0;
            while (line != null)
            {
                lineNo++;
                String[] columns = line.split(COLUMN_SEPARATOR);
                if (columns.length != 3)
                    throw new IOException("Incorrect number of columns (line " + lineNo + ").");

                int node;
                try
                {
                    node = Integer.parseUnsignedInt(columns[0]);
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Node must be represented by a zero-index integer number (line " + lineNo + ").");
                }
                if (node >= nNodes)
                    throw new IOException("Invalid node (line " + lineNo + ").");
                double x;
                double y;
                try
                {
                    x = Double.parseDouble(columns[1]);
                    y = Double.parseDouble(columns[2]);
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Coordinates must be numbers (line " + lineNo + ").");
                }
                if (hasCoordinates[node])
                    throw new IOException("Duplicate node (line " + lineNo + ").");
                coordinates[0][node] = x;
                coordinates[1][node] = y;
                hasCoordinates[node] = true;

                line = reader.readLine();
            }
            if (lineNo < nNodes)
                throw new IOException("Missing nodes.");
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error while reading layout from file: File not found.");
            System.exit(-1);
        }
        catch (IOException e)
        {
            System.err.println("Error while reading layout from file: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (reader != null)
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    System.err.println("Error while reading layout from file: " + e.getMessage());
                    System.exit(-1);
                }
        }

        return new Layout(coordinates);
    }

    /**
     * Writes a layout to a file.
     *
     * @param filename Filename
     * @param layout   Layout
     */
    public static void writeLayout(String filename, Layout layout)
    {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter((filename == null) ? new OutputStreamWriter(System.out) : new FileWriter(filename));
            for (int i = 0; i < layout.getNNodes(); i++)
            {
                double[] coordinates = layout.getCoordinates(i);
                writer.write(i + COLUMN_SEPARATOR + coordinates[0] + COLUMN_SEPARATOR + coordinates[1]);
                writer.newLine();
            }
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error while writing layout to file: File not found.");
            System.exit(-1);
        }
        catch (IOException e)
        {
            System.err.println("Error while writing layout to file: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    System.err.println("Error while writing layout to file: " + e.getMessage());
                    System.exit(-1);
                }
        }
    }

    private FileIO()
    {
    }
}
