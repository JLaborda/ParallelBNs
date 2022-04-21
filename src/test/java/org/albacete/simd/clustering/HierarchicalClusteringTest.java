package org.albacete.simd.clustering;


import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class HierarchicalClusteringTest {

    String networkFolder = "./res/networks/";
    String net_name = "alarm";
    String net_path = networkFolder + net_name + ".xbif";
    String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50001_.csv";
    Problem p = new Problem(Utils.readData(bbdd_path));


    @Before
    public void setRandomSeed() {
        Utils.setSeed(42);
    }

    @Test
    public void constructorTest() {
        HierarchicalClustering hc1 = new HierarchicalClustering(p);
        HierarchicalClustering hc2 = new HierarchicalClustering(p, false);

        assertNotNull(hc1);
        assertNotNull(hc2);
    }

    @Test
    public void clusterizeTest() {
        HierarchicalClustering clustering = new HierarchicalClustering(p);
        HierarchicalClustering clustering2 = new HierarchicalClustering(p, true);

        Set<String> names1 = new HashSet<>(Arrays.asList("MINVOLSET", "EXPCO2", "SAO2", "PULMEMBOLUS", "PVSAT", "INTUBATION", "MINVOL", "DISCONNECT", "VENTLUNG", "SHUNT", "VENTTUBE", "KINKEDTUBE", "VENTMACH", "ARTCO2", "BP", "VENTALV", "TPR", "PRESS"));
        Set<Node> cluster1 = p.getVariables().stream().filter(node -> names1.contains(node.getName())).collect(Collectors.toSet());
        Set<String> names2 = new HashSet<>(Arrays.asList("ERRCAUTER", "LVEDVOLUME", "HYPOVOLEMIA", "HRBP", "INSUFFANESTH", "STROKEVOLUME", "HRSAT", "CATECHOL", "PCWP", "LVFAILURE", "HR", "FIO2", "ERRLOWOUTPUT", "HISTORY", "PAP", "HREKG", "CVP", "ANAPHYLAXIS", "CO"));
        Set<Node> cluster2 = p.getVariables().stream().filter(node -> names2.contains(node.getName())).collect(Collectors.toSet());


        List<Set<Node>> clusters = clustering.clusterize(2);
        List<Set<Node>> clustersParallel = clustering2.clusterize(2);

        assertEquals(clusters.size(), clustersParallel.size());
        assertEquals(clusters.get(0).size(), clustersParallel.get(0).size());
        assertEquals(clusters.get(1).size(), clustersParallel.get(1).size());

        for (Node node : cluster1) {
            assertTrue(clusters.get(0).contains(node));
            assertTrue(clustersParallel.get(0).contains(node));
        }
        for (Node node : cluster2) {
            assertTrue(clusters.get(1).contains(node));
            assertTrue(clustersParallel.get(1).contains(node));
        }
    }

    @Test
    public void edgeDistributionTest() {
        HierarchicalClustering clustering = new HierarchicalClustering(p);

        //List<Set<Node>> clusters = clustering.clusterize(2);
        List<Set<Edge>> edgeDistribution = clustering.generateEdgeDistribution(2, false);

        // Checking that there is the same distribution in both clusters
        assertEquals(2, edgeDistribution.size());
        assertTrue(edgeDistribution.get(0).size() >= 650 && edgeDistribution.get(0).size() <= 675);
        assertTrue(edgeDistribution.get(1).size() >= 650 && edgeDistribution.get(1).size() <= 675);

    }

    @Test
    public void edgeDistributionDuplicateTest() {
        HierarchicalClustering clustering = new HierarchicalClustering(p);

        //List<Set<Node>> clusters = clustering.clusterize(2);
        List<Set<Edge>> edgeDistribution = clustering.generateEdgeDistribution(2, true);

        assertEquals(2, edgeDistribution.size());

        System.out.println("edgeDistribution0: " + edgeDistribution.get(0).size());
        System.out.println("edgeDistribution1: " + edgeDistribution.get(1).size());

        assertTrue(edgeDistribution.get(0).size() >= 725 && edgeDistribution.get(0).size() <= 745);
        assertTrue(edgeDistribution.get(1).size() >= 725 && edgeDistribution.get(1).size() <= 745);
    }
}
