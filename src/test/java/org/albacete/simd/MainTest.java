package org.albacete.simd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.cmu.tetrad.data.DataSet;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class MainTest
{
    /**
     * Testing csv reading
     */
    @Test
    public void shouldReadData(){
        //Arrange
        String path = "src/test/resources/cancer.xbif_.csv";
        int num_cols = 5;
        //Act
        Main main = new Main(path, 1);
        DataSet data = main.data;
        int result = data.getNumColumns();
        //Assert
        assertEquals(num_cols, result);
    }
}
