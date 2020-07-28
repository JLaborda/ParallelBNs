package org.albacete.simd.algorithms.pGESv2;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Utils;
import org.junit.Test;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;



public class ProblemTest {

    final String path = "src/test/resources/cancer.xbif_.csv";
    /**
     * Dataset created from the data file
     */
    final DataSet dataset = Utils.readData(path);


    @Test
    public void constructorTest(){
        //Act
        Problem p1 = new Problem(path);
        Problem p2 = new Problem(dataset);

        assertNotNull(p1);
        assertNotNull(p2);
    }

    @Test
    public void gettersTest(){
        Problem problem = new Problem(dataset);

        String [] varNames = problem.getVarNames();
        List<Node> variables = problem.getVariables();
        DataSet data = problem.getData();
        LocalScoreCacheConcurrent cache = problem.getLocalScoreCache();
        double samplePrior = problem.getSamplePrior();
        double structurePrior = problem.getStructurePrior();
        int [][] cases = problem.getCases();
        HashMap<Node, Integer> index = problem.getHashIndices();
        int [] nValues = problem.getnValues();


        // Checking names
        String [] cancerNames = {"Xray", "Dyspnoea", "Cancer", "Pollution", "Smoker"};
        boolean isCancerName;

        assertEquals(5, varNames.length);
        for (String name : varNames){
            isCancerName = false;
            for(String cName : cancerNames){
                if (name.equals(cName)){
                    isCancerName = true;
                }
            }
            assertTrue(isCancerName);
        }

        //Checking Variables
        assertEquals(5, variables.size());
        for(Node n: variables){
            isCancerName = false;
            for(String cName: cancerNames){
                if(n.getName().equals(cName)){
                    isCancerName = true;
                }
            }
            assertTrue(isCancerName);
        }

        //Checking Data
        assertEquals(data, dataset);

        //Checking Cache
        assertNotNull(cache);

        //Checking index
        assertNotNull(index);

        //Checking nValues
        assertEquals(5, nValues.length);
        for(int n : nValues){
            assertEquals(2, n);
        }

        //Checking cases
        assertEquals(5000, cases.length);
        //System.out.println(cases.length);
        for(int[] caseRow : cases){
            assertEquals(5, caseRow.length);
        }

        //Checking samplePrior
        assertEquals(10.0, samplePrior, 0.0001);
        //Checking structurePrior

        assertEquals(0.001, structurePrior, 0.0001);

    }

}
