package org.albacete.simd.utils;

import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.junit.Test;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import static org.junit.Assert.*;

import java.util.*;

/**
 * Test cases for the Utils class
 */
public class UtilsTest {


    final String path1 = "src/test/resources/repeatedNames.csv";
    final String path2 = "src/test/resources/cancer.xbif_.csv";
    /**
     * Dataset created from the data file1
     */
    //final DataSet datasetRepeated = Utils.readData(path1);
    final DataSet dataset = Utils.readData(path2);




    /**
     * Tests that a Utils object can be created
     * @result  Utils object not null
     */
    @Test
    public void constructorTest(){
        //Arrange
        Utils utils;

        //Act
        utils = new Utils();

        //Assert
        assertNotNull(utils);
    }

    /**
     * Tests that the method split for tuple nodes splits an array of TupleNode into two subsets correctly.
     * @result An ArrayList with two subset of TupleNode
     */
    @Test
    public void splitTest(){
        //Arrange
        Node n1 = new GraphNode("n1");
        Node n2 = new GraphNode("n2");
        Node n3 = new GraphNode("n3");
        List<Edge> edges = Arrays.asList(Edges.directedEdge(n1,n2), Edges.directedEdge(n1,n3));
        int seed = 42;
        int expectedSize = 2;

        //Act
        Utils.setSeed(seed);
        List<List<Edge>> result = Utils.split(edges, 2);

        //Assert
        assertEquals(expectedSize, result.size());

    }


    /**
     * Tests that the method readData loads data correctly into a DataSet.
     * @result The DataSet is created and the number of columns and of rows correspond with what the data actually has.
     */
    @Test
    public void readDataTest(){
        // Arrange
        String path = "src/test/resources/cancer.xbif_.csv";

        //Act
        DataSet result = Utils.readData(path);

        //Assert
        assertNotNull(result);
        assertEquals(5, result.getNumColumns());
        assertEquals(5000, result.getNumRows());

    }

    @Test
    public void compareTest() throws Exception {

        /*TEST: Comparing the same bn should return 0*/
        // Arrange
        String net_path = "./res/networks/cancer.xbif";
        BIFReader bf = new BIFReader();
        bf.processFile(net_path);
        BayesNet bn = (BayesNet) bf;
        MlBayesIm bn2 = new MlBayesIm(bn);

        // Act
        double result = Utils.compare(bn2.getDag(), bn2.getDag());

        // Assert
        assertEquals(0.0, result, 0.000001);

        /*TEST: Empty Dag against normal Dag should return the number of edges of the normal Dag*/
        Dag dag1 = bn2.getDag();
        Dag dag2 = new Dag(bn2.getDag().getNodes());

        int expected = dag1.getNumEdges();

        result = Utils.compare(dag1, dag2);
        assertEquals(expected,result,0.000001);

    }

    @Test
    public void markovBlanquetTest() throws Exception {
        // Arranging: Loading the cancer network
        String net_path1 = "./res/networks/cancer.xbif";
        BIFReader bf = new BIFReader();
        bf.processFile(net_path1);
        MlBayesIm bn1 = new MlBayesIm((BayesNet) bf);
        Dag dag = bn1.getDag();

        // Setting expected outcome
        Map<Node,List<Node>> expected = new HashMap<>();
        expected.put(dag.getNode("Pollution"), Arrays.asList(dag.getNode("Cancer"), dag.getNode("Smoker")));
        expected.put(dag.getNode("Smoker"), Arrays.asList(dag.getNode("Cancer"), dag.getNode("Pollution")));
        expected.put(dag.getNode("Cancer"), Arrays.asList(dag.getNode("Pollution"), dag.getNode("Smoker"),
                dag.getNode("Xray"), dag.getNode("Dyspnoea")));
        expected.put(dag.getNode("Xray"), Arrays.asList(dag.getNode("Cancer")));
        expected.put(dag.getNode("Dyspnoea"), Arrays.asList(dag.getNode("Cancer")));


        // Acting: Getting MB for every node
        Map<String,List<Node>> mbs = new HashMap<>();
        for (Node n: dag.getNodes() ) {
            List<Node> result = Utils.getMarkovBlanket(dag,n);
            List<Node> exp = expected.get(n);

            //Asserting result
            assertEquals(result.size(), exp.size());
            assertFalse(result.contains(n));

            for(Node e : exp){
                assertTrue(result.contains(e));
            }
            for(Node r : result){
                assertTrue(exp.contains(r));
            }
        }


    }

    @Test
    public void avgMarkovBlanquetDifTest() throws Exception {
        /*TEST: Different Dags should return null*/
        String net_path1 = "./res/networks/cancer.xbif";
        String net_path2 = "./res/networks/alarm.xbif";
        BIFReader bf = new BIFReader();

        // Arranging dags of alarm and cancer
        bf.processFile(net_path1);
        MlBayesIm bn1 = new MlBayesIm((BayesNet) bf);
        bf.processFile(net_path2);
        MlBayesIm bn2 = new MlBayesIm((BayesNet) bf);

        // Acting: Getting the avgMarkovBlanquetDif:
        double[] result = Utils.avgMarkovBlanquetdif(bn1.getDag(), bn2.getDag());
        // Asserting
        assertNull(result);

        /*TEST: Same DAGs should return the following array [0.0,0.0,0.0]*/
        // Arranging dags for the same data
        bf.processFile(net_path1);
        bn1 = new MlBayesIm((BayesNet) bf);
        bn2 = new MlBayesIm((BayesNet) bf);

        // Acting: Getting the avgMarkovBlanquetDif:
        result = Utils.avgMarkovBlanquetdif(bn1.getDag(), bn2.getDag());
        // Asserting
        for(double r: result){
            assertEquals(0,r,0.000001);
        }

        /*TEST: Same nodes but different DAGs should return it's avg difference*/
        // Arranging dags
        bf.processFile(net_path1);
        Dag dag1 = (new MlBayesIm((BayesNet) bf)).getDag();
        Dag dag2 = (new MlBayesIm((BayesNet) bf)).getDag();

        // Changing the original dag
        dag2.removeEdge(dag2.getNode("Cancer"), dag2.getNode("Dyspnoea"));
        dag2.addDirectedEdge(dag2.getNode("Xray"), dag2.getNode("Dyspnoea"));

        System.out.println(dag2);

        // Acting: Calculating average MB
        result = Utils.avgMarkovBlanquetdif(dag1, dag2);

        // Asserting
        double expected_mbavg = 4.0/5.0;
        double expected_mbplus = 2;
        double expected_mbminus = 2;

        assertEquals(expected_mbavg, result[0], 0.000001);
        assertEquals(expected_mbplus, result[1], 0.000001);
        assertEquals(expected_mbminus, result[2 ], 0.000001);
    }

    @Test
    public void getNodeByNameTest() throws Exception {
        BIFReader bf = new BIFReader();
        bf.processFile("res/networks/cancer.xbif");
        BayesNet bn = (BayesNet) bf;
        System.out.println("Numero de variables: "+bn.getNrOfNodes());
        MlBayesIm bn2 = new MlBayesIm(bn);

        Dag dag = bn2.getDag();

        Node n = Utils.getNodeByName(dag.getNodes(), "Pollution");
        Node n2 = Utils.getNodeByName(dag.getNodes(), "");

        assertNotNull(n);
        assertNull(n2);
    }

    @Test
    public void getIndexOfNodesByNameTest() throws Exception {
        BIFReader bf = new BIFReader();
        bf.processFile("res/networks/cancer.xbif");
        BayesNet bn = (BayesNet) bf;
        System.out.println("Numero de variables: "+bn.getNrOfNodes());
        MlBayesIm bn2 = new MlBayesIm(bn);

        Dag dag = bn2.getDag();
        List<Node> nodes = dag.getNodes();

        int result1 = Utils.getIndexOfNodeByName(nodes, "Pollution");
        int result2 = Utils.getIndexOfNodeByName(nodes, "");

        assertEquals(3, result1);
        assertEquals(-1, result2);

    }

    @Test
    public void removeInconsistenciesTest(){
        Graph g1 = new EdgeListGraph();
        Graph g2 = new EdgeListGraph();
        Node n1 = new GraphNode("Node1");
        Node n2 = new GraphNode("Node2");
        Node n3 = new GraphNode("Node3");
        g1.addNode(n1);
        g1.addNode(n2);
        g1.addNode(n3);
        g2.addNode(n1);
        g2.addNode(n2);
        g2.addNode(n3);

        Edge e1 = Edges.directedEdge(n1, n2);
        Edge e2 = Edges.directedEdge(n2, n3);
        Edge e3 = Edges.directedEdge(n3, n1);

        g1.addEdge(e1);
        g1.addEdge(e2);
        g2.addEdge(e1);
        g2.addEdge(e2);
        g2.addEdge(e3);

        Dag result1 = Utils.removeInconsistencies(g1);
        Dag result2 = Utils.removeInconsistencies(g2);

        assertTrue(result1.containsEdge(e1));
        assertTrue(result1.containsEdge(e2));
        assertFalse(result1.containsEdge(e3));

        assertFalse(result2.containsEdge(e1));
        assertTrue(result2.containsEdge(e2));
        assertTrue(result2.containsEdge(e3));


    }

    @Test
    public void calculateArcsTest(){

        // Checking cancer dataset
        //Arrange
        Node xray = dataset.getVariable("Xray");
        Node cancer = dataset.getVariable("Cancer");
        Node pollution = dataset.getVariable("Pollution");
        Node smoker = dataset.getVariable("Smoker");
        Node dypnoea = dataset.getVariable("Dyspnoea");

        List<Edge> expected = Arrays.asList(
                Edges.directedEdge(xray, smoker), Edges.directedEdge(xray, cancer), Edges.directedEdge(xray, pollution), Edges.directedEdge(xray, dypnoea),
                Edges.directedEdge(smoker, xray), Edges.directedEdge(cancer, xray), Edges.directedEdge(pollution, xray), Edges.directedEdge(dypnoea, xray),
                Edges.directedEdge(smoker, cancer), Edges.directedEdge(smoker, pollution), Edges.directedEdge(smoker, dypnoea),
                Edges.directedEdge(cancer, smoker), Edges.directedEdge(pollution, smoker), Edges.directedEdge(dypnoea, smoker),
                Edges.directedEdge(cancer, pollution), Edges.directedEdge(cancer, dypnoea),
                Edges.directedEdge(pollution, cancer), Edges.directedEdge(dypnoea, cancer),
                Edges.directedEdge(pollution, dypnoea), Edges.directedEdge(dypnoea, pollution)
        );
        //Act
        List<Edge> result = Utils.calculateArcs(dataset);
        for(Edge e: result){
            assertTrue(expected.contains(e));
        }

    }



    /*
    @Test
    public void scoreGraph(){
        String path = "./res/networks/BBDD/cancer.xbif50000_.csv";
        DataSet data = Utils.readData(path);
        Graph g = null;

        double score = Utils.scoreGraph(g, data);

        assertEquals(Double.NEGATIVE_INFINITY, score, 0.000001);

    }
*/

    /* For some reason this test doesn't get the same score always.
    @Test
    public void scoreGraph() throws Exception {
        //TEST: Empty graph should give back a score of 0
        String path = "./res/networks/BBDD/cancer.xbif50000_.csv";
        DataSet data = Utils.readData(path);
        Graph g = new EdgeListGraph();

        double score = Utils.scoreGraph(g, data);

        assertEquals(0.0, score, 0.0000001);

        //TEST: Score is consistent
        String net_path1 = "./res/networks/cancer.xbif";
        BIFReader bf = new BIFReader();
        bf.processFile(net_path1);
        Dag dag1 = (new MlBayesIm((BayesNet) bf)).getDag();
        Graph g2 = new EdgeListGraph(dag1);

        double score2 = Utils.scoreGraph(g2, data);

        assertEquals(-10701.380698450566, score2, 0.000001);

        //TEST: Checking score cache gets previous scores:
        double score3 = Utils.scoreGraph(g2, data);
        assertEquals(-10701.380698450566, score3, 0.000001);

    }
*/

}
