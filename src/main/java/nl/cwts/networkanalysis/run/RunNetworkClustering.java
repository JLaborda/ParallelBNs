package nl.cwts.networkanalysis.run;

import edu.cmu.tetrad.data.DataSet;
import java.util.Arrays;
import java.util.Random;

import nl.cwts.networkanalysis.CPMClusteringAlgorithm;
import nl.cwts.networkanalysis.Clustering;
import nl.cwts.networkanalysis.IterativeCPMClusteringAlgorithm;
import nl.cwts.networkanalysis.LeidenAlgorithm;
import nl.cwts.networkanalysis.LouvainAlgorithm;
import nl.cwts.networkanalysis.Network;
import org.albacete.simd.utils.Utils;

/**
 * Command line tool for running the Leiden and Louvain algorithms for network
 * clustering.
 *
 * <p>
 * All methods in this class are static.
 * </p>
 *
 * @author Ludo Waltman
 * @author Nees Jan van Eck
 * @author Vincent Traag
 */
public final class RunNetworkClustering
{
    /**
     * Quality function IDs.  
     */
    public static final int CPM = 0;
    public static final int MODULARITY = 1;

    /**
     * Normalization method IDs.
     */
    public static final int NO_NORMALIZATION = 0;
    public static final int ASSOCIATION_STRENGTH = 1;
    public static final int FRACTIONALIZATION = 2;

    /**
     * Clustering algorithm IDs.
     */
    public static final int LEIDEN = 0;
    public static final int LOUVAIN = 1;

    /**
     * Quality function names.
     */
    public static final String[] QUALITY_FUNCTION_NAMES = { "CPM", "Modularity" };

    /**
     * Normalization method names.
     */
    public static final String[] NORMALIZATION_NAMES = { "none", "AssociationStrength", "Fractionalization" };

    /**
     * Clustering algorithm names.
     */
    public static final String[] ALGORITHM_NAMES = { "Leiden", "Louvain" };

    /**
     * Default quality function.
     */
    public static final int DEFAULT_QUALITY_FUNCTION = CPM;

    /**
     * Default normalization method.
     */
    public static final int DEFAULT_NORMALIZATION = NO_NORMALIZATION;

    /**
     * Default clustering algorithm.
     */
    public static final int DEFAULT_ALGORITHM = LEIDEN;

    /**
     * Default resolution parameter.
     */
    public static final double DEFAULT_RESOLUTION = CPMClusteringAlgorithm.DEFAULT_RESOLUTION;

    /**
     * Default minimum cluster size.
     */
    public static final int DEFAULT_MIN_CLUSTER_SIZE = 2;

    /**
     * Default number of random starts.
     */
    public static final int DEFAULT_N_RANDOM_STARTS = 1;

    /**
     * Default number of iterations.
     */
    public static final int DEFAULT_N_ITERATIONS = 10;

    /**
     * Default randomness parameter.
     */
    public static final double DEFAULT_RANDOMNESS = LeidenAlgorithm.DEFAULT_RANDOMNESS;

    /**
     * This method is called when the tool is started.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args)
    {
        boolean useModularity = (DEFAULT_QUALITY_FUNCTION == MODULARITY);
        int normalization = DEFAULT_NORMALIZATION;
        double resolution = DEFAULT_RESOLUTION;
        int minClusterSize = DEFAULT_MIN_CLUSTER_SIZE;
        boolean useLouvain = (DEFAULT_ALGORITHM == LOUVAIN);
        int nRandomStarts = DEFAULT_N_RANDOM_STARTS;
        int nIterations = DEFAULT_N_ITERATIONS;
        double randomness = DEFAULT_RANDOMNESS;

        long seed = 0;
        boolean useSeed = false;
        boolean weightedEdges = false;
        boolean sortedEdgeList = false;
        String finalClusteringFilename = "clustering.txt";
        String edgeListFilename = null;
        
        int maxClusters = 2;

        String networkFolder = "./res/networks/";
        String net_name = "andes";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50001_.csv";
        DataSet ds = Utils.readData(bbdd_path);
        
        
        // Read edge list from file.
        Network network = FileIO.readEdgeList(ds, weightedEdges, sortedEdgeList);
      

        
        // Read initial clustering from file.
        Clustering initialClustering = new Clustering(network.getNNodes());
        
        
        // Run algorithm for network clustering.
        System.err.println("Running " + (useLouvain ? ALGORITHM_NAMES[LOUVAIN] : ALGORITHM_NAMES[LEIDEN]) + " algorithm.");
        System.err.println("Quality function:             " + (useModularity ? QUALITY_FUNCTION_NAMES[MODULARITY] : QUALITY_FUNCTION_NAMES[CPM]));
        if (!useModularity)
            System.err.println("Normalization method:         " + NORMALIZATION_NAMES[normalization]);
        System.err.println("Resolution parameter:         " + resolution);
        System.err.println("Minimum cluster size:         " + minClusterSize);
        System.err.println("Number of random starts:      " + nRandomStarts);
        System.err.println("Number of iterations:         " + nIterations);
        if (!useLouvain)
            System.err.println("Randomness parameter:         " + randomness);
        System.err.println("Random number generator seed: " + (useSeed ? seed : "random"));

        long startTimeAlgorithm = System.currentTimeMillis();
        if (!useModularity)
        {
            if (normalization == NO_NORMALIZATION)
                network = network.createNetworkWithoutNodeWeights();
            else if (normalization == ASSOCIATION_STRENGTH)
                network = network.createNormalizedNetworkUsingAssociationStrength();
            else if (normalization == FRACTIONALIZATION)
                network = network.createNormalizedNetworkUsingFractionalization();
        }
        double resolution2 = useModularity ? (resolution / (2 * network.getTotalEdgeWeight() + network.getTotalEdgeWeightSelfLinks())) : resolution;
        Random random = useSeed ? new Random(seed) : new Random();
        IterativeCPMClusteringAlgorithm algorithm = useLouvain ? new LouvainAlgorithm(resolution2, nIterations, random) : new LeidenAlgorithm(resolution2, nIterations, randomness, random);
        Clustering finalClustering = null;
        double maxQuality = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < nRandomStarts; i++)
        {
            Clustering clustering = initialClustering.clone();
            algorithm.improveClustering(network, clustering);
            double quality = algorithm.calcQuality(network, clustering);
            if (nRandomStarts > 1)
                System.err.println("Quality function in random start " + (i + 1) + " equals " + quality + ".");
            if (quality > maxQuality)
            {
                finalClustering = clustering;
                maxQuality = quality;
            }
        }
        finalClustering.orderClustersByNNodes();
        System.err.println("Running algorithm took " + (System.currentTimeMillis() - startTimeAlgorithm) / 1000 + "s.");

        while (finalClustering.getNClusters() > maxClusters) {
            System.err.println("Clustering consists of " + finalClustering.getNClusters() + " clusters.");
            
            int[] clusters = finalClustering.getNNodesPerCluster();
            Arrays.sort(clusters);
            
            for (int i = 0; i < clusters.length; i++) {
                System.out.println(clusters[i]);
            }
            
            System.out.println("\n\n Num: " + clusters[clusters.length-maxClusters-1]);
            int maxVars = clusters[clusters.length-maxClusters-1] + 1;
            
            System.err.println("Removing clusters consisting of fewer than " + (clusters[clusters.length-maxClusters-1] + 1) + " nodes.");
            
            
            System.out.println("clusters.length: " + clusters.length);
            System.out.println("clusters.length-maxClusters: " + (clusters.length-maxClusters));
            System.out.println("clusters.length-maxClusters-1: " + (clusters.length-maxClusters-1));
            System.out.println("clusters[clusters.length-maxClusters-1]: " + (clusters[clusters.length-maxClusters-1]));
            System.out.println("clusters[clusters.length-maxClusters-1]+1: " + (clusters[clusters.length-maxClusters-1]+1));
            algorithm.removeSmallClustersBasedOnNNodes(network, finalClustering, maxVars);
            finalClustering.removeEmptyClusters();
        }

        
        System.err.println("Final clustering consists of " + finalClustering.getNClusters() + " clusters.");
        
        int[] clusters = finalClustering.getNNodesPerCluster();
        Arrays.sort(clusters);
        for (int i = 0; i < clusters.length; i++) {
            System.out.println(clusters[i]);
        }

        // Write final clustering to file (or to standard output).
        System.err.println("Writing final clustering to " + ((finalClusteringFilename == null) ? "standard output." : "'" + finalClusteringFilename + "'."));
        FileIO.writeClustering(finalClusteringFilename, finalClustering);
    }

    private RunNetworkClustering()
    {
    }
}
