package org.albacete.simd.algorithms.GES;

import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static org.junit.Assert.*;


public class GESTest {

    @Test
    public void constructorTests() throws IOException {
        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);

        // Creating GES with the first constructor
        DataSet dataSet = reader.parseTabular(new File("res/networks/BBDD/alarm.xbif_.csv"));
        GES ges1 = new GES(dataSet);

        // Creating GES with the second constructor
        GES ges2 = new GES(dataSet, new EdgeListGraph(new LinkedList<>(dataSet.getVariables())));

        assertNotNull(ges1);
        assertNotNull(ges2);

    }

    @Test
    public void searchTest() throws IOException, InterruptedException {
        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);

        // Creating GES with the first constructor
        DataSet dataSet = reader.parseTabular(new File("res/networks/BBDD/cancer.xbif_.csv"));
        GES ges1 = new GES(dataSet);

        ges1.search();
        Graph g = ges1.getCurrentGraph();
        assertNotNull(g);

    }


}
