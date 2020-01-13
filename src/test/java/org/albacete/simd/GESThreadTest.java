package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GESThreadTest {

    final String path = "src/test/resources/cancer.xbif_.csv";
    final DataSet dataset = Main.readData(path);
    final Node xray = dataset.getVariable("Xray");
    final Node dyspnoea = dataset.getVariable("Dyspnoea");
    final Node cancer = dataset.getVariable("Cancer");
    final Node pollution = dataset.getVariable("Pollution");
    final Node smoker = dataset.getVariable("Smoker");

    ArrayList<TupleNode> subset1 = new ArrayList<>();
    ArrayList<TupleNode> subset2 = new ArrayList<>();


    public GESThreadTest() {
        initializeSubsets();
    }


    private void initializeSubsets() {
        // Seed used for arc split is 42

        // Subset 1:
        subset1.add(new TupleNode(dyspnoea, cancer));
        subset1.add(new TupleNode(dyspnoea, smoker));
        subset1.add(new TupleNode(xray, pollution));
        subset1.add(new TupleNode(xray, cancer));
        subset1.add(new TupleNode(cancer, pollution));


        //Subset 2:
        subset2.add(new TupleNode(pollution, smoker));
        subset2.add(new TupleNode(cancer, smoker));
        subset2.add(new TupleNode(dyspnoea, pollution));
        subset2.add(new TupleNode(xray, smoker));
        subset2.add(new TupleNode(xray, dyspnoea));

    }

    @Test
    public void insertTest(){
        // Arrange
        List<Node> nodes = new ArrayList<>();
        nodes.add(dyspnoea);
        nodes.add(smoker);
        nodes.add(xray);
        nodes.add(cancer);
        nodes.add(pollution);

        Graph g = new Dag(nodes);
        GESThread thread = new ThFES(dataset, subset1, 15);
        Set<Node> setNode = new HashSet<>();
        setNode.add(dyspnoea);
        setNode.add(smoker);

        //Expectations
        Edge edge1 = new Edge(cancer,xray, Endpoint.TAIL, Endpoint.ARROW);
        Edge edge2 = new Edge(dyspnoea,xray, Endpoint.TAIL, Endpoint.ARROW);
        Edge edge3 = new Edge(smoker,xray, Endpoint.TAIL, Endpoint.ARROW);


        // Act
        thread.insert(cancer,xray,setNode,g);

        // Assert
        assertTrue(g.getEdges().contains(edge1));
        assertTrue(g.getEdges().contains(edge2));
        assertTrue(g.getEdges().contains(edge3));
    }

    

}












    // Not working. Make Issue for this test
    /*
    @Test
    public void getFlagTest() throws InterruptedException {

        // Arrange
        GESThread gesThread = new ThFES(dataset, subset1,15);
        ThreadTest threadTest = new ThreadTest(gesThread);
        Thread thread = new Thread(threadTest);
        // Act
        thread.run();
        Graph g = new Dag();
        gesThread.currentGraph = g;
        boolean flag = threadTest.getFlag();

        // Assert
        assertFalse(flag);
    }

    private class ThreadTest implements Runnable{

        GESThread gesThread;
        boolean flag = false;
        ThreadTest(GESThread gesThread){
            this.gesThread = gesThread;
        }

        @Override
        public void run() {
            try {
                flag = this.gesThread.getFlag();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean getFlag() throws InterruptedException {
            this.flag = this.gesThread.getFlag();
            return this.flag;
        }
    }
    */


