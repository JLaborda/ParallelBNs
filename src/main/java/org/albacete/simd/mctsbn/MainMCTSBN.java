package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;

import java.util.List;

public class MainMCTSBN {

    public static void main(String[] args) {
        String networkFolder = "./res/networks/";
        String net_name = "cancer";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";

        Problem problem = new Problem(bbdd_path);

        MCTSBN mctsbn = new MCTSBN(problem, 1);
        long startTime = System.currentTimeMillis();
        Dag result = mctsbn.search();
        long endTime = System.currentTimeMillis();
        double score = GESThread.scoreGraph(result, problem);

        System.out.println("MCTSBN FINISHED!");
        System.out.println("Total time: " + (endTime - startTime)*1.0 / 1000);
        System.out.println("Score: " + score);
        System.out.println("Best Order");
        System.out.println(toStringOrder(mctsbn.getBestOrder()));
        System.out.println("Best Dag: ");
        System.out.println(result);
    }

    public static String toStringOrder(List<Node> order){
        StringBuilder result = new StringBuilder();
        for (Node o: order) {
            result.append(o).append(" < ");
        }
        return result.toString();
    }
}