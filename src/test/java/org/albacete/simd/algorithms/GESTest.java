package org.albacete.simd.algorithms;

import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.framework.BackwardStage;
import org.albacete.simd.framework.ForwardStage;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.Resources;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static org.junit.Assert.*;


public class GESTest {

    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void constructorTests() throws IOException {
        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);

        // Creating GES with the first constructor
        DataSet dataSet = reader.parseTabular(new File(Resources.ALARM_BBDD_PATH));
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
        DataSet dataSet = reader.parseTabular(new File(Resources.CANCER_BBDD_PATH));
        GES ges1 = new GES(dataSet);

        ges1.search();
        Graph g = ges1.getCurrentGraph();
        assertNotNull(g);

    }

    @Test
    public void getProblemTest() throws IOException {

        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);

        // Creating GES with the first constructor
        DataSet dataSet = reader.parseTabular(new File(Resources.CANCER_BBDD_PATH));
        Problem expected = new Problem(dataSet);

        GES ges1 = new GES(dataSet);

        assertEquals(expected.getData(), ges1.getProblem().getData());



    }


}
