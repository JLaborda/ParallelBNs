package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import sun.lwawt.macosx.CSystemTray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainHCEvaluator {

    public static void main(String[] args) {
        String networkFolder = "./res/networks/";
        String net_name = "pigs";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";

        int seed = 11231231;
        Problem problem = new Problem(bbdd_path);
        //List<Node> bestOrder = new ArrayList<>();
        //HypDistrib,DuctFlow,CardiacMixing,Disease,LungFlow,CO2Report,HypoxiaInO2,XrayReport,Age,LVH,LowerBodyO2,CO2,ChestXray,LVHreport,BirthAsphyxia,GruntingReport,Grunting,Sick,LungParench,RUQO2
        /*bestOrder.add(problem.getNode("BirthAsphyxia"));
        bestOrder.add(problem.getNode("Disease"));
        bestOrder.add(problem.getNode("Sick"));
        bestOrder.add(problem.getNode("DuctFlow"));
        bestOrder.add(problem.getNode("CardiacMixing"));
        bestOrder.add(problem.getNode("LungParench"));
        bestOrder.add(problem.getNode("LungFlow"));
        bestOrder.add(problem.getNode("LVH"));
        bestOrder.add(problem.getNode("Age"));
        bestOrder.add(problem.getNode("Grunting"));
        bestOrder.add(problem.getNode("HypDistrib"));
        bestOrder.add(problem.getNode("HypoxiaInO2"));
        bestOrder.add(problem.getNode("CO2"));
        bestOrder.add(problem.getNode("ChestXray"));
        bestOrder.add(problem.getNode("LVHreport"));
        bestOrder.add(problem.getNode("GruntingReport"));
        bestOrder.add(problem.getNode("LowerBodyO2"));
        bestOrder.add(problem.getNode("RUQO2"));
        bestOrder.add(problem.getNode("CO2Report"));
        bestOrder.add(problem.getNode("XrayReport"));
*/

        List<Node> order = randomOrder(problem, seed);

        System.out.println("The order is: ");
        System.out.println(toStringOrder(order));
        System.out.println();

        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem, order);
        long startTime = System.currentTimeMillis();
        double score = hc.search();
        long endTime = System.currentTimeMillis();
        Dag resultingDag = new Dag(hc.getGraph());

        System.out.println("----------------------------------");
        System.out.println("FINISHED!");
        System.out.println("For the order: " + toStringOrder(order));
        System.out.println("The score is: " + score);
        System.out.println("The resulting Dag is: \n" + resultingDag);
        System.out.println("Time: " + (endTime - startTime) / 1000 + " seconds");
    }

    public static List<Node> randomOrder(Problem problem, int seed){
        List<Node> randomOrder = new ArrayList<>(problem.getVariables());
        Random random = new Random(seed);
        Collections.shuffle(randomOrder, random);
        return randomOrder;
    }

    public static String toStringOrder(List<Node> order){
        StringBuilder result = new StringBuilder();
        for (Node o: order) {
            result.append(o).append(" < ");
        }
        return result.toString();
    }
}
