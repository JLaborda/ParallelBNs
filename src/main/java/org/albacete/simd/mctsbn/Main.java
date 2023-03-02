package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

public class Main {

    public static void main(String[] args) {
        String networkFolder = "./res/networks/";
        String net_name = "earthquake";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        DataSet ds = Utils.readData(bbdd_path);
        String test_path = networkFolder + "BBDD/tests/" + net_name + "_test.csv";

        Problem problem = new Problem(bbdd_path);

        MCTSBN mctsbn = new MCTSBN(problem, 1);
    }
}
